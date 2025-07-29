package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.datastructure.TypedMap;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class StateManager {

    private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);

    private static class AppExecution {
        private final String sessionId;
        // layout path
        // LinkedHashMap because the insertion order will correspond to the top to bottom order of the app script
        // component key to component object
        private final Map<Layout, LinkedHashMap<String, JtComponent<?>>> layoutToComponents = new LinkedHashMap<>();
        // current position in the list of components per layout
        private final Map<Layout, Integer> layoutToCurrentIndex = new LinkedHashMap<>();
        // whether a difference in components is found between the current app and the one being generated per layout
        private final Map<Layout, Boolean> layoutToFoundDifference = new LinkedHashMap<>();
        // does not record main and sidebar layouts - only children of these 2 roots layouts
        private final Set<Layout> clearedLayouts = new HashSet<>();

        public AppExecution(final String sessionId) {
            this.sessionId = sessionId;
        }
    }

    private static final ThreadLocal<AppExecution> CURRENT_EXECUTION_IN_THREAD = new ThreadLocal<>();

    private static final Map<String, InternalSessionState> SESSIONS = new ConcurrentHashMap<>();
    // session id to last AppExecution
    private static final Map<String, AppExecution> LAST_EXECUTIONS = new ConcurrentHashMap<>();
    // the cache is shared by all sessions
    private static final TypedMap CACHE = new TypedMap(new ConcurrentHashMap<>());

    /// A NoOpRenderServer to catch issues without breaking.
    /// This value is supposed to be changed to a proper rendering server with [#setRenderServer(RenderServer)]
    private static @Nonnull RenderServer renderServer = new NoOpRenderServer();

    protected interface RenderServer {
        // component can be null to trigger a full cleanup
        void send(final @Nonnull String sessionId, final @Nullable JtComponent<?> component, @Nonnull Layout layout, final @Nullable Integer index, final boolean clearBefore);
    }

    protected static void setRenderServer(final @Nonnull RenderServer sender) {
        renderServer = sender;
    }

    private StateManager() {
    }

    protected static InternalSessionState getSession(final String sessionId) {
        return SESSIONS.get(sessionId);
    }

    protected static InternalSessionState getCurrentSession() {
        final String currentSessionId = CURRENT_EXECUTION_IN_THREAD.get().sessionId;
        if (currentSessionId == null) {
            throw new IllegalStateException(
                    "Jeamlit Jt. methods must be called within an execution context");
        }
        return SESSIONS.get(currentSessionId);
    }

    protected static void clearSession(String sessionId) {
        SESSIONS.remove(sessionId);
    }

    protected static TypedMap getCache() {
        return CACHE;
    }

    protected static void registerCallback(final String sessionId, final String componentKey) {
        SESSIONS.get(sessionId).setCallbackComponentKey(componentKey);
    }

    /// Usage:
    /// - beginExecution
    /// - run the user app - it will call addComponent (done via Jt methods)
    /// - endExecution
    protected static void beginExecution(final String sessionId) {
        if (CURRENT_EXECUTION_IN_THREAD.get() != null) {
            throw new RuntimeException(
                    "Attempting to get a context without having removed the previous one. Application is in a bad state. Please reach out to support.");
        }
        CURRENT_EXECUTION_IN_THREAD.set(new AppExecution(sessionId));

        if (!SESSIONS.containsKey(sessionId)) {
            SESSIONS.put(sessionId, new InternalSessionState());
        }

        // run callback before everything else
        final InternalSessionState internalSessionState = SESSIONS.get(sessionId);
        final String callbackComponentKey = internalSessionState.getCallbackComponentKey();
        if (callbackComponentKey != null) {
            final JtComponent<?> jtComponent = LAST_EXECUTIONS.get(sessionId)
                    .layoutToComponents
                    .values().stream()
                    .filter(components -> components.containsKey(callbackComponentKey))
                    .findAny() // there should be only one anyway
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

    /// Usage:
    /// - beginExecution
    /// - run the user app - it will call addComponent (done via Jt methods)
    /// - endExecution
    /// Return if the component was added successfully. Else throw.
    protected static void addComponent(final @Nonnull JtComponent<?> component, final @Nonnull Layout layout) {
        final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
        if (currentExecution == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }

        if (currentExecution.layoutToComponents.values().stream().anyMatch(components -> components.containsKey(component.getKey()))) {
            // a component with the same id was already registered while running the app top to bottom
            throw DuplicateWidgetIDException.of(component);
        }
        currentExecution
                .layoutToComponents
                .computeIfAbsent(layout, k -> new LinkedHashMap<>())
                .put(component.getKey(), component);

        // Restore state from session if available
        final InternalSessionState session = getCurrentSession();
        final Object state = session.getComponentsState().get(component.getKey());
        if (state != null) {
            component.updateValue(state);
        } else if (component.returnValueIsAState()) {
            // put the current value in the widget states such that rows below this component have access to its state directly after it's added for the first time
            session.getComponentsState().put(component.getKey(), component.returnValue());
        }

        // Point-of-difference streaming logic
        final AppExecution lastExecution = LAST_EXECUTIONS.get(currentExecution.sessionId);
        boolean clearBefore = false;

        currentExecution.layoutToCurrentIndex.putIfAbsent(layout, 0);
        currentExecution.layoutToFoundDifference.putIfAbsent(layout, false);
        boolean lookForDifference = !currentExecution.clearedLayouts.contains(layout)
                                    && !currentExecution.layoutToFoundDifference.get(layout)
                                    && lastExecution != null
                                    && lastExecution.layoutToComponents.containsKey(layout)
                                    && currentExecution.layoutToCurrentIndex.get(layout) < lastExecution.layoutToComponents.get(layout).size();
        if (lookForDifference) {
            // Get previous component at the same position
            final JtComponent<?>[] previousComponents = lastExecution.layoutToComponents.get(layout).values()
                    .toArray(new JtComponent<?>[0]);
            final JtComponent<?> previousAtIndex = previousComponents[currentExecution.layoutToCurrentIndex.get(layout)];
            if (componentsEqual(previousAtIndex, component)) {
                // skip sending - increment index by 1 for layout
                currentExecution.layoutToCurrentIndex.merge(layout, 1, Integer::sum);
                return;
            } else {
                // Found difference! tell the frontend to clear from this point before adding the component
                clearBefore = true;
                // no need to look for a difference anymore - all other components in this run should be appended
                currentExecution.layoutToFoundDifference.put(layout, true);
            }
        }

        // send the component with clear instruction if needed
        renderServer.send(currentExecution.sessionId,
                          component,
                          layout,
                          // not necessary to pass the index if a difference has been found and the clear message has been sent already
                          currentExecution.layoutToFoundDifference.get(layout) && !clearBefore ? null : currentExecution.layoutToCurrentIndex.get(layout),
                          clearBefore);
        currentExecution.layoutToCurrentIndex.merge(layout, 1, Integer::sum);
        if (component.returnValue() instanceof Layout) {
            currentExecution.clearedLayouts.add((Layout) component.returnValue());
        }
    }

    // Helper method to compare components for changes
    private static boolean componentsEqual(JtComponent<?> prev, JtComponent<?> curr) {
        // Compare component type
        if (!prev.getClass().equals(curr.getClass())) {
            return false;
        }

        // Compare rendered HTML (simple approach - could be optimized)
        return prev.render().equals(curr.render());
    }

    /// Usage:
    /// - beginExecution
    /// - run the user app - it will call addComponent (done via Jt methods)
    /// - endExecution
    protected static void endExecution() {
        final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
        if (currentExecution == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        final AppExecution previousExecution = LAST_EXECUTIONS.get(currentExecution.sessionId);
        // empty layouts that did not appear in the current execution
        // clean up the end of layouts that had their number of components decrease - can happen if no clear is triggered, eg if only a statement is removed
        if (previousExecution != null) {
            for (final Layout layoutInPrevious : previousExecution.layoutToComponents.keySet()) {
                if (currentExecution.layoutToComponents.containsKey(layoutInPrevious)) {
                    final LinkedHashMap<String, JtComponent<?>> currentComponents = currentExecution.layoutToComponents.get(
                            layoutInPrevious);
                    final LinkedHashMap<String, JtComponent<?>> previousComponents = previousExecution.layoutToComponents.get(
                            layoutInPrevious);
                    if (previousComponents.size() > currentComponents.size()) {
                        renderServer.send(currentExecution.sessionId,
                                          null,
                                          layoutInPrevious,
                                          currentComponents.size(),
                                          true);
                    }
                } else {
                    // some layout is not used anymore - empty it - it's the responsibility of the layout to not appear when empty
                    renderServer.send(currentExecution.sessionId, null, layoutInPrevious, 0, true);
                }
            }
        }

        final InternalSessionState session = SESSIONS.get(currentExecution.sessionId);
        for (final LinkedHashMap<String, JtComponent<?>> currentComponents : currentExecution.layoutToComponents.values()) {
            // reset and save components state
            for (final Map.Entry<String, JtComponent<?>> entry : currentComponents.entrySet()) {
                final JtComponent<?> component = entry.getValue();
                component.resetIfNeeded();
                if (component.returnValueIsAState()) {
                    session.getComponentsState().put(entry.getKey(), component.returnValue());
                }
            }
        }

        if (previousExecution != null) {
            // remove component states of component that are not in the app anymore
            final Set<String> componentsInUseKeys = new HashSet<>();
            for (final Map<String, JtComponent<?>> m: currentExecution.layoutToComponents.values()) {
                componentsInUseKeys.addAll(m.keySet());
            }
            for (final Map<String, JtComponent<?>> m: previousExecution.layoutToComponents.values()) {
                m.keySet().stream().filter(k -> !componentsInUseKeys.contains(k)).forEach(k -> session.getComponentsState().remove(k));
            }
        }

        LAST_EXECUTIONS.put(currentExecution.sessionId, currentExecution);
         CURRENT_EXECUTION_IN_THREAD.remove();
    }

    private static class NoOpRenderServer implements RenderServer {
        @Override
        public void send(String sessionId, @Nullable JtComponent<?> component, @NotNull Layout layout, @Nullable Integer index, boolean clearBefore) {
            LOG.error(
                    "Cannot send indexed delta for component {} in layout {} at index {} to session {}. No render server is registered.",
                    component != null ? component.getKey() : null,
                    layout,
                    index,
                    sessionId);
        }
    }
}
