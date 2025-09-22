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

public final class ToggleComponent extends JtComponent<@NotNull Boolean> {

    final String label;
    final String help;
    final boolean disabled;
    final LabelVisibility labelVisibility;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/ToggleComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/ToggleComponent.render.html.mustache");
    }

    public static final class Builder extends JtComponentBuilder<@NotNull Boolean, ToggleComponent, Builder> {
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

        public Builder value(final boolean value) {
            this.value = value;
            return this;
        }

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

        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder labelVisibility(final @Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        public Builder width(final @Nullable String width) {
            if (width != null && !"stretch".equals(width) && !"content".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "width must be 'stretch', 'content', or a pixel value (integer). Got: " + width);
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
        public ToggleComponent build() {
            return new ToggleComponent(this);
        }
    }

    private ToggleComponent(final @Nonnull Builder builder) {
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
