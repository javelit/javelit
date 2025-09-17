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
package tech.catheu.jeamlit.components.text;

import java.io.StringWriter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

public final class TextComponent extends JtComponent<JtComponent.NONE> {
    // visible to the template engine
    final String body;
    final String help;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/TextComponent.register.html.mustache");
        renderTemplate = mf.compile("components/TextComponent.render.html.mustache");
    }

    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<NONE, TextComponent, Builder> {
        private @Language("Markdown") String body;
        private String help;
        private String width = "content";

        public Builder(final @Nonnull @Language("Markdown") String body) {
            this.body = body;
        }


        /**
         * The string to display.
         */
        public Builder body(final @Nullable @Language("Markdown") String body) {
            this.body = body;
            return this;
        }

        /**
         * A tooltip that gets displayed next to the text. If this is null (default), no tooltip is displayed.
         * The tooltip can optionally contain Markdown, including the Markdown directives, see [tech.catheu.jeamlit.core.Jt#markdown] for details.
         */
        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }

        /**
         * The width of the text element. This can be one of the following:
         * - "content" (default): The width of the element matches the width of its content, but doesn't exceed the width of the parent container.
         * - "stretch": The width of the element matches the width of the parent container.
         * - An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.
         */
        // FIXME input checks
        public Builder width(final @Nonnull String width) {
            this.width = width;
            return this;
        }

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
        public TextComponent build() {
            return new TextComponent(this);
        }
    }

    private TextComponent(Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE, null);
        this.body = builder.body;
        this.help = builder.help;
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
