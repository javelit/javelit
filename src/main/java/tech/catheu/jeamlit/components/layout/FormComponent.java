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
package tech.catheu.jeamlit.components.layout;

import java.io.StringWriter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import tech.catheu.jeamlit.core.JtContainer;

public final class FormComponent extends JtComponent<JtContainer> {

    protected final boolean clearOnSubmit;
    protected final boolean enterToSubmit;
    protected final boolean border;
    protected final @Nullable String width;
    protected final @Nullable String height;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/layout/FormComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/FormComponent.render.html.mustache");
    }

    private FormComponent(final Builder builder) {
        // the currentValue is set when use() is called
        super(builder.generateKeyForInteractive(), null, null);
        this.clearOnSubmit = builder.clearOnSubmit;
        this.enterToSubmit = builder.enterToSubmit;
        this.border = builder.border;
        this.width = builder.width;
        this.height = builder.height;
    }

    public static class Builder extends JtComponentBuilder<JtContainer, FormComponent, Builder> {
        private boolean clearOnSubmit;
        private boolean enterToSubmit = true;
        private boolean border = true;
        private @Nullable String width = "stretch";
        private @Nullable String height = "content";

        public Builder(final @Nonnull String key) {
            this.key = key;
        }

        public Builder clearOnSubmit(final boolean clearOnSubmit) {
            this.clearOnSubmit = clearOnSubmit;
            return this;
        }

        public Builder enterToSubmit(final boolean enterToSubmit) {
            this.enterToSubmit = enterToSubmit;
            return this;
        }

        public Builder border(final boolean border) {
            this.border = border;
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

        public Builder height(final @Nullable String height) {
            if (height != null && !"content".equals(height) && !"stretch".equals(height) && !height.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "height must be 'content', 'stretch', or a pixel value (integer). Got: " + height);
            }
            this.height = height;
            return this;
        }

        @Override
        public FormComponent build() {
            if (JtContainer.RESERVED_PATHS.contains(this.key)) {
                throw new IllegalArgumentException("Component " + this.key + " is a reserved value. Please use another key value.");
            }
            return new FormComponent(this);
        }
    }

    @Override
    protected String register() {
        if (currentValue == null) {
            throw new IllegalStateException(
                    "Component has not been fully initialized yet. use() should be called before register().");
        }
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        if (currentValue == null) {
            throw new IllegalStateException(
                    "Component has not been fully initialized yet. use() should be called before register().");
        }
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    protected TypeReference<JtContainer> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    public void beforeUse(final @NotNull JtContainer container) {
        this.currentValue = container.formChild(getKey());
    }

    public boolean isClearOnSubmit() {
        return clearOnSubmit;
    }
}
