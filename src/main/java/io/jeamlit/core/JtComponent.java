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
package io.jeamlit.core;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;

/**
 * Base class for all Jeamlit components.
 *
 * @param <T> The type of value this component returns
 */
public abstract class JtComponent<T> {

    protected static final String UNIQUE_NAVIGATION_COMPONENT_KEY = "THERE_CAN_ONLY_BE_ONE_NAVIGATION_COMPONENT";

    // used by the components' mustache templates
    protected static final String LIT_DEPENDENCY = "https://cdn.jsdelivr.net/gh/lit/dist@3/all/lit-all.min.js";
    // see https://fonts.google.com/icons?icon.set=Material+Symbols&icon.style=Rounded
    protected static final String MATERIAL_SYMBOLS_CDN = "https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200&display=swap";
    protected static final String SPRINTF_DEPENDENCY = "https://cdn.jsdelivr.net/npm/sprintf-js@1.1.3/dist/sprintf.min.js";
    // not esm on purpose - use default if possible - esm has some hard to fix gotchas
    protected static final String ECHARTS_DEPENDENCY = "https://cdn.jsdelivr.net/npm/echarts@6.0.0/dist/echarts.min.js";
    protected static final String DOM_PURIFY_DEPENDENCY = "https://cdn.jsdelivr.net/npm/dompurify@3.2.6/dist/purify.min.js";
    // prism resources
    // inject in template by simply putting:
    // {{{ PRISM_SETUP_SNIPPET }}}
    // then inject the css, see below
    protected static final @Language("javascript") String PRISM_SETUP_SNIPPET = """
            import Prism from 'https://cdn.jsdelivr.net/npm/prismjs@1.30.0/+esm';
            import 'https://cdn.jsdelivr.net/npm/prismjs@1.30.0/plugins/autoloader/prism-autoloader.min.js/+esm';
            import 'https://cdn.jsdelivr.net/npm/prismjs@1.30.0/plugins/line-numbers/prism-line-numbers.min.js/+esm';
            Prism.plugins.autoloader.languages_path = 'https://cdn.jsdelivr.net/npm/prismjs@1.30.0/components/'; 
            """;
    // Note:
    // css should be imported with the following:
    // import prismTheme from 'https://cdn.jsdelivr.net/npm/prismjs@1.30.0/themes/prism.min.css' with {type: 'css'};
    // import prismLineNumbers from 'https://cdn.jsdelivr.net/npm/prismjs@1.30.0/plugins/line-numbers/prism-line-numbers.min.css' with {type: 'css'};
    // then in lit static styles, add prismTheme and prismLineNumbers to the array
    // BUT AS OF TODAY THIS IS ONLY COMPATIBLE WITHE CHROMIUM
    // workaround is to use this CSS directly and inject it in the css via mustache templating
    // value below from https://cdn.jsdelivr.net/npm/prismjs@1.30.0/plugins/line-numbers/prism-line-numbers.min.css
    private static final @Language("css") String PRISM_LINE_NUMBERS_CSS = "pre[class*=language-].line-numbers{position:relative;padding-left:3.8em;counter-reset:linenumber}pre[class*=language-].line-numbers>code{position:relative;white-space:inherit}.line-numbers .line-numbers-rows{position:absolute;pointer-events:none;top:0;font-size:100%;left:-3.8em;width:3em;letter-spacing:-1px;border-right:1px solid #999;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.line-numbers-rows>span{display:block;counter-increment:linenumber}.line-numbers-rows>span:before{content:counter(linenumber);color:#999;display:block;padding-right:.8em;text-align:right}";
    // value below from https://cdn.jsdelivr.net/npm/prismjs@1.30.0/themes/prism.min.css
    private static final @Language("css") String PRISM_MAIN_CSS = "code[class*=language-],pre[class*=language-]{color:#000;background:0 0;text-shadow:0 1px #fff;font-family:Consolas,Monaco,'Andale Mono','Ubuntu Mono',monospace;font-size:1em;text-align:left;white-space:pre;word-spacing:normal;word-break:normal;word-wrap:normal;line-height:1.5;-moz-tab-size:4;-o-tab-size:4;tab-size:4;-webkit-hyphens:none;-moz-hyphens:none;-ms-hyphens:none;hyphens:none}code[class*=language-] ::-moz-selection,code[class*=language-]::-moz-selection,pre[class*=language-] ::-moz-selection,pre[class*=language-]::-moz-selection{text-shadow:none;background:#b3d4fc}code[class*=language-] ::selection,code[class*=language-]::selection,pre[class*=language-] ::selection,pre[class*=language-]::selection{text-shadow:none;background:#b3d4fc}@media print{code[class*=language-],pre[class*=language-]{text-shadow:none}}pre[class*=language-]{padding:1em;margin:.5em 0;overflow:auto}:not(pre)>code[class*=language-],pre[class*=language-]{background:#f5f2f0}:not(pre)>code[class*=language-]{padding:.1em;border-radius:.3em;white-space:normal}.token.cdata,.token.comment,.token.doctype,.token.prolog{color:#708090}.token.punctuation{color:#999}.token.namespace{opacity:.7}.token.boolean,.token.constant,.token.deleted,.token.number,.token.property,.token.symbol,.token.tag{color:#905}.token.attr-name,.token.builtin,.token.char,.token.inserted,.token.selector,.token.string{color:#690}.language-css .token.string,.style .token.string,.token.entity,.token.operator,.token.url{color:#9a6e3a;background:hsla(0,0%,100%,.5)}.token.atrule,.token.attr-value,.token.keyword{color:#07a}.token.class-name,.token.function{color:#dd4a68}.token.important,.token.regex,.token.variable{color:#e90}.token.bold,.token.important{font-weight:700}.token.italic{font-style:italic}.token.entity{cursor:help}";
    /**
     * the prism css, if injected in a css text with mustache, use triple brackets
     */
    protected static final String PRISM_CSS = PRISM_LINE_NUMBERS_CSS + "\n" + PRISM_MAIN_CSS;
    protected static final @Language("css") String MARKDOWN_CSS = """
            .markdown-content strong {
                font-weight: var(--jt-font-weight-bold);
            }
            
            .markdown-content em {
                font-style: italic;
            }
            
            /* inline code is styled here - code block should be styled with prism */
            .markdown-content :not(pre) > code {
                background-color: var(--jt-bg-tertiary);
                padding: 0.1em 0.25em;
                border-radius: var(--jt-border-radius-sm);
                font-family: var(--jt-font-family-mono);
                font-size: 0.9em;
            }
            
            .markdown-content h1,
            .markdown-content h2,
            .markdown-content h3,
            .markdown-content h4,
            .markdown-content h5,
            .markdown-content h6 {
                margin: var(--jt-spacing-lg) 0 var(--jt-spacing-md) 0;
                font-weight: var(--jt-font-weight-bold);
            }
            
            .markdown-content p {
                margin: var(--jt-spacing-md) 0;
            }
            
            .markdown-content ul,
            .markdown-content ol {
                margin: var(--jt-spacing-md) 0;
                padding-left: var(--jt-spacing-xl);
            }
            
            .markdown-content li {
                margin: var(--jt-spacing-xs) 0;
            }
            
            .markdown-content a {
                color: var(--jt-theme-color);
                text-decoration: none;
            }
            
            .markdown-content a:hover {
                text-decoration: underline;
            }
            
            .markdown-content del {
                text-decoration: line-through;
            }
            
            .markdown-content blockquote {
                margin: var(--jt-spacing-md) 0;
                padding-left: var(--jt-spacing-md);
                border-left: 4px solid var(--jt-border-color);
                color: var(--jt-text-secondary);
            }
            """;


