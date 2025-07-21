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


// main interface for developers - should only contain functions of the public API.
public class Jt {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static TypedMap sessionState() {
        final ExecutionContext context = StateManager.getContext();
        final SessionState session = StateManager.getSession(context.getSessionId());
        return new TypedMap(session.getUserState());
    }

    /**
     * Returns the app cache.
     * See https://docs.streamlit.io/get-started/fundamentals/advanced-concepts#caching
     */
    public static TypedMap cache() {
        return StateManager.getCache();
    }

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
        final ExecutionContext context = StateManager.getContext();
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