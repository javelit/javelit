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
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

public final class CodeComponent extends JtComponent<JtComponent.NONE> {
    
    final String body;
    final @Nullable String language;
    final boolean lineNumbers;
    final boolean wrapLines;
    final @Nullable String height;
    final @Nullable String width;

    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        renderTemplate = mf.compile("components/text/CodeComponent.render.html.mustache");
    }

    private CodeComponent(final Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE, null);
        this.body = builder.body;
        this.language = builder.language;
        this.lineNumbers = builder.lineNumbers;
        this.wrapLines = builder.wrapLines;
        this.height = builder.height;
        this.width = builder.width;
    }

    public static class Builder extends JtComponentBuilder<NONE, CodeComponent, Builder> {
        private final String body;
        private @Nullable String language = "java";
        private boolean lineNumbers;
        private boolean wrapLines;
        private @Nullable String height = "content";
        private @Nullable String width = "stretch";

        public Builder(final @Nonnull String body) {
            this.body = body;
        }

        public Builder language(final @Nullable String language) {
            this.language = language;
            return this;
        }

        public Builder lineNumbers(final boolean lineNumbers) {
            this.lineNumbers = lineNumbers;
            return this;
        }

        public Builder wrapLines(final boolean wrapLines) {
            this.wrapLines = wrapLines;
            return this;
        }

        public Builder height(final @Nullable String height) {
            if (height != null && !"content".equals(height) && !"stretch".equals(height) && !height.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "height must be 'content', 'stretch', or a pixel value (integer). Got: " + height);
            }
            this.height = height;
            return this;
        }

        public Builder height(final int heightPixels) {
            if (heightPixels < 0) {
                throw new IllegalArgumentException("Height in pixels must be non-negative. Got: " + heightPixels);
            }
            this.height = String.valueOf(heightPixels);
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
        public CodeComponent build() {
            return new CodeComponent(this);
        }
    }

    @Override
    protected @Nullable String register() {
        return null;
    }

    @Override
    protected String render() {
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected TypeReference<NONE> getTypeReference() {
        return new TypeReference<>() {};
    }
}
