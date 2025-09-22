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

        public Builder body(final @Language("markdown") @Nonnull String body) {
            this.body = body;
            return this;
        }

        public Builder icon(final @Nullable String icon) {
            this.icon = icon;
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
        /**
         * The width of the text element in pixels. The element will have a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.
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
        super(builder.generateKeyForInteractive(), NONE.NONE, null);
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
