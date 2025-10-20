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
package io.javelit.components.layout;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import io.javelit.core.JtComponent;
import io.javelit.core.JtComponentBuilder;
import io.javelit.core.JtContainer;
import io.javelit.core.JtLayout;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public final class TabsComponent extends JtComponent<TabsComponent.Tabs> {

    final @Nonnull List<@NotNull String> tabs;
    final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final DefaultMustacheFactory mf = new DefaultMustacheFactory();
        mf.setObjectHandler(new ReflectionObjectHandler() {
            @Override
            protected boolean areMethodsAccessible(Map<?, ?> map) {
                return true;
            }
        });
        registerTemplate = mf.compile("components/layout/TabsComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/TabsComponent.render.html.mustache");
    }

    private TabsComponent(final Builder builder) {
        // the currentValue is set when use() is called - see beforeUse
        super(builder, null, null);
        this.tabs = builder.tabs;
        this.width = builder.width;
    }

    public static class Builder extends JtComponentBuilder<Tabs, TabsComponent, Builder> {
        private final List<@NotNull String> tabs;
        private String width = "stretch";

        public Builder(@Nonnull List<@NotNull String> tabs) {
            if (tabs.isEmpty()) {
                throw new IllegalArgumentException("tabs cannot be null or empty");
            }
            for (int i = 0; i < tabs.size(); i++) {
                if (tabs.get(i).trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Tab name at index %s is null or empty. Please use a non-empty string.".formatted(i));
                }
            }
            this.tabs = List.copyOf(tabs);
        }

        /**
         * The width of the element. This can be one of the following:
         * <ul>
         * <li>{@code stretch}: The width of the element matches the width of the parent container.</li>
         * <li>An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.</li>
         * </ul>
         */
        public Builder width(final String width) {
            if (width != null && !"stretch".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException("width must be 'stretch' or a pixel value (integer). Got: " + width);
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
        public TabsComponent build() {
            return new TabsComponent(this);
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
    protected TypeReference<Tabs> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    protected void beforeUse(final @NotNull JtContainer container) {
        final JtContainer baseContainer = container.child(getInternalKey());
        this.currentValue = new Tabs(baseContainer, this.tabs);
    }

    // Helper method for Mustache template to render widths as JSON array
    String getTabsJson() {
        return toJson(tabs);
    }


    public static final class Tabs implements NotAState, JtLayout {
        private final List<@NotNull String> tabNames;
        private final List<JtContainer> backing;
        private final JtContainer layoutContainer;
        // helper data structure for mustache templates
        private final LinkedHashMap<Integer, JtContainer> indexedTabs;

        private Tabs(final JtContainer baseContainer, final List<@NotNull String> tabs) {
            this.tabNames = List.copyOf(tabs);
            this.layoutContainer = baseContainer;
            final List<JtContainer> tabsList = new ArrayList<>();
            for (int i = 0; i < tabs.size(); i++) {
                // CAUTION - the tab_{{ i }} logic is duplicated in this class and both templates
                tabsList.add(baseContainer.child("tab_" + i));
            }
            this.backing = tabsList;
            indexedTabs = new LinkedHashMap<>();
            for (int i = 0; i < backing.size(); i++) {
                indexedTabs.put(i, backing.get(i));
            }
        }

        public JtContainer tab(final int index) {
            return backing.get(index);
        }

        public JtContainer tab(final String tabName) {
            final int idx = tabNames.indexOf(tabName);
            if (idx == -1) {
                throw new IllegalArgumentException("Unknown tab name %s. Valid tab names: %s".formatted(tabName,
                                                                                                        backing.toString()));
            }
            return backing.get(idx);
        }

        // helper for mustache templates
        public Map<Integer, JtContainer> indexedTabs() {
            return indexedTabs;
        }

        @Override
        public JtContainer layoutContainer() {
            return layoutContainer;
        }
    }
}