    private final String internalKey;
    private final @Nullable String userKey;
    private final boolean noPersist;
    protected T currentValue;
    private T initialValue;
    protected @Nullable Consumer<T> callback;
    private final JtContainer defaultContainer;

    protected JtComponent(final @Nonnull JtComponentBuilder builder,
                          final T currentValue,
                          final @Nullable Consumer<T> callback,
                          final @Nonnull JtContainer defaultContainer) {
        this.internalKey = builder.generateInternalKey();
        this.userKey = builder.userKey;
        this.noPersist = builder.noPersist;
        this.currentValue = currentValue;
        if (returnValueIsAState() && currentValue != null && !(currentValue instanceof Number) && !(currentValue instanceof String)) {
            // deep copy - not sure if it's really necessary
            try {
                // NOTE: some getTypeReference can only be resolved properly after the instantiation - so this call would throw an error
                // see NumberInputComponent - we avoid the issue by excluding deep copies for values of type Number - it works because they are immutable
                this.initialValue = Shared.OBJECT_MAPPER.readValue(Shared.OBJECT_MAPPER.writeValueAsString(
                        currentValue), getTypeReference());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        this.callback = callback;
        this.defaultContainer = defaultContainer;
    }

    protected JtComponent(final @Nonnull JtComponentBuilder builder, final T currentValue, final @Nullable Consumer<T> callback) {
        this(builder, currentValue, callback, JtContainer.MAIN);
    }

    // note: not renamed to internalKey for the moment because it'd break all templates // FIXME PERSIST
    public String getKey() {
        return internalKey;
    }

    @Nullable String getUserKey() {
        return userKey;
    }

    boolean isNoPersist() {
        return noPersist;
    }

    /**
     * Component definition - called once per component type.
     * This should return HTML/JS/CSS that defines the component.
     * Return {@code null} if there is nothing to do.
     */
    protected abstract @Nullable String register();

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
    protected final void updateValue(final Object valueUpdate) {
        this.currentValue = validate((T) valueUpdate);
    }

    // convert the frontend value to the java representation
    // in most cases the components T is json serializable and this method should not be overridden
    // components that maintain not json-serializable states may need to override this method
    // NOTE: in effect, overriding this method enables any kind of message passing between the frontend and the backend,
    // it is not recommended to deviate too much from the expected "json deser" logic though
    protected T convert(final Object rawValue) {
        try {
            // Use Jackson to convert to the target type
            return Shared.OBJECT_MAPPER.convertValue(rawValue, getTypeReference());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse input widget value coming from the app. Please reach out to support.",
                    e);
        }
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
        return use(defaultContainer);
    }

    /**
     * Add the component to the app in the provided container and return its value.
     */
    public final T use(final @Nonnull JtContainer container) {
        beforeUse(container);
        StateManager.addComponent(this, container);
        afterUse(container);
        return returnValue();
    }

    protected void beforeUse(final @Nonnull JtContainer container) {
        // Override in subclasses that need to do things before StateManager.addComponent runs in use()
        // subclasses are not allowed to use StateManager hence using this template pattern
    }

    protected void afterUse(final @Nonnull JtContainer container) {
        // Override in subclasses that need to do things after StateManager.addComponent runs in use().
        // subclasses are not allowed to use StateManager hence using this template pattern
    }

    /**
     * identifies a T type of a JtComponent as not to be stored in the session state
     * anything that is not a state should implement this interface
     * see also [NONE]
     */
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
            return name().toLowerCase(Locale.ROOT);
        }
    }

    // use this type to signify a component is not interactive and does not return anything
    public enum NONE implements NotAState {
        NONE_VALUE
    }

    protected static String toJson(final Object objs) {
        try {
            return Shared.OBJECT_MAPPER.writeValueAsString(objs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String markdownToHtml(final @Language("markdown") @Nullable String markdown,
                                           final boolean removeWrap) {
        return MarkdownUtils.markdownToHtml(markdown, removeWrap);
    }


    // StateManager wrappers.
    //  The methods below are simply wrapping StateManager methods
    //  this is because the JtComponent class is part of the component developer API, and we will try to not break it
    //  StateManager is not part of the public API and may get broken.
    protected static @Nonnull String getCurrentPath() {
        return StateManager.getUrlContext().currentPath();
    }

    protected static @Nonnull Map<String, List<String>> getCurrentQueryParameters() {
        return StateManager.getUrlContext().queryParameters();
    }

    protected static @Nullable NavigationComponent getNavigationComponent() {
        return StateManager.getNavigationComponent();
    }
    // end of StateManager wrappers
}
