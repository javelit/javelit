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
import java.time.LocalDate;
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

import static io.jeamlit.core.utils.LangUtils.optional;
import static io.jeamlit.core.utils.Preconditions.checkArgument;

public class DateInputComponent extends JtComponent<LocalDate> {

    final @Nonnull String label;
    final @Nonnull LocalDate minValue;
    final @Nonnull LocalDate maxValue;
    final @Nonnull String format;
    final String help;
    final boolean disabled;
    final LabelVisibility labelVisibility;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/DateInputComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/DateInputComponent.render.html.mustache");
    }

    private DateInputComponent(final @Nonnull Builder builder) {
        super(builder.generateKeyForInteractive(), builder.value, builder.onChange);
        this.label = markdownToHtml(builder.label, true);
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.format = builder.format;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
    }

    public static class Builder extends JtComponentBuilder<LocalDate, DateInputComponent, Builder> {
        private final @Language("markdown") @Nonnull String label;
        private @Nullable LocalDate value;
        private @Nullable LocalDate minValue;
        private @Nullable LocalDate maxValue;
        private String help;
        private Consumer<LocalDate> onChange;
        private @Nonnull String format = "YYYY/MM/DD";
        private boolean disabled;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private String width = "stretch";
        private boolean withDefaultValue = true;

        public Builder(final @Language("markdown") @Nonnull String label) {
            // Validate required parameters
            checkArgument(!label.trim().isEmpty(), "DateInput label cannot be null or empty");
            this.label = label;
        }

        public Builder value(final @Nullable LocalDate value) {
            if (value == null) {
                withDefaultValue = false;
            }
            this.value = value;
            return this;
        }

        public Builder minValue(final @Nullable LocalDate minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder maxValue(final @Nullable LocalDate maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder onChange(final @Nullable Consumer<@org.jetbrains.annotations.Nullable LocalDate> onChange) {
            this.onChange = onChange;
            return this;
        }

        public Builder format(final @Nonnull String format) {
            this.format = format;
            return this;
        }

        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder labelVisibility(final @Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        public Builder width(final @Nonnull String width) {
            if (!"stretch".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "width must be 'stretch' or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        public Builder width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        @Override
        public DateInputComponent build() {
            if (this.value == null && withDefaultValue) {
                this.value = LocalDate.now();
            }
            if (this.minValue == null) {
                this.minValue = optional(this.value).orElse(LocalDate.now()).minusYears(10);
            } else if (this.value != null) {
                checkArgument(this.minValue.isBefore(this.value) || this.minValue.isEqual(this.value),
                              "minValue %s must be before default value %s",
                              this.minValue,
                              this.value);
            }
            if (this.maxValue == null) {
                this.maxValue = optional(this.value).orElse(LocalDate.now()).plusYears(10);
            } else if (this.value != null) {
                checkArgument(this.maxValue.isAfter(this.value) || this.maxValue.isEqual(this.value),
                              "maxValue %s must be after default  value %s",
                              this.maxValue,
                              this.value);
            }

            return new DateInputComponent(this);
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
    protected TypeReference<LocalDate> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
