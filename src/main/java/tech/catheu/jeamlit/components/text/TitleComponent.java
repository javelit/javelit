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
import org.jetbrains.annotations.NotNull;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

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
        super(builder.generateKeyForInteractive(), NONE.NONE, null);
        this.body = markdownToHtml(builder.body, true);
        this.anchor = builder.anchor;
        this.help = builder.help;
        this.width = builder.width;
    }
    
    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<NONE, TitleComponent, Builder> {
        @Language("markdown")
        private final @Nonnull String body;
        private String anchor;
        private String help;
        private String width = "stretch";
        
        public Builder(final @Language("markdown") @NotNull String body) {
            this.body = body;
        }
        
        public Builder anchor(final @Nullable String anchor) {
            this.anchor = anchor;
            return this;
        }
        
        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }
        
        public Builder width(final @Nonnull String width) {
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
