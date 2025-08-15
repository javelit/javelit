/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.catheu.jeamlit.core;

import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;

/**
 * Base class for all Jeamlit components.
 *
 * @param <T> The type of value this component returns
 */
public abstract class JtComponent<T> {

    // used by the components' mustache templates
    protected static final String LIT_DEPENDENCY = "https://cdn.jsdelivr.net/gh/lit/dist@3/all/lit-all.min.js";
    protected static final String MATERIAL_SYMBOLS_CDN = "https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200";
    protected static final String SPRINTF_DEPENDENCY = "https://cdn.jsdelivr.net/npm/sprintf-js@1.1.3/dist/sprintf.min.js";

    private final String key;
    protected T currentValue;
    private T initialValue;
    protected @Nullable Consumer<T> callback;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected JtComponent(final @Nonnull String key, final T currentValue, final @Nullable Consumer<T> callback) {
        this.key = key;
        this.currentValue = currentValue;
        if (returnValueIsAState() && currentValue != null && !(currentValue instanceof Number) && !(currentValue instanceof String)) {
            // deep copy - not sure if it's really necessary
            try {
                // NOTE: some getTypeReference can only be resolved properly after the instantiation - so this call would throw an error
                // see NumberInputComponent - we avoid the issue by excluding deep copies for values of type Number - it works because they are immutable
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
        T value;
        try {
            // Use Jackson to convert to the target type
            value = OBJECT_MAPPER.convertValue(rawValue, getTypeReference());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse input widget value coming from the app. Please reach out to support.", e);
        }
        value = validate(value);
        this.currentValue = value;
    }

    protected T validate(final T value) {
        // template pattern - allows implementing class to perform further validation and cleanup
        // after the input value received from the frontend is parsed successfully
        return value;
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

    /**
     * Label visibility options for components
     */
    public enum LabelVisibility {
        VISIBLE,
        HIDDEN,
        COLLAPSED;
        
        @Override
        public String toString() {
            return name().toLowerCase();
        }
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

    protected static String markdownToHtml(final @Language("markdown") @Nullable String markdown) {
        return MarkdownUtils.markdownToHtml(markdown);
    }

    protected static String markdownToHtml(final @Language("markdown") @Nullable String markdown, final boolean removeWrap) {
        return MarkdownUtils.markdownToHtml(markdown, removeWrap);
    }
}
