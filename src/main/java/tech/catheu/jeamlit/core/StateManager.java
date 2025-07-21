package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import tech.catheu.jeamlit.spi.JtComponent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateManager {

    private static final ThreadLocal<ExecutionContext> CURRENT_CONTEXT = new ThreadLocal<>();
    private static final Map<String, SessionState> SESSIONS = new ConcurrentHashMap<>();
    private static final TypedMap CACHE = new TypedMap(new ConcurrentHashMap<>());

    private StateManager() {}

    protected static void beginExecution(final String sessionId) {
        if (CURRENT_CONTEXT.get() != null) {
            throw new RuntimeException(
                    "Attempting to get a context without having removed the previous one. Application is in a bad state. Please reach out to support.");
        }
        final ExecutionContext context = new ExecutionContext(sessionId);
        CURRENT_CONTEXT.set(context);

        if (!SESSIONS.containsKey(sessionId)) {
            SESSIONS.put(sessionId, new SessionState());
        }
    }

    protected static SessionState getSession(final String sessionId) {
        return SESSIONS.get(sessionId);
    }


    protected static void clearSession(String sessionId) {
        SESSIONS.remove(sessionId);
    }

    protected static TypedMap getCache() {
        return CACHE;
    }

    protected static @Nonnull List<JtComponent<?>> endExecution() {
        final ExecutionContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        final SessionState session = SESSIONS.get(context.getSessionId());
        session.updateWidgetStates(context.getWidgetStates());

        final List<JtComponent<?>> result = context.getComponents();

        CURRENT_CONTEXT.remove();
        return result;
    }

    protected static ExecutionContext getContext() {
        final ExecutionContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "Jeamlit Jt. methods must be called within an execution context");
        }
        return context;
    }
}
