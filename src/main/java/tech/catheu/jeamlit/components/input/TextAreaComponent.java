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
package tech.catheu.jeamlit.components.input;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;
import java.util.function.Consumer;

public final class TextAreaComponent extends JtComponent<String> {
    protected final @Nonnull String label;
    protected final String value;
    protected final @Nullable String height;
    protected final @Nullable Integer maxChars;
    protected final @Nullable String help;
    protected final @Nullable String placeholder;
    protected final boolean disabled;
    protected final LabelVisibility labelVisibility;
    protected final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/TextAreaComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/TextAreaComponent.render.html.mustache");
    }
    
    private TextAreaComponent(Builder builder) {
        super(builder.generateKeyForInteractive(), builder.value, builder.onChange);

        this.label = markdownToHtml(builder.label, true);
        this.value = builder.value;
        this.height = builder.height;
        this.maxChars = builder.maxChars;
        this.help = builder.help;
        this.placeholder = builder.placeholder;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
    }
    
    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<String, TextAreaComponent, Builder> {
        @Language("markdown")
        private final @Nonnull String label;
        private String value = "";
        private @Nullable String height; // null means default (3 lines)
        private @Nullable Integer maxChars;
        private @Nullable String help;
        private @Nullable String placeholder;
        private boolean disabled;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private String width = "stretch";
        private @Nullable Consumer<String> onChange;
        
        public Builder(@Language("markdown") @Nonnull String label) {
            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("Label cannot be null or empty");
            }
            this.label = label;
        }
        
        public Builder value(@Nullable String value) {
            this.value = value != null ? value : "";
            return this;
        }
        
        public Builder height(@Nullable String height) {
            if (height != null) {
                // Validate height values
                if (!"content".equals(height) && !"stretch".equals(height) && !height.matches("\\d+")) {
                    throw new IllegalArgumentException(
                            "height must be 'content', 'stretch', or a pixel value (integer). Got: " + height);
                }
                
                // Validate minimum heights for pixel values
                if (height.matches("\\d+")) {
                    int pixels = Integer.parseInt(height);
                    return height(pixels);
                }
            }
            this.height = height;
            return this;
        }

        public Builder height(final int heightInPixels) {
            // Note: We check labelVisibility at build time, not here, since it might change
            // Minimum validation will be done in build()
            if (heightInPixels < 68) {
                throw new IllegalArgumentException(
                        "height must be at least 68 pixels (minimum allowed). Got: " + heightInPixels);
            }
            this.height = String.valueOf(heightInPixels);
            return this;
        }
        
        public Builder maxChars(@Nullable Integer maxChars) {
            if (maxChars != null && maxChars <= 0) {
                throw new IllegalArgumentException("max_chars must be positive if specified");
            }
            this.maxChars = maxChars;
            return this;
        }
        
        public Builder help(@Nullable String help) {
            this.help = help;
            return this;
        }
        
        public Builder placeholder(@Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }
        
        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }
        
        public Builder labelVisibility(@Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }
        
        public Builder width(@Nonnull String width) {
            if (!"stretch".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "width must be 'stretch' or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        /**
         * Convenience method for setting width as integer pixels.
         *
         * @param widthPixels Width in pixels (must be non-negative)
         * @return this builder
         */
        public Builder width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }
        
        public Builder onChange(@Nullable Consumer<String> onChange) {
            this.onChange = onChange;
            return this;
        }
        
        @Override
        public TextAreaComponent build() {
            // Note: args/kwargs equivalent (varargs and Map parameters) not implemented
            
            // Validate minimum height based on labelVisibility
            if (height != null && height.matches("\\d+")) {
                int pixels = Integer.parseInt(height);
                int minHeight = labelVisibility == LabelVisibility.COLLAPSED ? 68 : 98;
                if (pixels < minHeight) {
                    throw new IllegalArgumentException(
                            "height must be at least " + minHeight + " pixels when label_visibility='" + 
                            labelVisibility + "'. Got: " + pixels);
                }
            }
            
            // Validate maxChars vs value length
            if (maxChars != null && value != null && value.length() > maxChars) {
                throw new IllegalArgumentException(
                        "maxChars is " + maxChars + ". Length of default value '" + value + 
                        "' is " + value.length() + ". You must provide a default value with length " +
                        "smaller or equal to max_chars");
            }
            
            return new TextAreaComponent(this);
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
        return new TypeReference<>() {};
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
        // Text area keeps its value - no reset needed
    }

}