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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

public final class HtmlComponent extends JtComponent<JtComponent.NONE> {
    
    final String htmlContent;
    final @Nullable String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/text/HtmlComponent.register.html.mustache");
        renderTemplate = mf.compile("components/text/HtmlComponent.render.html.mustache");
    }

    private HtmlComponent(final Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE, null);
        this.htmlContent = builder.htmlContent;
        this.width = builder.width;
    }

    public static class Builder extends JtComponentBuilder<NONE, HtmlComponent, Builder> {
        private final @Nonnull @Language("HTML") String htmlContent;
        private @Nullable String width;

        public Builder(final @Nonnull @Language("HTML") String htmlContent) {
            this.htmlContent = htmlContent;
        }

        public Builder(final @Nonnull Path filePath) {
            try {
                this.htmlContent = Files.readString(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read HTML file: " + filePath, e);
            }
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
        public HtmlComponent build() {
            return new HtmlComponent(this);
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
        return new TypeReference<>() {};
    }
}
