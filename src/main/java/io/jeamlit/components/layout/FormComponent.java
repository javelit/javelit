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
package io.jeamlit.components.layout;

import java.io.StringWriter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import io.jeamlit.core.JtContainer;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public final class FormComponent extends JtComponent<JtContainer> {

    final boolean clearOnSubmit;
    final boolean enterToSubmit;
    final boolean border;
    final @Nullable String width;
    final @Nullable String height;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/layout/FormComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/FormComponent.render.html.mustache");
    }

    private FormComponent(final Builder builder) {
        // the currentValue is set when use() is called
        super(builder, null, null);
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

        public Builder() {
        }

        /**
         * If True, all widgets inside the form will be reset to their default values after the user presses the Submit button.
         */
        public Builder clearOnSubmit(final boolean clearOnSubmit) {
            this.clearOnSubmit = clearOnSubmit;
            return this;
        }

        /**
         * If True (default), pressing Enter while in a form widget is like clicking the first form submit button. If False, the user must click the submit button to submit the form.
         */
        public Builder enterToSubmit(final boolean enterToSubmit) {
            this.enterToSubmit = enterToSubmit;
            return this;
        }

        /**
         * Whether to show a border around the form. Default is {@code true}. It is recommended to only remove the border if there is another border or the form is small.
         */
        public Builder border(final boolean border) {
            this.border = border;
            return this;
        }

        /**
         * The width of the element. This can be one of the following:
         * <ul>
         * <li>{@code content} (default): The width of the element matches the width of its content, but doesn't exceed the width of the parent container.</li>
         * <li>{@code stretch}: The width of the element matches the width of the parent container.</li>
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

        /**
         * The height of the form container. Can be {@code "content"} (default - matches height of content), {@code "stretch"} (matches content or parent container height), or a specific pixel value (sets a fixed height, enabling scrolling if content exceeds it).
         */
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

    @Override
    protected TypeReference<JtContainer> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    protected void beforeUse(final @NotNull JtContainer container) {
        this.currentValue = container.formChild(getKey());
    }

    public boolean isClearOnSubmit() {
        return clearOnSubmit;
    }
}
