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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
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

import static com.google.common.base.Preconditions.checkArgument;
import static io.javelit.core.utils.LangUtils.optional;

public class DateInputComponent extends JtComponent<LocalDate> {

    private static final Set<Object> VALID_FRONTEND_FORMATS = Set.of(
            "YYYY/MM/DD", "DD/MM/YYYY", "MM/DD/YYYY",
            "YYYY-MM-DD", "DD-MM-YYYY", "MM-DD-YYYY",
            "YYYY.MM.DD", "DD.MM.YYYY", "MM.DD.YYYY");
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
        super(builder, builder.value, builder.onChange);
        this.label = markdownToHtml(builder.label, true);
        this.minValue = Objects.requireNonNull(builder.minValue, "Implementation error. Please reach out to support.");
        this.maxValue = Objects.requireNonNull(builder.maxValue, "Implementation error. Please reach out to support.");
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

        /**
         * The value of this widget when it first renders. Can be a specific date or {@code null} for no initial value.
         */
        public Builder value(final @Nullable LocalDate value) {
            if (value == null) {
                withDefaultValue = false;
            }
            this.value = value;
            return this;
        }

        /**
         * The minimum selectable date. If {@code null}, defaults to ten years before the initial value.
         */
        public Builder minValue(final @Nullable LocalDate minValue) {
            this.minValue = minValue;
            return this;
        }

        /**
         * The maximum selectable date. If {@code null}, defaults to ten years after the initial value.
         */
        public Builder maxValue(final @Nullable LocalDate maxValue) {
            this.maxValue = maxValue;
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
         * An optional callback invoked when the date input's value changes.
         * The value passed to the callback is the previous value of the component.
         */
        public Builder onChange(final @Nullable Consumer<@org.jetbrains.annotations.Nullable LocalDate> onChange) {
            this.onChange = onChange;
            return this;
        }

        /**
         * Controls how dates are displayed in the interface. Supported formats: {@code YYYY/MM/DD}, {@code DD/MM/YYYY}, {@code MM/DD/YYYY}.
         * You may also use a period ({@code .}) or hyphen ({@code -}) as separators.
         */
        public Builder format(final @Nonnull String format) {
            checkArgument(VALID_FRONTEND_FORMATS.contains(format), "Invalid format: %s. Valid formats: %s",
                          format, VALID_FRONTEND_FORMATS);
            this.format = format;
            return this;
        }

        /**
         * Disable the date input if set to true. When disabled, users cannot interact with the widget.
         */
        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * The visibility of the label. The default is {@code VISIBLE}.
         * If this is {@code HIDDEN}, Javelit displays an empty spacer instead of the label, which can help keep the
         * widget aligned with other widgets. If this is {@code COLLAPSED}, Javelit displays no label or spacer.
         */
        public Builder labelVisibility(final @Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        /**
         * Controls the widget's width. Can be {@code "stretch"} to match parent container or a pixel value as string.
         */
        public Builder width(final @Nonnull String width) {
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

        @Override
        public DateInputComponent build() {
            if (this.value == null && withDefaultValue) {
                this.value = LocalDate.now(ZoneId.systemDefault());
            }
            if (this.minValue == null) {
                this.minValue = optional(this.value).orElse(LocalDate.now(ZoneId.systemDefault())).minusYears(10);
            } else if (this.value != null) {
                checkArgument(this.minValue.isBefore(this.value) || this.minValue.isEqual(this.value),
                              "minValue %s must be before default value %s",
                              this.minValue,
                              this.value);
            }
            if (this.maxValue == null) {
                this.maxValue = optional(this.value).orElse(LocalDate.now(ZoneId.systemDefault())).plusYears(10);
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
