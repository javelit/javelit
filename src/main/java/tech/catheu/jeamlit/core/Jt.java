package tech.catheu.jeamlit.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import tech.catheu.jeamlit.components.ButtonComponent;
import tech.catheu.jeamlit.components.SliderComponent;
import tech.catheu.jeamlit.components.TitleComponent;
import tech.catheu.jeamlit.components.TextComponent;

// main interface for developers - should only contain syntactic sugar, no core logic
public class Jt {
    private static final ThreadLocal<ExecutionContext> CURRENT_CONTEXT = new ThreadLocal<>();
    static final Map<String, SessionState> SESSIONS = new ConcurrentHashMap<>();

    private static final TypedMap CACHE = new TypedMap(new ConcurrentHashMap<>());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static void beginExecution(final String sessionId) {
        if (CURRENT_CONTEXT.get() != null) {
            throw new RuntimeException(
                    "Attempting to get a context without having removed the previous one. Application is in a bad state. Please reach out to support.");
        }
        final ExecutionContext context = new ExecutionContext(sessionId);
        CURRENT_CONTEXT.set(context);

        if (!SESSIONS.containsKey(sessionId)) {
            SESSIONS.put(sessionId, new SessionState(sessionId));
        }
    }

    static Map<String, SessionState> getSessions() {
        return SESSIONS;
    }

    protected static @Nonnull List<JtComponent<?>> endExecution() {
        final ExecutionContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "No active execution context. Please reach out to support.");
        }
        final SessionState session = SESSIONS.get(context.getSessionId());
        session.updateWidgetStates(context.getWidgetStates());

        final List<JtComponent<?>> result = context.getJtComponents();

        CURRENT_CONTEXT.remove();
        return result;
    }

    public static class ExecutionResult {
        public final List<JtComponent<?>> jtComponents;

        public ExecutionResult(List<JtComponent<?>> jtComponents) {
            this.jtComponents = jtComponents;
        }
    }

    private static ExecutionContext getContext() {
        ExecutionContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "Jeamlit methods must be called within an execution context");
        }
        return context;
    }

    public static String text(final String text) {
        ExecutionContext context = getContext();
        TextComponent textComponent = new TextComponent.Builder(text).build();
        context.addJtComponent(textComponent);
        return textComponent.returnValue();
    }

    public static String title(final String text) {
        ExecutionContext context = getContext();
        TitleComponent titleComponent = new TitleComponent.Builder(text).build();
        context.addJtComponent(titleComponent);
        return titleComponent.returnValue();
    }

    public static boolean button(String label) {
        ExecutionContext context = getContext();
        String key = context.generateKey("button", label);
        return button(label, key);
    }

    public static boolean button(String label, String key) {
        ExecutionContext context = getContext();

        // Use new component system with explicit key collision detection
        ButtonComponent button = context.getComponent(key,
                                                      () -> new ButtonComponent.Builder(label).build(),
                                                      true);

        context.addJtComponent(button);
        return button.returnValue();
    }

    public static int slider(String label, int min, int max) {
        return slider(label, min, max, min);
    }

    public static int slider(String label, int min, int max, int defaultValue) {
        ExecutionContext context = getContext();
        String key = context.generateKey("slider",
                                         label,
                                         String.valueOf(min),
                                         String.valueOf(max),
                                         String.valueOf(defaultValue));
        return slider(label, min, max, defaultValue, key);
    }

    public static int slider(String label, int min, int max, int defaultValue, String key) {
        ExecutionContext context = getContext();

        // Use new component system with explicit key collision detection
        SliderComponent slider = context.getComponent(key,
                                                      () -> new SliderComponent.Builder(label).min(
                                                              min).max(max).value(defaultValue).help(
                                                              null).disabled(false).build(), true);

        context.addJtComponent(slider);
        return slider.returnValue().intValue();
    }

    public static TypedMap sessionState() {
        final ExecutionContext context = getContext();
        SessionState session = SESSIONS.get(context.getSessionId());
        return new TypedMap(session.getUserState());
    }

    public static TypedMap cache() {
        return CACHE;
    }

    //

    /**
     * Slow deep copy utility: serialize then deserialize json.
     * Made available to be able to implement the behaviour of st.cache_data that does a copy on read.
     * https://docs.streamlit.io/get-started/fundamentals/advanced-concepts#caching
     *
     * @return a deep copy of the provided object.
     * TODO add example usage for typeRef
     */
    public static <T> T deepCopy(final T original, final TypeReference<T> typeRef) {
        try {
            return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsBytes(original), typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Deep copy failed", e);
        }
    }

    public static void clearSession(String sessionId) {
        SESSIONS.remove(sessionId);
    }

}