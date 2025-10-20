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
package io.javelit.components.input;

import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javelit.core.JtComponent;
import io.javelit.core.JtComponentBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

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
        super(builder, builder.value, builder.onChange);

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
        private boolean valueSetToMin = true;

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

        /**
         * The value of this widget when it first renders.
         * If {@code null}, initializes with no value and returns null until an input is provided to the component.
         * The default behavior is to return the minimum value. If the minimum value is not set, the widget initializes
         * with a value of {@code 0}.
         */
        public Builder<T> value(final @Nullable T value) {
            this.value = value;
            this.valueSetToMin = false;
            return this;
        }

        /**
         * The minimum permitted value. For {@code Integer} and {@code Long}, defaults to the corresponding minimum
         * possible value.
         * For {@code Float} and {@code Double}, no minimum by default.
         */
        public Builder<T> minValue(final @Nullable T minValue) {
            this.minValue = minValue;
            return this;
        }

        /**
         * The maximum permitted value. For {@code Integer} and {@code Long}, defaults to the corresponding maximum
         * possible value.
         * For {@code Float} and {@code Double}, no maximum by default.
         */
        public Builder<T> maxValue(final @Nullable T maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        /**
         * The stepping interval. Defaults to 1 for {@code Integer} and {@code Long}, 0.01 for floating points.
         * Must be strictly positive.
         */
        public Builder<T> step(final @Nonnull T step) {
            checkArgument(step.doubleValue() > 0, "Step must be strictly greater than 0");
            this.step = step;
            return this;
        }

        /**
         * A printf-style format string controlling how numbers are displayed in the interface.
         * The output must be purely numeric. This does not impact the return value of the widget.
         * For more information about the formatting specification, see <a href="https://github.com/alexei/sprintf.js?tab=readme-ov-file#format-specification" target="_blank">sprintf.js</a>.
         * <p>
         * For example, {@code format="%0.1f"} adjusts the displayed decimal precision to only show one digit after the decimal.
         */
        public Builder<T> format(@Nullable String format) {
            this.format = format;
            return this;
        }

        /**
         * A tooltip that gets displayed next to the widget label. If {@code null}, no tooltip is displayed.
         */
        public Builder<T> help(@Nullable String help) {
            this.help = help;
            return this;
        }

        /**
         * An optional text displayed when the number input is empty, providing guidance to the user.
         * If None, no placeholder is displayed.
         */
        public Builder<T> placeholder(@Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Disables the number input if set to true. When disabled, users cannot interact with the widget.
         */
        public Builder<T> disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * The visibility of the label. The default is {@code VISIBLE}.
         * If this is {@code HIDDEN}, Javelit displays an empty spacer instead of the label, which can help keep the
         * widget aligned with other widgets. If this is {@code COLLAPSED}, Javelit displays no label or spacer.
         */
        public Builder<T> labelVisibility(@Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        /**
         * An icon to display with the error message. The following values are valid:
         * <ul>
         *     <li>A single-character emoji. For example: {@code 🔥}. Emoji short codes are not supported.</li>
         *     <li>An icon from the Material Symbols library (rounded style) in the format {@code :icon_name:} where {@code icon_name} is the name of the icon in snake case. For example: {@code :search:}. See full list of icons <a href="https://fonts.google.com/icons?icon.set=Material+Symbols&icon.style=Rounded&selected=Material+Symbols+Rounded:search:FILL@0;wght@400;GRAD@0;opsz@24&icon.size=24&icon.color=%231f1f1f" target="_blank">here</a>.</li>
         * </ul>
         * If {@code null} (default), no icon is displayed.
         */
        public Builder<T> icon(final @Nullable String icon) {
            ensureIsValidIcon(icon);
            this.icon = icon;
            return this;
        }

        /**
         * The width of the element. This can be one of the following:
         * <ul>
         * <li>{@code stretch}: The width of the element matches the width of the parent container.</li>
         * <li>An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.</li>
         * </ul>
         */
        public Builder<T> width(@Nonnull String width) {
            if (!"stretch".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException("width must be 'stretch' or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        /**
         * The width of the number input in pixels. The element will have a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.
         */
        public Builder<T> width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        /**
         * An optional callback invoked when the number input's value changes.
         * The value passed to the callback is the previous value of the component.
         */
        public Builder<T> onChange(@Nullable Consumer<T> onChange) {
            this.onChange = onChange;
            return this;
        }

        @Override
        public NumberInputComponent<T> build() {
            if (valueType == Integer.class) {
                final boolean minValueIsNull = minValue == null;
                final boolean maxValueIsNull = maxValue == null;
                if (!minValueIsNull && !maxValueIsNull) {
                    checkArgument(minValue.doubleValue() < maxValue.doubleValue(),
                                  "minValue must be strictly less than maxValue");
                }
                if (minValueIsNull) {
                    minValue = (T) Integer.valueOf(Integer.MIN_VALUE);
                }
                if (maxValueIsNull) {
                    maxValue = (T) Integer.valueOf(Integer.MAX_VALUE);
                }
                if (step == null) {
                    step = (T) Integer.valueOf(1);
                }
                if (value != null) {
                    checkArgument(value.doubleValue() >= minValue.doubleValue(), "value must be greater than minValue");
                    checkArgument(value.doubleValue() <= maxValue.doubleValue(), "value must be less than maxValue");
                } else if (valueSetToMin) {
                    if (minValueIsNull && maxValueIsNull) {
                        value = (T) Integer.valueOf(0);
                    } else if (minValueIsNull) {
                        value = maxValue.doubleValue() < 0 ? maxValue : (T) Integer.valueOf(0);
                    } else {
                        value = minValue;
                    }
                } // else value will be null until user interacts with the component

                return new NumberInputComponent<>(this);
            }

            if (valueType == Long.class) {
                final boolean minValueIsNull1 = minValue == null;
                final boolean maxValueIsNull1 = maxValue == null;
                if (!minValueIsNull1 && !maxValueIsNull1) {
                    checkArgument(minValue.doubleValue() < maxValue.doubleValue(),
                                  "minValue must be strictly less than maxValue");
                }
                if (minValueIsNull1) {
                    minValue = (T) Long.valueOf(Integer.MIN_VALUE);
                }
                if (maxValueIsNull1) {
                    maxValue = (T) Long.valueOf(Integer.MAX_VALUE);
                }
                if (step == null) {
                    step = (T) Long.valueOf(1);
                }
                if (value != null) {
                    checkArgument(value.doubleValue() >= minValue.doubleValue(), "value must be greater than minValue");
                    checkArgument(value.doubleValue() <= maxValue.doubleValue(), "value must be less than maxValue");
                } else if (valueSetToMin) {
                    if (minValueIsNull1 && maxValueIsNull1) {
                        value = (T) Long.valueOf(0);
                    } else if (minValueIsNull1) {
                        value = maxValue.doubleValue() < 0 ? maxValue : (T) Long.valueOf(0);
                    } else {
                        value = minValue;
                    }
                } // else value will be null until user interacts with the component

                return new NumberInputComponent<>(this);
            }

            // generic logic for floating point types
            if (valueType == Float.class) {
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
            if (minValue != null && maxValue != null) {
                checkArgument(minValue.doubleValue() < maxValue.doubleValue(),
                              "minValue must be strictly less than maxValue");
            }

            if (value != null) {
                if (minValue != null) {
                    checkArgument(value.doubleValue() >= minValue.doubleValue(), "value must be greater than minValue");
                }
                if (maxValue != null) {
                    checkArgument(value.doubleValue() <= maxValue.doubleValue(), "value must be less than maxValue");
                }
            } else if (valueSetToMin) {
                if (minValue == null && maxValue == null) {
                    value = valueType == Float.class ? (T) Float.valueOf(0f) : (T) Double.valueOf(0d);
                } else if (minValue == null) {
                    value = maxValue.doubleValue() < 0 ?
                            maxValue :
                            valueType == Float.class ? (T) Float.valueOf(0f) : (T) Double.valueOf(0d);
                    ;
                } else {
                    value = minValue;
                }
            }
            // else, the value will be null until the slider is updated by the user

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

}
