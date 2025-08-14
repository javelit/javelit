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

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;

public class TitleComponent extends JtComponent<JtComponent.NONE> {
    // protected to be visible to the template engine
    protected final String body;
    protected final String anchor;
    protected final String help;
    protected final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/TitleComponent.register.html.mustache");
        renderTemplate = mf.compile("components/TitleComponent.render.html.mustache");
    }
    
    private TitleComponent(final Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE, null);
        this.body = builder.body;
        this.anchor = builder.anchor;
        this.help = builder.help;
        this.width = builder.width;
    }
    
    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<NONE, TitleComponent, Builder> {
        private String body;
        private String anchor;
        private String help;
        private String width = "stretch";
        
        public Builder(final String body) {
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