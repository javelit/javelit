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
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkArgument;

public final class SliderComponent extends JtComponent<Double> {
    final @NotNull String label;
    final double min;
    final double max;
    final double value;
    final double step;
    final @Nullable String format;
    final @Nullable String help;
    final boolean disabled;
    final LabelVisibility labelVisibility;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/SliderComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/SliderComponent.render.html.mustache");
    }

    private SliderComponent(Builder builder) {
        super(builder, builder.value, builder.onChange);

        this.label = markdownToHtml(builder.label, true);
        this.min = builder.min;
        this.max = builder.max;
        // builder.value cannot be null - see builder build()
        //noinspection DataFlowIssue
        this.value = builder.value;
        this.step = builder.step;
        this.format = builder.format;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
    }

    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<Double, SliderComponent, Builder> {
        @Language("markdown") private final @NotNull String label;
        private double min = 0.0;
        private double max = 100.0;
        private @Nullable Double value; // Will be set to min if not specified
        private double step = 1.0;
        private @Nullable String format;
        private @Nullable String help;
        private boolean disabled;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private @Nullable Consumer<Double> onChange;
        private String width = "stretch";

        public Builder(@Language("markdown") @NotNull String label) {
            this.label = label;
        }

        /**
         * The minimum permitted value.
         */
        public Builder min(final double min) {
            this.min = min;
            return this;
        }

        /**
         * The maximum permitted value.
         */
        public Builder max(final double max) {
            this.max = max;
            return this;
        }

        /**
         * The initial slider value. Defaults to the {@code min} value.
         */
        public Builder value(final double value) {
            this.value = value;
            return this;
        }

        /**
         * The stepping interval. Default is 1.
         */
        public Builder step(final double step) {
            this.step = step;
            return this;
        }

        /**
         * A printf-style format string controlling how the interface should display numbers. This does not impact the return value.
         * <p>
         * For information about formatting integers and floats, see <a href="https://github.com/alexei/sprintf.js?tab=readme-ov-file#format-specification" target="_blank">sprintf.js</a>.
         * For example, {@code format="%0.1f"} adjusts the displayed decimal precision to only show one digit after the decimal.
         */
        public Builder format(final @Nullable String format) {
            this.format = format;
            return this;
        }

        /**
         * A tooltip that gets displayed next to the text. If this is {@code null} (default), no tooltip is displayed.
         */
        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }

        /**
         * Disable the slider if set to true. When disabled, users cannot interact with the widget.
         */
        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * The visibility of the label. The default is {@code VISIBLE}.
         * If this is {@code HIDDEN}, Jeamlit displays an empty spacer instead of the label, which can help keep the
         * widget aligned with other widgets. If this is {@code COLLAPSED}, Jeamlit displays no label or spacer.
         */
        public Builder labelVisibility(final LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        /**
         * An optional callback function invoked when the slider value changes.
         * The value passed to the callback is the previous value of the component.
         */
        public Builder onChange(final @Nullable Consumer<Double> onChange) {
            this.onChange = onChange;
            return this;
        }

        /**
         * The width of the element. This can be one of the following:
         * <ul>
         * <li>{@code stretch}: The width of the element matches the width of the parent container.</li>
         * <li>An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.</li>
         * </ul>
         */
        public Builder width(final String width) {
            if (width != null && !"stretch".equals(width) && !width.matches("\\d+")) {
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

        @Override
        public SliderComponent build() {
            // Set value to min if not explicitly set
            if (this.value == null) {
                this.value = this.min;
            }

            // Validate parameters
            checkArgument(!this.label.trim().isEmpty(), "Label cannot be null or empty");
            checkArgument(this.min < this.max, "minValue must be strictly smaller than maxValue");
            checkArgument(this.step > 0, "step must be strictly positive");
            checkArgument(this.value >= this.min, "value must be greater than minValue");
            checkArgument(this.value <= this.max, "value must be smaller than maxValue");

            return new SliderComponent(this);
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
    protected TypeReference<Double> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    protected Double validate(Double value) {
        return Math.max(min, Math.min(max, value));
    }
}
