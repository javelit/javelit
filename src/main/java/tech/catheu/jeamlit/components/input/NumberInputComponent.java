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

import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

public final class NumberInputComponent<T extends Number> extends JtComponent<T> {

    private static final Logger LOG = LoggerFactory.getLogger(NumberInputComponent.class);

    final @Nonnull String label;
    final @Nullable T minValue;
    final @Nullable T maxValue;
    final T step;
    final @Nullable String format;
    final @Nullable String help;
    final @Nullable String placeholder;
    final boolean disabled;
    final LabelVisibility labelVisibility;
    final @Nullable String icon;
    final String width;
    private final Class<T> valueType;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/NumberInputComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/NumberInputComponent.render.html.mustache");
    }

    private NumberInputComponent(final Builder<T> builder) {
        super(builder.generateKeyForInteractive(), builder.value, builder.onChange);

        this.label = markdownToHtml(builder.label, true);
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.step = builder.step;
        this.format = builder.format;
        this.help = builder.help;
        this.placeholder = builder.placeholder;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.icon = builder.icon;
        this.width = builder.width;
        this.valueType = builder.valueType;
    }

    @SuppressWarnings("unused")
    public static class Builder<T extends Number> extends JtComponentBuilder<T, NumberInputComponent<T>, Builder<T>> {

        @Language("markdown") private final @Nonnull String label;
        private Class<T> valueType;
        private @Nullable T value = null;
        private @Nullable T minValue = null;
        private @Nullable T maxValue = null;
        private @Nullable T step = null;
        private @Nullable String format;
        private @Nullable String help;
        private @Nullable String placeholder;
        private boolean disabled;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private @Nullable String icon;
        private String width = "stretch";
        private @Nullable Consumer<T> onChange;
        private boolean valueSetToMin;

        public Builder(final @Language("markdown") @Nonnull String label, final @Nullable Class<T> valueType) {
            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("Label cannot be null or empty");
            }
            this.label = label;
            this.valueType = valueType;
        }

        public Builder(final @Language("markdown") @Nonnull String label) {
            this(label, null);
        }

        public Builder<T> value(@Nullable T value) {
            this.value = value;
            this.valueSetToMin = false;
            return this;
        }

        // Special method for "min" behavior (Streamlit default)
        public Builder<T> valueMin() {
            this.valueSetToMin = true;
            this.value = null; // Will be set to minValue in build()
            return this;
        }

        public Builder<T> minValue(@Nullable T minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder<T> maxValue(@Nullable T maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder<T> step(@Nullable T step) {
            if (step != null && step.doubleValue() <= 0) {
                throw new IllegalArgumentException("step must be positive if specified");
            }
            this.step = step;
            return this;
        }

        public Builder<T> format(@Nullable String format) {
            this.format = format;
            return this;
        }

        public Builder<T> help(@Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder<T> placeholder(@Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder<T> disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder<T> labelVisibility(@Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        public Builder<T> icon(@Nullable String icon) {
            ensureIsValidIcon(icon);
            this.icon = icon;
            return this;
        }

        public Builder<T> width(@Nonnull String width) {
            if (!"stretch".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException("width must be 'stretch' or a pixel value (integer). Got: " + width);
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
        public Builder<T> width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        public Builder<T> onChange(@Nullable Consumer<T> onChange) {
            this.onChange = onChange;
            return this;
        }

        @Override
        public NumberInputComponent<T> build() {
            if (valueType == Integer.class) {
                if (minValue == null) {
                    minValue = (T) Integer.valueOf(Integer.MIN_VALUE);
                }
                if (maxValue == null) {
                    maxValue = (T) Integer.valueOf(Integer.MAX_VALUE);
                }
                if (step == null) {
                    step = (T) Integer.valueOf(1);
                }
            } else if (valueType == Long.class) {
                if (minValue == null) {
                    minValue = (T) Long.valueOf(Long.MIN_VALUE);
                }
                if (maxValue == null) {
                    maxValue = (T) Long.valueOf(Long.MAX_VALUE);
                }
                if (step == null) {
                    step = (T) Long.valueOf(1L);
                }
            } else if (valueType == Float.class) {
                // min and max stay null - no limit
                if (step == null) {
                    step = (T) Float.valueOf(0.01f);
                }
            } else {
                if (!(valueType == null || valueType == Double.class)) {
                    LOG.warn("Unknown value type: {}. Assuming it can be manipulated like a Double.", valueType);
                }
                valueType = (Class<T>) Double.class;
                if (step == null) {
                    step = (T) Double.valueOf(0.01);
                }
            }

            // Validations
            if (minValue != null && maxValue != null && minValue.doubleValue() >= maxValue.doubleValue()) {
                throw new IllegalArgumentException("min_value must be less than max_value");
            }

            if (this.step.doubleValue() <= 0) {
                throw new IllegalArgumentException("step must be positive");
            }

            if (value != null) {
                if (minValue != null && value.doubleValue() < minValue.doubleValue()) {
                    throw new IllegalArgumentException("value must be >= min_value");
                }
                if (maxValue != null && value.doubleValue() > maxValue.doubleValue()) {
                    throw new IllegalArgumentException("value must be <= max_value");
                }
            }

            return new NumberInputComponent<>(this);
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
    protected TypeReference<T> getTypeReference() {
        return new TypeReference<>() {
            @Override
            public Type getType() {
                return valueType;
            }
        };
    }

    @Override
    protected void resetIfNeeded() {
        // Number input keeps its value - no reset needed
    }

}
