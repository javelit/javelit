package tech.catheu.jeamlit.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.components.JsConstants;

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


    private String id;
    protected T currentValue;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public JtComponent() {
        // ID will be set by the component registry
        this.id = null;
    }
    
    public JtComponent(String id) {
        this.id = id;
    }
    
    public void setId(String id) {
        if (this.id == null) {
            this.id = id;
        }
    }
    
    public String getId() { 
        return id; 
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
    public T returnValue() {
        T value = currentValue;
        resetIfNeeded();
        return value;
    }
    
    /**
     * Update the component's value from frontend.
     * Uses Jackson for type-safe deserialization.
     */
    public void updateValue(Object rawValue) {
        this.currentValue = castAndValidate(rawValue);
    }
    
    /**
     * Convert raw value to the component's type T.
     * Subclasses should override for custom validation.
     */
    protected T castAndValidate(Object rawValue) {
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
     */
    protected void resetIfNeeded() {
        // Override in subclasses that need reset behavior
    }

}