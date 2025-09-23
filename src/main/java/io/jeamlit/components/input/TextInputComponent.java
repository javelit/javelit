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
package io.jeamlit.components.input;

import java.io.StringWriter;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;

public final class TextInputComponent extends JtComponent<String> {
    final @Nonnull String label;
    final String value;
    final @Nullable Integer maxChars;
    final String type;
    final @Nullable String help;
    final @Nullable String autocomplete;
    final @Nullable String placeholder;
    final boolean disabled;
    final LabelVisibility labelVisibility;
    final @Nullable String icon;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/TextInputComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/TextInputComponent.render.html.mustache");
    }

    private TextInputComponent(Builder builder) {
        super(builder.generateKeyForInteractive(), builder.value, builder.onChange);

        this.label = markdownToHtml(builder.label, true);
        this.value = builder.value;
        this.maxChars = builder.maxChars;
        this.type = builder.type;
        this.help = builder.help;
        this.autocomplete = builder.autocomplete;
        this.placeholder = builder.placeholder;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.icon = builder.icon;
        this.width = builder.width;
    }

    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<String, TextInputComponent, Builder> {
        @Language("markdown") private final @Nonnull String label;
        private String value = "";
        private @Nullable Integer maxChars;
        private String type = "default";
        private @Nullable String help;
        private @Nullable String autocomplete;
        private @Nullable String placeholder;
        private boolean disabled;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private @Nullable String icon;
        private String width = "stretch";
        private @Nullable Consumer<String> onChange;

        public Builder(@Language("markdown") @Nonnull String label) {
            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("Label cannot be null or empty");
            }
            this.label = label;
        }

        /**
         * The text value of this widget when it first renders. Defaults to empty string.
         */
        public Builder value(@Nullable String value) {
            this.value = value != null ? value : "";
            return this;
        }

        /**
         * The maximum number of characters allowed in the text input.
         */
        public Builder maxChars(@Nullable Integer maxChars) {
            if (maxChars != null && maxChars <= 0) {
                throw new IllegalArgumentException("max_chars must be positive if specified");
            }
            this.maxChars = maxChars;
            return this;
        }

        /**
         * Can be "default" or "password". Determines if input masks the user's typed value.
         */
        public Builder type(@Nonnull String type) {
            if (!"default".equals(type) && !"password".equals(type)) {
                throw new IllegalArgumentException("type must be 'default' or 'password'. Got: " + type);
            }
            this.type = type;
            return this;
        }

        /**
         * A tooltip that gets displayed next to the text. If this is null (default), no tooltip is displayed.
         */
        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }

        /**
         * An optional value that will be passed to the <input> element's autocomplete property. If unspecified, this value will be set to "new-password" for "password" inputs, and the empty string for "default" inputs.
         * For more details, see <a href="https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete" target="_blank">https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete</a>.
         */
        public Builder autocomplete(@Nullable String autocomplete) {
            this.autocomplete = autocomplete;
            return this;
        }

        /**
         * An optional string displayed when the text input is empty.
         */
        public Builder placeholder(@Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Disable the text input if set to true. When disabled, users cannot interact with the widget.
         */
        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * The visibility of the label. The default is {@code VISIBLE}.
         * If this is {@code HIDDEN}, Jeamlit displays an empty spacer instead of the label, which can help keep the
         * widget aligned with other widgets. If this is {@code COLLAPSED}, Jeamlit displays no label or spacer.
         */
        public Builder labelVisibility(@Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        /**
         * An icon to display with the error message. The following values are valid:
         * <ul>
         *     <li>A single-character emoji. For example: {@code 🔥}. Emoji short codes are not supported.</li>
         *     <li>An icon from the Material Symbols library (rounded style) in the format ":icon_name:" where "icon_name" is the name of the icon in snake case. For example: {@code :search:}. See full list of icons <a href="https://fonts.google.com/icons?icon.set=Material+Symbols&icon.style=Rounded&selected=Material+Symbols+Rounded:search:FILL@0;wght@400;GRAD@0;opsz@24&icon.size=24&icon.color=%231f1f1f" target="_blank">here</a>.</li>
         * </ul>
         * If null (default), no icon is displayed.
         */
        public Builder icon(final @Nullable String icon) {
            ensureIsValidIcon(icon);
            this.icon = icon;
            return this;
        }

        /**
         * Controls the widget width. Can be "stretch" (default) or a pixel value.
         */
        public Builder width(@Nonnull String width) {
            if (!"stretch".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException("width must be 'stretch' or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        /**
         * The width of the element in pixels. The element will have a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.
         */
        public Builder width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        /**
         * An optional callback invoked when the text input's value changes.
         * The value passed in the callback is the previous value of the component.
         */
        public Builder onChange(@Nullable Consumer<String> onChange) {
            this.onChange = onChange;
            return this;
        }

        @Override
        public TextInputComponent build() {
            // Set autocomplete default based on type if not explicitly set
            if (this.autocomplete == null) {
                if ("password".equals(this.type)) {
                    this.autocomplete = "new-password";
                } else {
                    this.autocomplete = "";
                }
            }

            if (maxChars != null && value != null && value.length() > maxChars) {
                throw new IllegalArgumentException(
                        "maxChars is %s. Length of default value '%s' is %s. You must provide a default value with length smaller or equal to max_chars".formatted(
                                maxChars,
                                value,
                                value.length()));
            }

            return new TextInputComponent(this);
        }
    }

    @Override
    protected String register() {
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected TypeReference<String> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    protected String validate(String value) {
        // Apply max_chars limit if specified
        if (maxChars != null && value != null && value.length() > maxChars) {
            value = value.substring(0, maxChars);
        }

        return value != null ? value : "";
    }

    @Override
    protected void resetIfNeeded() {
        // Text input keeps its value - no reset needed
    }
}
