package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import tech.catheu.jeamlit.exception.DuplicateWidgetIDException;
import tech.catheu.jeamlit.spi.JtComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateManager {

    // LinkedHashMap because the insertion order will correspond to the top to bottom order of the app script
    // component key to component object
    private record CurrentSession(String id,  LinkedHashMap<String, JtComponent<?>> components) {}
    private static final ThreadLocal<CurrentSession> CURRENT_SESSION_IN_THREAD = new ThreadLocal<>();

    private static final Map<String, SessionState> SESSIONS = new ConcurrentHashMap<>();
    // the cache is shared by all sessions
    private static final TypedMap CACHE = new TypedMap(new ConcurrentHashMap<>());

    private StateManager() {
    }

    protected static SessionState getSession(final String sessionId) {
        return SESSIONS.get(sessionId);
    }

    protected static SessionState getCurrentSession() {
        final String currentSessionId = CURRENT_SESSION_IN_THREAD.get().id();
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

    /**
     * Usage:
     * beginExecution
     * run the user app - it will call addComponent (done via Jt methods)
     * endExecution
     */
    protected static void beginExecution(final String sessionId) {
        if (CURRENT_SESSION_IN_THREAD.get() != null) {
            throw new RuntimeException(
                    "Attempting to get a context without having removed the previous one. Application is in a bad state. Please reach out to support.");
        }
        CURRENT_SESSION_IN_THREAD.set(new CurrentSession(sessionId, new LinkedHashMap<>()));

        if (!SESSIONS.containsKey(sessionId)) {
            SESSIONS.put(sessionId, new SessionState());
        }
    }

    /**
     * Usage:
     * beginExecution
     * run the user app - it will call addComponent (done via Jt methods)
     * endExecution
     */
    protected static <T> JtComponent<T> addComponent(final JtComponent<T> component) {
        final CurrentSession currentSession = CURRENT_SESSION_IN_THREAD.get();
        if (currentSession == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        final Map<String, JtComponent<?>> currentComponents = currentSession.components();
        final String key = component.getKey();
        if (currentComponents.containsKey(key)) {
            // a component with the same id was already registered while running the app top to bottom
            throw DuplicateWidgetIDException.of(component);
        }
        currentComponents.put(key, component);
        // Restore state from session if available
        final SessionState session = getCurrentSession();
        final Object state = session.getWidgetStates().get(key);
        if (state != null) {
            component.updateValue(state);
        } else {
            // put the current value in the widget states such that rows below this component have access to its state directly after it's added for the first time
            session.getWidgetStates().put(key, component.returnValue());
        }
        return component;
    }

    /**
     * Usage:
     * beginExecution
     * run the user app - it will call addComponent (done via Jt methods)
     * endExecution
     */
    protected static @Nonnull List<JtComponent<?>> endExecution() {
        final CurrentSession currentSession = CURRENT_SESSION_IN_THREAD.get();
        if (currentSession == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        final String sessionId = currentSession.id();
        final SessionState session = SESSIONS.get(sessionId);
        final Map<String, JtComponent<?>> currentComponents = currentSession.components();
        if (currentComponents == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        for (final Map.Entry<String, JtComponent<?>> entry : currentComponents.entrySet()) {
            entry.getValue().resetIfNeeded();
            session.getWidgetStates().put(entry.getKey(), entry.getValue().returnValue());
        }

        final List<JtComponent<?>> result = new ArrayList<>(currentComponents.values());

        CURRENT_SESSION_IN_THREAD.remove();
        return result;
    }
}
