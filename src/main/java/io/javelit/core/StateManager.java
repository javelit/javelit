/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javelit.core;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.javelit.components.layout.FormComponent;
import io.javelit.components.layout.FormSubmitButtonComponent;
import io.javelit.datastructure.TypedMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.javelit.core.Server.SESSION_ID_QUERY_PARAM;

// no api here should ever be exposed
// users should use Jt
// components developers should use JtContainer and JtComponent
// it's ok to break methods and interfaces here as long as the ones above are not broken
final class StateManager {

  private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);
  private static final HashFunction HF = Hashing.murmur3_128(31);

  private static class AppExecution {
    private final String sessionId;
    private final RenderServer renderServer;
    // {container_path: {component_internal_key: component_object}}
    // LinkedHashMap because the insertion order will correspond to the top to bottom order of the app script
    private final Map<JtContainer, LinkedHashMap<String, JtComponent<?>>> containerToComponents = new LinkedHashMap<>();
    // current position in the list of components per container
    private final Map<JtContainer, Integer> containerToCurrentIndex = new LinkedHashMap<>();
    // whether a difference in components is found between the current app and the one being generated per container
    private final Map<JtContainer, Boolean> containerToFoundDifference = new LinkedHashMap<>();
    // does not record main and sidebar containers - only children of these 2 root containers
    private final Set<JtContainer> clearedContainers = new HashSet<>();
    private final Set<JtContainer> clearedLayoutContainers = new HashSet<>();
    private final HashMap<String, Integer> unusedComponents = new HashMap<>();
    // this is not the "current page" according to the URL - this is the currentPage for key context purpose
    // for instance
    // var page1 = Jt.navigation().use(); // currentPage is null
    // page.run() // inside the run, currentPage=page1 --> key context is different
    // Jt.text().use() // currentPage is back to null
    // this is used to implement: 1: key isolation by page  2: state persistence across pages when page is changed
    private JtPage executionPage;

    private AppExecution(final @Nonnull String sessionId, final @Nonnull RenderServer renderServer) {
      this.sessionId = sessionId;
      this.renderServer = renderServer;
    }
  }

  private static final ThreadLocal<AppExecution> CURRENT_EXECUTION_IN_THREAD = new ThreadLocal<>();

  private static final Map<String, InternalSessionState> SESSIONS = new ConcurrentHashMap<>();
  // session id to last AppExecution
  private static final Map<String, AppExecution> LAST_EXECUTIONS = new ConcurrentHashMap<>();
  // the cache is shared by all sessions
  private static final TypedMap CACHE = new TypedMap(new ConcurrentHashMap<>());

  enum ExecutionStatus {
    BEGIN,
    RUNNING, // can be used to provide a hearbeat for very long runs - not used for the moment
    END
  }

  interface RenderServer {
    // component can be null to trigger a full cleanup
    void send(final @Nonnull String sessionId,
              final @Nullable String renderHtml,
              final @Nullable String registrationHtml,
              @Nonnull JtContainer container,
              final @Nullable Integer index,
              final boolean clearBefore);

    void sendStatus(final @Nonnull String sessionId, final @Nonnull ExecutionStatus executionStatus,
                    final @Nullable Map<String, Integer> unusedComponents);
  }

  private StateManager() {
  }

  static InternalSessionState getCurrentSession() {
    final String currentSessionId = CURRENT_EXECUTION_IN_THREAD.get().sessionId;
    if (currentSessionId == null) {
      throw new IllegalStateException(
          "Javelit Jt. methods must be called within an execution context");
    }
    return SESSIONS.get(currentSessionId);
  }

  // must run on the same thread as the handler (it is the case) for CURRENT_EXECUTION_IN_THREAD.remove() to be correct
  static void clearSession(String sessionId) {
    SESSIONS.remove(sessionId);
    LAST_EXECUTIONS.remove(sessionId);
    CURRENT_EXECUTION_IN_THREAD.remove();
  }


  static void handleUserCodeComponentUpdate(@NotNull String userKey, @Nullable Object value) {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    checkState(currentExecution != null, "No active execution context. Please reach out to support.");
    final InternalSessionState session = StateManager.getCurrentSession();
    // Check if component has already been rendered in current execution
    final String internalKey = session.getInternalKeyFromUserKey(userKey);
    checkArgument(internalKey != null,
                  "No component with key %s exists in current page context. Make sure the component has been rendered with .key(\"%s\") at least once before trying to update its value.",
                  userKey,
                  userKey);
    boolean componentAlreadyUsedInRun = currentExecution.containerToComponents
        .values()
        .stream()
        .anyMatch(components -> components.containsKey(internalKey));
    checkArgument(!componentAlreadyUsedInRun,
                  """
                      Cannot update the value of component with key `%s`. The component has already been rendered in the current run. \s
                      Component state updates must happen before the component is rendered with .use(). \s
                      Consider moving Jt.updateComponentState(...) to before the component's .use() call, or use a button/callback to update on the next run. \s
                      See https://docs.javelit.io/develop/concepts/design/buttons#buttons-to-modify-or-reset-other-widgets
                      """,
                  userKey);
    final AppExecution lastExecution = LAST_EXECUTIONS.get(currentExecution.sessionId);
    checkState(lastExecution != null, "Implementation error. Please reach out to support.");
    JtComponent c = findIn(lastExecution, internalKey);
    Object safeValue = c.validate(value);

    session.updateComponentsState(internalKey, safeValue);
  }

  /**
   * Handles frontend component updates and returns true if app should be re-run
   */
  static boolean handleComponentUpdate(final String sessionId,
                                       final String componentKey,
                                       final Object updatedValue) {
    // find the component and its container
    final InternalSessionState session = SESSIONS.get(sessionId);
    checkState(session != null, "No session with id %s. Implementation error ?", sessionId);
    final AppExecution lastExecution = LAST_EXECUTIONS.get(sessionId);
    checkState(lastExecution != null,
               "Received an update from session %s but there wasn't any previous run in this session. Try to refresh the page.",
               sessionId);
    JtComponent<?> component = null;
    JtContainer componentContainer = null;
    for (final Map.Entry<JtContainer, LinkedHashMap<String, JtComponent<?>>> entry : lastExecution.containerToComponents.entrySet()) {
      if (entry.getValue().containsKey(componentKey)) {
        component = entry.getValue().get(componentKey);
        componentContainer = entry.getKey();
        break;
      }
    }
    checkState(component != null,
               "Received update for unknown component %s. Try to refresh the page.",
               componentKey);
    checkState(componentContainer != null, "Implementation error. Please reach out to support.");
    final String parentFormComponentKey = componentContainer.getParentFormComponentKey();


    // handle special case of form button component
    boolean rerun = true;
    if (component instanceof FormSubmitButtonComponent && Boolean.TRUE.equals(updatedValue)) {
      checkState(parentFormComponentKey != null, "FormSubmitButton must be inside a form container");
      final Map<String, Object> pendingInFormComponentStates = session
          .pendingInFormComponentsState()
          .get(parentFormComponentKey);
      if (pendingInFormComponentStates != null) {
        session.updateAllComponentsState(pendingInFormComponentStates);

        // check if the form has clearOnSubmit enabled and act accordingly - Note: data struct manipulation is a bit heavy but won't optimize for the moment
        final FormComponent formComponent = (FormComponent) findIn(lastExecution, parentFormComponentKey);
        checkState(formComponent != null, "Form component not found for key %s", parentFormComponentKey);
        if (formComponent.isClearOnSubmit()) {
          session.formComponentsToReset().addAll(pendingInFormComponentStates.keySet());
        }

        pendingInFormComponentStates.clear();
      }
      session.updateComponentsState(componentKey, updatedValue);
      registerCallback(sessionId, componentKey);
      return true;
    }
    // handle special case of component inside a form
    if (parentFormComponentKey != null) {
      if (component.returnValueIsAState()) {
        session.pendingInFormComponentsState()
               .computeIfAbsent(parentFormComponentKey, e -> new LinkedHashMap<>())
               .put(componentKey, component.convert(updatedValue));
      }
      return false;
    }
    // handle normal case
    session.updateComponentsState(componentKey, component.convert(updatedValue));
    registerCallback(sessionId, componentKey);
    return rerun;
  }

  static TypedMap getCache() {
    return CACHE;
  }

  static void setPageContext(final @Nonnull JtPage page) {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    checkState(currentExecution != null, "No active execution context. Please reach out to support.");
    final InternalSessionState session = getCurrentSession();

    // Get previous page to check if we need to clear its state
    final JtPage previousPage = session.getLastExecutionPage();

    // If previous page had noPersistWhenLeft flag and we're switching to a different page, clear its state
    if (previousPage != null
        && previousPage.isNoPersistWhenLeft()
        // urlPath diff should be enough and will be simpler to understand/debug than equals until the code stabilizes
        && // urlPath diff should be enough and will be simpler to understand/debug than equals until the code stabilizes
        !previousPage.urlPath().equals(page.urlPath())) {
      LOG.debug("Clearing state for page {} with noPersistWhenLeft", previousPage.urlPath());
      final String prefixToClear = prefixOf(previousPage);
      session.removeAllComponentsWithPrefix(prefixToClear);
    }

    // Set new page context
    currentExecution.executionPage = page;
  }

  static void clearPageContext() {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    checkState(currentExecution != null, "No active execution context.");
    // could happen for legit edge case but throwing for the moment to catch implementation errors
    if (currentExecution.executionPage != null) {
      getCurrentSession().setLastExecutionPage(currentExecution.executionPage);
      currentExecution.executionPage = null;
    } else {
      LOG.warn(
          "No current execution page. This should only happen if an error happened upstream in setPageContext.");
    }
  }

  static String pagePrefix() {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    if (currentExecution == null || currentExecution.executionPage == null) {
      return "";
    }
    return prefixOf(currentExecution.executionPage);
  }

  private static String prefixOf(final @Nonnull JtPage page) {
    return page.urlPath() + "/";
  }

  static void registerCallback(final String sessionId, final String componentKey) {
    SESSIONS.get(sessionId).setCallbackComponentKey(componentKey);
  }

  static @Nonnull String registerMedia(final MediaEntry mediaEntry) {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    checkState(currentExecution != null, "No active execution context. Please reach out to support.");
    final InternalSessionState sessionState = SESSIONS.get(currentExecution.sessionId);
    final String hash = HF
        .newHasher()
        .putBytes(mediaEntry.bytes())
        .putString(mediaEntry.format(), StandardCharsets.UTF_8)
        .hash()
        .toString();
    sessionState.getMedia().put(hash, mediaEntry);

    // /_/media/{hash}?sid={sessionId}
    return Server.MEDIA_PATH + hash + "?" + SESSION_ID_QUERY_PARAM + "=" + currentExecution.sessionId;
  }

  static @Nullable MediaEntry getMedia(final String sessionId, final String hash) {
    final InternalSessionState sessionState = SESSIONS.get(sessionId);
    // we don't throw if the session is not found - we return null, and let the server return a 404 (for security purpose)
    if (sessionState == null) {
      return null;
    }
    return sessionState.getMedia().get(hash);
  }

  /**
   * Usage:
   * - beginExecution
   * - run the user app - it will call addComponent (done via Jt methods)
   * - endExecution
   */
  // Contract: CURRENT_EXECUTION_IN_THREAD value will always be set properly, except if the CURRENT_EXECUTION_IN_THREAD already has a value, which would correspond to an incorrect implementation of the endExecution method
  static void beginExecution(final String sessionId, final RenderServer renderServer) {
    checkState(CURRENT_EXECUTION_IN_THREAD.get() == null,
               "Attempting to get a context without having removed the previous one. Application is in a bad state. Please reach out to support.");
    final AppExecution execution = new AppExecution(sessionId, renderServer);
    CURRENT_EXECUTION_IN_THREAD.set(execution);
    execution.renderServer.sendStatus(sessionId, ExecutionStatus.BEGIN, null);

    final InternalSessionState internalSessionState = SESSIONS.computeIfAbsent(sessionId,
                                                                               k -> new InternalSessionState());

    // clean-up media - does not happen in endExecution because media need to be available between executions
    internalSessionState.getMedia().clear();

    // run callback before everything else
    final String callbackComponentKey = internalSessionState.getCallbackComponentKey();
    if (callbackComponentKey != null) {
      final JtComponent<?> jtComponent = LAST_EXECUTIONS.get(sessionId)
          .containerToComponents
          .values().stream()
          .filter(components -> components.containsKey(callbackComponentKey))
          .findAny()// there should be only one anyway
          .map(components -> components.get(callbackComponentKey))
          .orElse(null);
      if (jtComponent == null) {
        LOG.warn("Failed to run callback method. Component with key {} not found. " + "To ensure the key of a component is not changed when the component is edited or mutated, pass a key parameter. " + "This issue is caused by the hot reload and will not happen when the app is deployed, so you may ignore this warning.",
                 callbackComponentKey);
      } else {
        jtComponent.executeCallback();
      }
    }
  }

  /**
   * Usage:
   * - beginExecution
   * - run the user app - it will call addComponent (done via Jt methods)
   * - endExecution
   * Return if the component was added successfully. Else throw.
   */
  static void addComponent(final @Nonnull JtComponent<?> component, final @Nonnull JtContainer container) {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    checkState(currentExecution != null, "No active execution context. Please reach out to support.");

    if (component.requiresUniqueKey()) {
      if (currentExecution.containerToComponents
          .values()
          .stream()
          .anyMatch(components -> components.containsKey(component.getInternalKey()))) {
        // a component with the same id was already registered while running the app top to bottom
        throw DuplicateWidgetIDException.forDuplicateInternalKey(component);
      }
      if (component.getUserKey() != null && currentExecution.containerToComponents
          .values()
          .stream()
          .anyMatch(components ->
                        components
                            .values()
                            .stream()
                            .anyMatch(c -> component.getUserKey().equals(c.getUserKey()))
          )) {
        throw DuplicateWidgetIDException.forDuplicateUserKey(component);
      }
    }

    final LinkedHashMap<String, JtComponent<?>> componentsMap = currentExecution
        .containerToComponents
        .computeIfAbsent(container, k -> new LinkedHashMap<>());
    if (container.isInPlace()) {
      componentsMap.clear();
    }
    componentsMap.put(component.getInternalKey(), component);

    // Restore state from session if available
    final InternalSessionState session = getCurrentSession();
    if (!NavigationComponent.UNIQUE_NAVIGATION_COMPONENT_KEY.equals(component.getInternalKey())) {
      final Object state = session.getComponentState(component.getInternalKey());
      if (state != null) {
        component.setCurrentValue(state);
      }
      if (component.returnValueIsAState()) {
        // put the current value in the widget states such that rows below this component have access to its state directly after it's added for the first time
        // and the user visible componentsState map is up to date
        session.upsertComponentsState(component);
      }
    } else {
      // navigation component is responsible for managing its own state through url context
      // also it may contain references to other classes that may be hot-reloaded, and the information of hot-reload is not available, hence getting the previous value would NEVER be correct
      // SO:
      // we never get the old value from the current state (component.setCurrentValue(state); above) ...
      // we expect the NavigationComponent to get its currentValue on its own based on urlContext --> see NavigationComponent constructor
      // ...but we still put the current value in the widget states such that it's available like any other component state
      session.upsertComponentsState(component);
    }


    // Point-of-difference streaming logic
    final AppExecution lastExecution = LAST_EXECUTIONS.get(currentExecution.sessionId);
    boolean clearBefore = false;

    currentExecution.containerToCurrentIndex.putIfAbsent(container, 0);
    currentExecution.containerToFoundDifference.putIfAbsent(container, false);
    if (container.isInPlace()) {
      // always reset inPlace containers and clear before
      currentExecution.containerToCurrentIndex.put(container, 0);
      currentExecution.containerToFoundDifference.put(container, true);
      clearBefore = true;
    }
    if (currentExecution.clearedLayoutContainers.contains(container.parent())) {
      currentExecution.containerToFoundDifference.put(container, true);
    }
    if (currentExecution.clearedContainers.contains(container)) {
      currentExecution.containerToFoundDifference.put(container, true);
    }

    final boolean lookForDifference = !currentExecution.containerToFoundDifference.get(container)
                                      && lastExecution != null
                                      && lastExecution.containerToComponents.containsKey(container)
                                      && currentExecution.containerToCurrentIndex.get(container) < lastExecution.containerToComponents
        .get(
            container)
        .size();
    if (lookForDifference) {
      // Get previous component at the same position
      final JtComponent<?>[] previousComponents = lastExecution.containerToComponents.get(
                                                                   container).values()
                                                                                     .toArray(new JtComponent<?>[0]);
      final JtComponent<?> previousAtIndex = previousComponents[currentExecution.containerToCurrentIndex.get(
          container)];
      if (previousAtIndex.contentEquals(component)) {
        // skip sending - increment index by 1 for container
        currentExecution.containerToCurrentIndex.merge(container, 1, Integer::sum);
        return;
      } else {
        // Found difference! tell the frontend to clear from this point before adding the component
        clearBefore = true;
        // no need to look for a difference anymore - all other components in this run should be appended
        currentExecution.containerToFoundDifference.put(container, true);
      }
    }

    // send the component with clear instruction if needed
    final Set<String> registeredInFrontend = session.getRegisteredInFrontend();
    final String frontendRegistrationKey = component.frontendRegistrationKey();
    currentExecution.renderServer.send(currentExecution.sessionId,
                                       component.render(),
                                       registeredInFrontend.contains(frontendRegistrationKey) ?
                                           null :
                                           component.register(),
                                       container,
                                       // not necessary to pass the index if a difference has been found and the clear message has been sent already
                                       currentExecution.containerToFoundDifference.get(container) && !clearBefore ?
                                           null :
                                           currentExecution.containerToCurrentIndex.get(
                                               container),
                                       clearBefore);
    // assume that if send does not throw, the message was well received by the frontend and the component was registered properly
    registeredInFrontend.add(frontendRegistrationKey);
    currentExecution.containerToCurrentIndex.merge(container, 1, Integer::sum);
    if (component.returnValue() instanceof JtContainer) {
      currentExecution.clearedContainers.add((JtContainer) component.returnValue());
    }
    // if a layout is cleared, all first level containers inside the layout should be cleaned up too - they are managed by this layout
    if (component.returnValue() instanceof JtLayout) {
      currentExecution.clearedLayoutContainers.add(((JtLayout) component.returnValue()).layoutContainer());
    }
  }

  /**
   * Usage:
   * - beginExecution
   * - run the user app - it will call addComponent (done via Jt methods)
   * - endExecution
   */
  // Contract: CURRENT_EXECUTION_IN_THREAD value will always be removed properly, even if something else fails
  static void endExecution() {
    try {
      final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
      checkState(currentExecution != null, "No active execution context. Please reach out to support.");
      final AppExecution previousExecution = LAST_EXECUTIONS.get(currentExecution.sessionId);
      // empty containers that did not appear in the current execution
      // clean up the end of containers that had their number of components decrease - can happen if no clear is triggered, eg if only a statement is removed
      if (previousExecution != null) {
        for (final JtContainer containerInPrevious : previousExecution.containerToComponents.keySet()) {
          if (currentExecution.containerToComponents.containsKey(containerInPrevious)) {
            final LinkedHashMap<String, JtComponent<?>> currentComponents = currentExecution.containerToComponents.get(
                containerInPrevious);
            final LinkedHashMap<String, JtComponent<?>> previousComponents = previousExecution.containerToComponents.get(
                containerInPrevious);
            if (previousComponents.size() > currentComponents.size()) {
              currentExecution.renderServer.send(currentExecution.sessionId,
                                                 null,
                                                 null,
                                                 containerInPrevious,
                                                 currentComponents.size(),
                                                 true);
            }
          } else {
            // some container is not used anymore - empty it - it's the responsibility of the container to not appear when empty
            currentExecution.renderServer.send(currentExecution.sessionId, null, null, containerInPrevious, 0, true);
          }
        }
      }

      final InternalSessionState session = SESSIONS.get(currentExecution.sessionId);
      for (final Map.Entry<JtContainer, LinkedHashMap<String, JtComponent<?>>> e : currentExecution.containerToComponents.entrySet()) {
        final JtContainer container = e.getKey();
        final LinkedHashMap<String, JtComponent<?>> currentComponents = e.getValue();
        // reset and save components state
        for (final Map.Entry<String, JtComponent<?>> entry : currentComponents.entrySet()) {
          final JtComponent<?> component = entry.getValue();
          component.resetIfNeeded();
          if (component.returnValueIsAState()) {
            checkState(entry.getKey().equals(component.getInternalKey()),
                       "Implementation error. Please reach out to support"); // used to find bug quickly during state rewrite
            session.upsertComponentsState(component);
          }
        }

        // clear form to default values
        if (!session.formComponentsToReset().isEmpty() && container.getParentFormComponentKey() != null) {
          // exploit the ordering of the linked hashmap to know the correct index to override
          int i = 0;
          for (final Map.Entry<String, JtComponent<?>> entry : currentComponents.entrySet()) {
            if (session.formComponentsToReset().contains(entry.getKey())) {
              final JtComponent<?> component = entry.getValue();
              component.resetToInitialValue();
              checkState(entry.getKey().equals(component.getInternalKey()),
                         "Implementation error. Please reach out to support"); // used to find bug quicluy during state rewrite
              session.upsertComponentsState(component);
              // registrationHtml is never necessary here - skipping the check/update of the registeredInFrontend Set
              currentExecution.renderServer.send(currentExecution.sessionId,
                                                 component.render(),
                                                 null,
                                                 container,
                                                 i,
                                                 false);
            }
            i++;
          }
          session.formComponentsToReset().clear();
        }
      }

      if (previousExecution != null) {
        // remove component states from session state if:
        //   (component was not present in the current execution) AND (component does not have a user key OR component has flag noPersist)
        final Set<String> componentsUsedInExecutionKeys = new HashSet<>();
        for (final Map<String, JtComponent<?>> m : currentExecution.containerToComponents.values()) {
          componentsUsedInExecutionKeys.addAll(m.keySet());
        }
        for (final Map<String, JtComponent<?>> m : previousExecution.containerToComponents.values()) {
          // remove components state for components that:
          m.values()
           .stream()
           // component was not present in the current execution
           .filter(c -> !componentsUsedInExecutionKeys.contains(c.getInternalKey()))
           // component does not have a user key OR component has flag noPersist
           .filter(e -> e.getUserKey() == null || e.isNoPersist())
           .map(JtComponent::getInternalKey)
           .forEach(session::removeComponentState);
        }
      }

      LAST_EXECUTIONS.put(currentExecution.sessionId, currentExecution);
      currentExecution.renderServer.sendStatus(currentExecution.sessionId,
                                               ExecutionStatus.END,
                                               currentExecution.unusedComponents);
    } catch (Exception e) {
      LOG.error(
          "Failed to end execution properly. A reload of the app may be necessary. If this happens multiple times, please reach out to support.",
          e);
    } finally {
      CURRENT_EXECUTION_IN_THREAD.remove();
    }
  }

  static void setUrlContext(final @Nonnull String sessionId,
                            final @Nonnull UrlContext urlContext) {
    final InternalSessionState session = SESSIONS.computeIfAbsent(sessionId, k -> new InternalSessionState());
    session.setUrlContext(urlContext);
    session.removeComponentState(JtComponent.UNIQUE_NAVIGATION_COMPONENT_KEY);
  }

  // this feature is named "clear cache" in the frontend but it clears cache and session states
  // it is only available to developer sessions
  static void developerReset() {
    CACHE.clear();
    SESSIONS.values().forEach(InternalSessionState::clearStates);
  }

  static @Nonnull UrlContext getUrlContext() {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    checkState(currentExecution != null, "No active execution context. Please reach out to support.");
    final InternalSessionState session = SESSIONS.get(currentExecution.sessionId);
    checkState(session != null, "No active session. Please reach out to support.");
    return session.getUrlContext();
  }

  static @Nullable NavigationComponent getNavigationComponent() {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    return (NavigationComponent) findIn(currentExecution, JtComponent.UNIQUE_NAVIGATION_COMPONENT_KEY);

  }

  static void recordComponentInstantiation(final @Nonnull String componentName) {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    if (currentExecution != null) {
      currentExecution.unusedComponents.compute(componentName, (k, v) -> v == null ? 1 : v + 1);
    } else {
      // we need to support out-of-execution case for Nb
      // just do nothing, the record thing is a nice to have to tell users in dev mode they are not
      // eventually all methods that depend on CURRENT_EXECUTION_IN_THREAD.get() will be a no op if they do not apply in CURRENT_EXECUTION_IN_THREAD
    }
  }

  // see recordComponentInstantiation
  static void recordComponentUsed(final @Nonnull String componentName) {
    final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
    if (currentExecution != null && currentExecution.unusedComponents.containsKey(componentName)) {
      if (currentExecution.unusedComponents.get(componentName) == 1) {
        currentExecution.unusedComponents.remove(componentName);
      } else {
        currentExecution.unusedComponents.compute(componentName, (k, v) -> v - 1);
      }
    }
  }

  static void registerDeveloperSession(final @Nonnull String sessionId) {
    final InternalSessionState sessionState = SESSIONS.computeIfAbsent(sessionId, k -> new InternalSessionState());
    sessionState.setDeveloper(true);
  }

  static boolean isDeveloperSession(final String sessionId) {
    final InternalSessionState sessionState = SESSIONS.get(sessionId);
    // can be relaxed later - for the moment catching bugs early
    checkState(sessionState != null, "Unknown session %s. Please reach out to support.", sessionId);
    return sessionState.isDeveloper();

  }

  private static @Nullable JtComponent<?> findIn(StateManager.AppExecution execution, String internalKey) {
    return execution.containerToComponents.values().stream()
                                          .filter(c -> c.containsKey(internalKey))
                                          .findFirst()
                                          .map(m -> m.get(internalKey))
                                          .orElse(null);
  }


  /// CAUTION the return values  of the methods below are exposed in the public api
  static @Nonnull TypedMap publicSessionState() {
    final InternalSessionState session = getCurrentSession();
    return new TypedMap(session.getUserState());
  }

  static @Nonnull TypedMap publicComponentsState() {
    final InternalSessionState session = getCurrentSession();
    // NOTE: best would be to have a deep-copy-on-read map
    // here it's the responsibility of the user to not mutate the values inside this map
    return new TypedMap(Map.copyOf(session.getUserVisibleComponentsState()), pagePrefix());
  }


}
