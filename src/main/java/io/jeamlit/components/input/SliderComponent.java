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
        super(builder.generateKeyForInteractive(), builder.value, builder.onChange);

        this.label = markdownToHtml(builder.label, true);
        this.min = builder.min;
        this.max = builder.max;
        // builder.value cannot be null - see builder build()
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

        public Builder min(final double min) {
            this.min = min;
            return this;
        }

        public Builder max(final double max) {
            this.max = max;
            return this;
        }

        public Builder value(final double value) {
            this.value = value;
            return this;
        }

        public Builder step(final double step) {
            this.step = step;
            return this;
        }

        public Builder format(final @Nullable String format) {
            this.format = format;
            return this;
        }

        /**
         * A tooltip that gets displayed next to the text. If this is null (default), no tooltip is displayed.
         */
        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder labelVisibility(final LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        public Builder onChange(final @Nullable Consumer<Double> onChange) {
            this.onChange = onChange;
            return this;
        }

        public Builder width(final String width) {
            if (width != null && !"stretch".equals(width) && !width.matches("\\d+")) {
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
        public Builder width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        @Override
        public SliderComponent build() {
            // Add comment about args/kwargs not being implemented
            // Note: args/kwargs equivalent (varargs and Map parameters) not implemented

            // Set value to min if not explicitly set (matching Streamlit behavior)
            if (this.value == null) {
                this.value = this.min;
            }

            // Validate parameters
            if (this.label == null || this.label.trim().isEmpty()) {
                throw new IllegalArgumentException("Label cannot be null or empty");
            }
            if (this.min >= this.max) {
                throw new IllegalArgumentException("min_value must be less than max_value");
            }
            if (this.step <= 0) {
                throw new IllegalArgumentException("step must be positive");
            }
            if (this.value < this.min || this.value > this.max) {
                throw new IllegalArgumentException("value must be between min_value and max_value");
            }

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

    @Override
    protected void resetIfNeeded() {
        // Slider keeps its value - no reset needed
    }
}
