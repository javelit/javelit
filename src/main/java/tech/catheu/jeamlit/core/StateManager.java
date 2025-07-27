package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.datastructure.TypedMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StateManager {

    private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);

    private static class AppExecution {
        private final String sessionId;
        // LinkedHashMap because the insertion order will correspond to the top to bottom order of the app script
        // component key to component object
        private final LinkedHashMap<String, JtComponent<?>> components = new LinkedHashMap<>();
        // current position in the list of components
        private int currentIndex = 0;
        // whether a difference in components is found between the current app and the one being generated
        private boolean foundDifference = false;

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

    public interface RenderServer {
        void send(final @Nonnull String sessionId, final @Nonnull JtComponent<?> component, final @Nullable Integer index, final boolean clearBefore);
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
            final JtComponent<?> jtComponent = LAST_EXECUTIONS.get(sessionId).components.get(
                    callbackComponentKey);
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
    protected static void addComponent(final @Nonnull JtComponent<?> component) {
        final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
        if (currentExecution == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }

        if (currentExecution.components.containsKey(component.getKey())) {
            // a component with the same id was already registered while running the app top to bottom
            throw DuplicateWidgetIDException.of(component);
        }
        currentExecution.components.put(component.getKey(), component);

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

        boolean lookForDifference = !currentExecution.foundDifference && lastExecution != null && currentExecution.currentIndex < lastExecution.components.size();
        if (lookForDifference) {
            // Get previous component at the same position
            final JtComponent<?>[] previousComponents = lastExecution.components.values()
                    .toArray(new JtComponent<?>[0]);
            final JtComponent<?> previousAtIndex = previousComponents[currentExecution.currentIndex];
            if (componentsEqual(previousAtIndex, component)) {
                // skip sending
                currentExecution.currentIndex += 1;
                return;
            } else {
                // Found difference! tell the frontend to clear from this point before adding the component
                clearBefore = true;
                // no need to look for a difference anymore - all other components in this run should be appended
                currentExecution.foundDifference = true;
            }
        }

        // send the component with clear instruction if needed
        renderServer.send(currentExecution.sessionId,
                          component,
                          // not necessary to pass the index if a difference has been found and the clear message has been sent already
                          currentExecution.foundDifference && !clearBefore ? null : currentExecution.currentIndex,
                          clearBefore);
        currentExecution.currentIndex += 1;
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
    protected static @Nonnull List<JtComponent<?>> endExecution() {
        final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
        if (currentExecution == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        final InternalSessionState session = SESSIONS.get(currentExecution.sessionId);
        final Map<String, JtComponent<?>> currentComponents = currentExecution.components;

        // reset and save components state
        for (final Map.Entry<String, JtComponent<?>> entry : currentComponents.entrySet()) {
            final JtComponent<?> component = entry.getValue();
            component.resetIfNeeded();
            if (component.returnValueIsAState()) {
                session.getComponentsState().put(entry.getKey(), component.returnValue());
            }
        }

        final List<JtComponent<?>> result = new ArrayList<>(currentComponents.values());

        // remove component states of component that are not in the app anymore
        final AppExecution previousExecution = LAST_EXECUTIONS.get(currentExecution.sessionId);
        if (previousExecution != null) {
            final LinkedHashMap<String, JtComponent<?>> previousComponents = previousExecution.components;
            previousComponents.keySet().stream().filter(k -> !currentComponents.containsKey(k))
                    .forEach(key -> session.getComponentsState().remove(key));
        }

        LAST_EXECUTIONS.put(currentExecution.sessionId, currentExecution);
        CURRENT_EXECUTION_IN_THREAD.remove();
        return result;
    }

    private static class NoOpRenderServer implements RenderServer {
        @Override
        public void send(String sessionId, @Nonnull JtComponent<?> component, @Nullable Integer index, boolean clearBefore) {
            LOG.error(
                    "Cannot send indexed delta for component {} at index {} to session {}. No render server is registered.",
                    component.getKey(),
                    index,
                    sessionId);
        }
    }
}
