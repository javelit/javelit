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
package io.jeamlit.components.status;

import java.io.StringWriter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;

public final class ErrorComponent extends JtComponent<JtComponent.NONE> {
    // visible to the template engine
    final String body;
    final String icon;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/status/ErrorComponent.register.html.mustache");
        renderTemplate = mf.compile("components/status/ErrorComponent.render.html.mustache");
    }

    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<NONE, ErrorComponent, Builder> {
        private @Language("markdown") @Nonnull String body;
        private @Nullable String icon;
        private String width = "stretch";

        public Builder(final @Language("markdown") @Nonnull String body) {
            this.body = body;
        }

        /**
         * The error message content to display. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
         */
        public Builder body(final @Language("markdown") @Nonnull String body) {
            this.body = body;
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
        public Builder icon(final @Nullable String icon) {
            ensureIsValidIcon(icon);
            this.icon = icon;
            return this;
        }

        /**
         * The width of the element. This can be one of the following:
         * <ul>
         * <li>"stretch": The width of the element matches the width of the parent container.</li>
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
        public ErrorComponent build() {
            return new ErrorComponent(this);
        }
    }

    private ErrorComponent(Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE_VALUE, null);
        this.body = markdownToHtml(builder.body, false);
        this.icon = builder.icon;
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
    protected TypeReference<NONE> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
