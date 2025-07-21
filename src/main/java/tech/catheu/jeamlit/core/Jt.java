package tech.catheu.jeamlit.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import tech.catheu.jeamlit.components.ButtonComponent;
import tech.catheu.jeamlit.components.SliderComponent;
import tech.catheu.jeamlit.components.TextComponent;
import tech.catheu.jeamlit.components.TitleComponent;
import tech.catheu.jeamlit.spi.JtComponent;
import tech.catheu.jeamlit.spi.JtComponentBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private static ExecutionContext getContext() {
        final ExecutionContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "Jeamlit Jt. methods must be called within an execution context");
        }
        return context;
    }

    public static TypedMap sessionState() {
        final ExecutionContext context = getContext();
        final SessionState session = SESSIONS.get(context.getSessionId());
        return new TypedMap(session.getUserState());
    }

    /**
     * Returns the app cache.
     * See https://docs.streamlit.io/get-started/fundamentals/advanced-concepts#caching
     */
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

    /**
     * Add a component to the app and return its value.
     * Running directly on the builder is syntactic sugar to avoid tons of .build() in the user App
     */
    public static <T, C extends JtComponent<T>, B extends JtComponentBuilder<C>> T use(final B componentBuilder) {
        return use(componentBuilder.build());
    }

    /**
     * Add a component to the app and return its value.
     * Prefer the syntactic sugar use(final JtComponentBuilder<JtComponent<T>> componentBuilder)
     * Let available for users that want to create a JtComponent without creating a builder.
     */
    public static <T, C extends JtComponent<T>> T use(final C component) {
        final ExecutionContext context = getContext();
        final JtComponent<T> componentOnceAdded = context.addComponent(component);
        return componentOnceAdded.returnValue();
    }

    // syntactic sugar for all components - 1 method per component
    // Example: Jt.use(Jt.text("my text")); is equivalent to Jt.use(new TextComponent.Builder("my text"));
    public static TextComponent.Builder text(final @Nonnull String body) {
        return new TextComponent.Builder(body);
    }

    public static TitleComponent.Builder title(final @Nonnull String body) {
        return new TitleComponent.Builder(body);
    }

    public static ButtonComponent.Builder button(final @Nonnull String label) {
        return new ButtonComponent.Builder(label);
    }

    public static SliderComponent.Builder slider(@Nonnull String label) {
        return new SliderComponent.Builder(label);
    }

}