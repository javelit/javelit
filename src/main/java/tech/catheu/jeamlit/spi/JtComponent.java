package tech.catheu.jeamlit.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Nonnull;

/**
 * Base class for all Jeamlit components.
 *
 * @param <T> The type of value this component returns
 */
public abstract class JtComponent<T> {

    // used by the components' mustache templates
    @SuppressWarnings("unused")
    protected static final String LIT_DEPENDENCY = JsConstants.LIT_DEPENDENCY;
    @SuppressWarnings("unused")
    protected static final String MATERIAL_SYMBOLS_CDN = JsConstants.MATERIAL_SYMBOLS_CDN;


    private final String key;
    protected T currentValue;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected JtComponent(final @Nonnull String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Component definition - called once per component type.
     * This should return HTML/JS/CSS that defines the component.
     */
    public abstract String register();

    /**
     * Component instance rendering - called for each render.
     * This should return the HTML for this specific instance.
     */
    public abstract String render();

    /**
     * Get the current value and optionally reset state.
     * Button components reset to false after reading.
     * Input components keep their value.
     */
    public final T returnValue() {
        T value = currentValue;
        resetIfNeeded();
        return value;
    }

    /**
     * For internal use only. returnValue() should be used instead in most cases
     * Will be replaced by returnValue() once it's confirm the resetIfNeeded operation is always
     * idempotent and low resource.
     */
    @Deprecated
    public final T _internalCurrentValue() {
        return currentValue;
    }

    /**
     * Update the component's value from frontend.
     * Uses Jackson for type-safe deserialization.
     */
    public void updateValue(final Object rawValue) {
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

}