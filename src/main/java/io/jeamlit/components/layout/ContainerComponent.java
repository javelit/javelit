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

/**
 * implements both the Container component and the Empty component via the inPlace flag.
 */
public final class ContainerComponent extends JtComponent<JtContainer> {

    final Integer height;
    final Boolean border;
    private final boolean inPlace;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/layout/ContainerComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/ContainerComponent.render.html.mustache");
    }

    private ContainerComponent(final Builder builder) {
        // the currentValue is set when use() is called
        super(builder, null, null);
        this.height = builder.height;
        this.border = builder.border;
        this.inPlace = builder.inPlace;
    }

    public static class Builder extends JtComponentBuilder<JtContainer, ContainerComponent, Builder> {
        private @Nullable Integer height;
        private @Nullable Boolean border;
        private final boolean inPlace;

        public Builder(final boolean inPlace) {
            this.inPlace = inPlace;
        }

        /**
         * The height of the container in pixels. When a fixed height is specified, the container will enable scrolling if content exceeds the specified height. It is recommended to use scrolling containers sparingly and avoid heights that exceed 500 pixels for optimal mobile experience.
         */
        public Builder height(final Integer height) {
            this.height = height;
            return this;
        }

        /**
         * Whether to show a border around the container. If not specified ({@code null}), the border is automatically shown when the container has a fixed height, and hidden when height adapts to content.
         */
        public Builder border(final @Nullable Boolean border) {
            this.border = border;
            return this;
        }

        @Override
        public ContainerComponent build() {
            if (border == null) {
                border = height != null;
            }
            return new ContainerComponent(this);
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

    /**
     * Add the component to the app in the provided [JtContainer] and return this component's [JtContainer].
     * for instance, if the container is "main", returns a container \["main", $key\]
     */
    @Override
    protected void beforeUse(final @NotNull JtContainer container) {
        if (inPlace) {
            this.currentValue = container.inPlaceChild(getKey());
        } else {
            this.currentValue = container.child(getKey());
        }
    }
}
