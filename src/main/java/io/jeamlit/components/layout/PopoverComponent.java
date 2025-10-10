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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public final class PopoverComponent extends JtComponent<JtContainer> {

    final @Nonnull String label;
    final @Nullable String help;
    final boolean disabled;
    final boolean useContainerWidth;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/layout/PopoverComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/PopoverComponent.render.html.mustache");
    }

    private PopoverComponent(final Builder builder) {
        // the currentValue is set when use() is called
        super(builder, null, null);
        this.label = markdownToHtml(builder.label, true);
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.useContainerWidth = builder.useContainerWidth;
    }

    public static class Builder extends JtComponentBuilder<JtContainer, PopoverComponent, Builder> {
        @Language("markdown") private final @Nonnull String label;
        private @Nullable String help;
        private boolean disabled;
        private boolean useContainerWidth;

        public Builder(@Language("markdown") final @Nonnull String label) {
            this.label = label;
        }

        /**
         * A tooltip that gets displayed next to the text. If this is {@code null} (default), no tooltip is displayed.
         */
        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }

        /**
         * Disables the popover button if set to True. When disabled, users cannot interact with the widget.
         */
        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * Controls the button width. If True, the button width matches the parent container width. If False (default), the button width matches its content width. This parameter is deprecated - use width() instead.
         */
        @Deprecated // to be replaced by width
        public Builder useContainerWidth(final boolean useContainerWidth) {
            this.useContainerWidth = useContainerWidth;
            return this;
        }

        @Override
        public PopoverComponent build() {
            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("Popover label cannot be null or empty");
            }
            return new PopoverComponent(this);
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
        this.currentValue = container.child(getKey());
    }
}
