package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.datastructure.TypedMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StateManager {

    private static Logger LOG = LoggerFactory.getLogger(StateManager.class);

    // LinkedHashMap because the insertion order will correspond to the top to bottom order of the app script
    // component key to component object
    private record AppExecution(String sessionId,
                                LinkedHashMap<String, JtComponent<?>> components) {
    }

    private static final ThreadLocal<AppExecution> CURRENT_EXECUTION_IN_THREAD = new ThreadLocal<>();

    private static final Map<String, InternalSessionState> SESSIONS = new ConcurrentHashMap<>();
    // session id to last AppExecution
    private static final Map<String, AppExecution> LAST_EXECUTIONS = new ConcurrentHashMap<>();
    // the cache is shared by all sessions
    private static final TypedMap CACHE = new TypedMap(new ConcurrentHashMap<>());

    private StateManager() {
    }

    protected static InternalSessionState getSession(final String sessionId) {
        return SESSIONS.get(sessionId);
    }

    protected static InternalSessionState getCurrentSession() {
        final String currentSessionId = CURRENT_EXECUTION_IN_THREAD.get().sessionId();
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

    /**
     * Usage:
     * beginExecution
     * run the user app - it will call addComponent (done via Jt methods)
     * endExecution
     */
    protected static void beginExecution(final String sessionId) {
        if (CURRENT_EXECUTION_IN_THREAD.get() != null) {
            throw new RuntimeException(
                    "Attempting to get a context without having removed the previous one. Application is in a bad state. Please reach out to support.");
        }
        CURRENT_EXECUTION_IN_THREAD.set(new AppExecution(sessionId, new LinkedHashMap<>()));

        if (!SESSIONS.containsKey(sessionId)) {
            SESSIONS.put(sessionId, new InternalSessionState());
        }

        // run callback before everything else
        final InternalSessionState internalSessionState = SESSIONS.get(sessionId);
        final String callbackComponentKey = internalSessionState.getCallbackComponentKey();
        if (callbackComponentKey != null) {
            JtComponent<?> jtComponent = LAST_EXECUTIONS.get(sessionId).components().get(
                    callbackComponentKey);
            if (jtComponent == null) {
                LOG.warn("Failed to run callback method. Component with key {} not found. " +
                                 "To ensure the key of a component is not changed when the component is edited, pass a key parameter. " +
                                 "This issue is caused by the hot reload and will not happen when the app is deployed, so you may ignore this warning.",
                         callbackComponentKey);
            } else {
                jtComponent.executeCallback();
            }
        }
    }

    /**
     * Usage:
     * beginExecution
     * run the user app - it will call addComponent (done via Jt methods)
     * endExecution
     * Return if the component was added successfully. Else throw.
     */
    protected static void addComponent(final JtComponent<?> component) {
        final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
        if (currentExecution == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        final Map<String, JtComponent<?>> currentComponents = currentExecution.components();
        final String key = component.getKey();
        if (currentComponents.containsKey(key)) {
            // a component with the same id was already registered while running the app top to bottom
            throw DuplicateWidgetIDException.of(component);
        }
        currentComponents.put(key, component);
        // Restore state from session if available
        final InternalSessionState session = getCurrentSession();
        final Object state = session.getComponentsState().get(key);
        if (state != null) {
            component.updateValue(state);
        } else {
            // put the current value in the widget states such that rows below this component have access to its state directly after it's added for the first time
            session.getComponentsState().put(key, component.returnValue());
        }
    }

    /**
     * Usage:
     * beginExecution
     * run the user app - it will call addComponent (done via Jt methods)
     * endExecution
     */
    protected static @Nonnull List<JtComponent<?>> endExecution() {
        final AppExecution currentExecution = CURRENT_EXECUTION_IN_THREAD.get();
        if (currentExecution == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        final String sessionId = currentExecution.sessionId();
        final InternalSessionState session = SESSIONS.get(sessionId);
        final Map<String, JtComponent<?>> currentComponents = currentExecution.components();
        if (currentComponents == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        for (final Map.Entry<String, JtComponent<?>> entry : currentComponents.entrySet()) {
            entry.getValue().resetIfNeeded();
            session.getComponentsState().put(entry.getKey(), entry.getValue().returnValue());
        }

        final List<JtComponent<?>> result = new ArrayList<>(currentComponents.values());

        LAST_EXECUTIONS.put(sessionId, currentExecution);
        CURRENT_EXECUTION_IN_THREAD.remove();
        return result;
    }
}
