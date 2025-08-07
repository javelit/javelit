package tech.catheu.jeamlit.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for all Jeamlit components.
 *
 * @param <T> The type of value this component returns
 */
public abstract class JtComponent<T> {

    // used by the components' mustache templates
    protected static final String LIT_DEPENDENCY = "https://cdn.jsdelivr.net/gh/lit/dist@3/core/lit-core.min.js";
    protected static final String MATERIAL_SYMBOLS_CDN = "https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200";

    private final String key;
    protected T currentValue;
    private T initialValue;
    protected @Nullable Consumer<T> callback;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected JtComponent(final @Nonnull String key, final T currentValue, final @Nullable Consumer<T> callback) {
        this.key = key;
        this.currentValue = currentValue;
        if (returnValueIsAState() && currentValue != null) {
            // deep copy - not sure if it's really necessary
            try {
                this.initialValue = OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(
                        currentValue), getTypeReference());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        this.callback = callback;
    }

    public String getKey() {
        return key;
    }

    /**
     * Component definition - called once per component type.
     * This should return HTML/JS/CSS that defines the component.
     */
    protected abstract String register();

    /**
     * Component instance rendering - called for each render.
     * This should return the HTML for this specific instance.
     */
    protected abstract String render();

    protected void executeCallback() {
        if (callback != null) {
            callback.accept(currentValue);
        }
    }

    /**
     * Get the current value and optionally reset state.
     * Button components reset to false after reading.
     * Input components keep their value.
     */
    protected final T returnValue() {
        return currentValue;
    }

    protected final boolean returnValueIsAState() {
        // do not compute and store at instantiation - some components (eg layout/container components) start
        // with a null value and have their actual value binded later
        return !(currentValue instanceof JtComponent.NotAState);
    }

    protected final void resetToInitialValue() {
        currentValue = initialValue;
    }

    /**
     * Update the component's value from frontend.
     * Uses Jackson for type-safe deserialization.
     */
    protected void updateValue(final Object rawValue) {
        this.currentValue = castAndValidate(rawValue);
    }

    /**
     * Convert raw value to the component's type T.
     * Subclasses should override for custom validation.
     */
    protected T castAndValidate(final Object rawValue) {
        try {
            // Use Jackson to convert to the target type
            return OBJECT_MAPPER.convertValue(rawValue, getTypeReference());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the TypeReference for Jackson deserialization.
     * Subclasses must implement this to specify their type.
     */
    protected abstract TypeReference<T> getTypeReference();

    /**
     * Reset component state if needed after returnValue().
     * Default implementation does nothing.
     * See example in ButtonComponent
     */
    protected void resetIfNeeded() {
        // Override in subclasses that need reset behavior
    }

    /**
     * Add the component to the app in the main container and return its value.
     */
    public final T use() {
        return use(JtContainer.MAIN);
    }

    /**
     * Add the component to the app in the provided container and return its value.
     */
    public final T use(final @Nonnull JtContainer container) {
        beforeUse(container);
        StateManager.addComponent(this, container);
        return returnValue();
    }

    public void beforeUse(final @Nonnull JtContainer container) {
        // Override in subclasses that need to do things before use() runs.
        // subclasses are not allowed to use StateManager hence using this template pattern
    }

    /// identifies a T type of a JtComponent as not to be stored in the session state
    /// anything that is not a state should implement this interface
    /// see also [NONE]
    public interface NotAState {

    }

    // use this type to signify a component is not interactive and does not return anything
    public enum NONE implements NotAState {
        NONE
    }

    protected static String toJson(final List<?> objs) {
        try {
            return OBJECT_MAPPER.writeValueAsString(objs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}