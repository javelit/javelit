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
package io.jeamlit.components.text;

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
import org.jetbrains.annotations.NotNull;

public final class TitleComponent extends JtComponent<JtComponent.NONE> {
    // protected to be visible to the template engine
    final @Nonnull String body;
    final String anchor;
    final String help;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/TitleComponent.register.html.mustache");
        renderTemplate = mf.compile("components/TitleComponent.render.html.mustache");
    }

    private TitleComponent(final Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE_VALUE, null);
        this.body = markdownToHtml(builder.body, true);
        this.anchor = builder.anchor;
        this.help = builder.help;
        this.width = builder.width;
    }

    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<NONE, TitleComponent, Builder> {
        @Language("markdown") private final @Nonnull String body;
        private String anchor;
        private String help;
        private String width = "stretch";

        /**
         * The text to display. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
         */
        public Builder(final @Language("markdown") @NotNull String body) {
            this.body = body;
        }

        /**
         * The anchor name of the header that can be accessed with #anchor in the URL.
         * If omitted, it generates an anchor using the body. If False, the anchor is not shown in the UI.
         */
        public Builder anchor(final @Nullable String anchor) {
            this.anchor = anchor;
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
         * The width of the element. This can be one of the following:
         * <ul>
         * <li>"content" (default): The width of the element matches the width of its content, but doesn't exceed the width of the parent container.</li>
         * <li>"stretch": The width of the element matches the width of the parent container.</li>
         * <li>An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.</li>
         * </ul>
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
        public TitleComponent build() {
            return new TitleComponent(this);
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
    protected TypeReference<NONE> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
