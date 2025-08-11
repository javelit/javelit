package tech.catheu.jeamlit.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import tech.catheu.jeamlit.components.input.ButtonComponent;
import tech.catheu.jeamlit.components.input.NumberInputComponent;
import tech.catheu.jeamlit.components.input.SliderComponent;
import tech.catheu.jeamlit.components.TextComponent;
import tech.catheu.jeamlit.components.TitleComponent;
import tech.catheu.jeamlit.components.input.TextInputComponent;
import tech.catheu.jeamlit.components.layout.ColumnsComponent;
import tech.catheu.jeamlit.components.layout.ContainerComponent;
import tech.catheu.jeamlit.components.layout.ExpanderComponent;
import tech.catheu.jeamlit.components.layout.FormComponent;
import tech.catheu.jeamlit.components.layout.FormSubmitButtonComponent;
import tech.catheu.jeamlit.components.layout.PopoverComponent;
import tech.catheu.jeamlit.components.layout.TabsComponent;
import tech.catheu.jeamlit.datastructure.TypedMap;

import java.util.List;
import java.util.Map;


// main interface for developers - should only contain functions of the public API.

/**
 * The main entrypoint for app creators.
 * Add elements with Jt.title(...).use(), Jt.button(...).use(), etc...
 * Get the session state with Jt.sessionState().
 * Get the app cache Jt.cache().
 * Perform a deep copy with Jt.deepCopy(someObject).
 */
public class Jt {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static TypedMap sessionState() {
        final InternalSessionState session = StateManager.getCurrentSession();
        return new TypedMap(session.getUserState());
    }

    public static TypedMap componentsState() {
        final InternalSessionState session = StateManager.getCurrentSession();
        // NOTE: best would be to have a deep-copy-on-read map
        // here it's the responsibility of the user to not play around with the values inside this map
        return new TypedMap(Map.copyOf(session.getComponentsState()));
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

    public static JtContainer sidebar() {
        return JtContainer.SIDEBAR;
    }

    // syntactic sugar for all components - 1 method per component
    // Example: Jt.use(Jt.text("my text")); is equivalent to Jt.use(new TextComponent.Builder("my text"));
    public static TextComponent.Builder text(final @Nonnull @Language("Markdown") String body) {
        return new TextComponent.Builder(body);
    }

    public static TitleComponent.Builder title(final @Nonnull String body) {
        return new TitleComponent.Builder(body);
    }

    public static ButtonComponent.Builder button(final @Nonnull String label) {
        return new ButtonComponent.Builder(label);
    }

    public static SliderComponent.Builder slider(final @Nonnull String label) {
        return new SliderComponent.Builder(label);
    }

    public static ContainerComponent.Builder container(final @Nonnull String key) {
        return new ContainerComponent.Builder(key, false);
    }

    public static ContainerComponent.Builder empty(final @Nonnull String key) {
        return new ContainerComponent.Builder(key, true);
    }

    public static ColumnsComponent.Builder columns(final @Nonnull String key, final int numColumns) {
        return new ColumnsComponent.Builder(key, numColumns);
    }

    public static TabsComponent.Builder tabs(final @Nonnull String key, @Nonnull List<@NotNull String> tabs) {
        return new TabsComponent.Builder(key, tabs);
    }

    public static ExpanderComponent.Builder expander(final @Nonnull String key, @Nonnull String label) {
        return new ExpanderComponent.Builder(key, label);
    }

    public static PopoverComponent.Builder popover(final @Nonnull String key, @Nonnull String label) {
        return new PopoverComponent.Builder(key, label);
    }

    public static FormComponent.Builder form(final @Nonnull String key) {
        return new FormComponent.Builder(key);
    }

    public static FormSubmitButtonComponent.Builder formSubmitButton(final @Nonnull String label) {
        return new FormSubmitButtonComponent.Builder(label);
    }

    public static TextInputComponent.Builder textInput(final @Nonnull String label) {
        return new TextInputComponent.Builder(label);
    }

    public static NumberInputComponent.Builder<Number> numberInput(final @Nonnull String label) {
        return new NumberInputComponent.Builder<>(label);
    }

    public static <T extends Number> NumberInputComponent.Builder<T> numberInput(final @Nonnull String label, final Class<T> valueClass) {
        return new NumberInputComponent.Builder<>(label, valueClass);
    }

}