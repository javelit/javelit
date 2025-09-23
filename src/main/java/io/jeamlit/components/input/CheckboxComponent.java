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
import org.jetbrains.annotations.NotNull;

public final class CheckboxComponent extends JtComponent<@NotNull Boolean> {

    final String label;
    final String help;
    final boolean disabled;
    final LabelVisibility labelVisibility;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/CheckboxComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/CheckboxComponent.render.html.mustache");
    }

    public static final class Builder extends JtComponentBuilder<@NotNull Boolean, CheckboxComponent, Builder> {
        private boolean value;
        private @Nullable Consumer<@NotNull Boolean> onChange;
        private final @Language("markdown") @Nonnull String label;
        private @Nullable String help;
        private boolean disabled;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private String width = "content";

        public Builder(@Language("markdown") final @Nonnull String label) {
            this.label = label;
        }

        /**
         * Preselect the checkbox when first rendered.
         */
        public Builder value(final boolean value) {
            this.value = value;
            return this;
        }

        /**
         * An optional callback function invoked when the checkbox value changes.
         * The value passed to the callback is the previous value of the component.
         */
        public Builder onChange(final @Nullable Consumer<@NotNull Boolean> onChange) {
            this.onChange = onChange;
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
         * Disable the checkbox if set to true. When disabled, users cannot interact with the widget.
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
        public Builder labelVisibility(final @Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        /**
         * The width of the element. This can be one of the following:
         * - "content" (default): The width of the element matches the width of its content, but doesn't exceed the width of the parent container.
         * - "stretch": The width of the element matches the width of the parent container.
         * - An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.
         */
        public Builder width(final @Nullable String width) {
            if (width != null && !"stretch".equals(width) && !"content".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "width must be 'stretch', 'content', or a pixel value (integer). Got: " + width);
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
        public CheckboxComponent build() {
            return new CheckboxComponent(this);
        }
    }

    private CheckboxComponent(final @Nonnull Builder builder) {
        super(builder.generateKeyForInteractive(), builder.value, builder.onChange);
        this.label = markdownToHtml(builder.label, true);
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
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
    protected TypeReference<@NotNull Boolean> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
