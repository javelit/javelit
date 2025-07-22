package tech.catheu.jeamlit.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    protected @Nullable Consumer<T> callback;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected JtComponent(final @Nonnull String key, final T currentValue, final @Nullable Consumer<T> callback) {
        this.key = key;
        this.currentValue = currentValue;
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
            return objectMapper.convertValue(rawValue, getTypeReference());
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
     * Add the component to the app and return its value.
     */
    public final T use() {
        StateManager.addComponent(this);
        return returnValue();
    }

}