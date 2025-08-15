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

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import tech.catheu.jeamlit.core.JtContainer;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import tech.catheu.jeamlit.core.JtLayout;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ColumnsComponent extends JtComponent<ColumnsComponent.Columns> {

    protected final int numColumns;
    protected final @Nullable List<@NotNull Double> widths;

    public enum Gap {
        SMALL, MEDIUM, LARGE, NONE
    }

    protected final Gap gap;
    protected final VerticalAlignment verticalAlignment;

    public enum VerticalAlignment {
        TOP, CENTER, BOTTOM
    }

    protected final boolean border;

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
        registerTemplate = mf.compile("components/layout/ColumnsComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/ColumnsComponent.render.html.mustache");
    }

    private ColumnsComponent(final ColumnsComponent.Builder builder) {
        // the currentValue is set when use() is called - see beforeUse
        super(builder.generateKeyForInteractive(), null, null);
        this.numColumns = builder.numColumns;
        this.widths = builder.widths;
        this.gap = builder.gap;
        this.verticalAlignment = builder.verticalAlignment;
        this.border = builder.border;
    }

    public static class Builder extends JtComponentBuilder<Columns, ColumnsComponent, Builder> {
        private int numColumns;
        private @Nullable List<@NotNull Double> widths;
        private @Nonnull Gap gap = Gap.SMALL;
        private @Nonnull ColumnsComponent.VerticalAlignment verticalAlignment = VerticalAlignment.TOP;
        private boolean border;

        public Builder(final @Nonnull String key, final int numColumns) {
            this.key = key;
            numColumns(numColumns);
        }

        public Builder numColumns(final int numColumns) {
            if (numColumns < 2 || numColumns > 12) {
                throw new IllegalArgumentException("numColumns must be in [2, 12]");
            }
            this.numColumns = numColumns;
            return this;
        }

        public Builder widths(final @Nullable List<@NotNull Double> widths) {
            this.widths = widths;
            return this;
        }

        public Builder gap(final @Nonnull Gap gap) {
            this.gap = gap;
            return this;
        }

        public Builder verticalAlignment(final @Nonnull VerticalAlignment verticalAlignment) {
            this.verticalAlignment = verticalAlignment;
            return this;
        }

        public Builder border(final boolean border) {
            this.border = border;
            return this;
        }

        @Override
        public ColumnsComponent build() {
            if (JtContainer.RESERVED_PATHS.contains(this.key)) {
                throw new IllegalArgumentException("Component " + this.key + " is a reserved value. Please use another key value.");
            }
            if (widths != null) {
                if (widths.size() != numColumns) {
                    throw new IllegalArgumentException(
                            "The columns widths size is %s. The number of columns is %s. These numbers must match.".formatted(
                                    widths.size(),
                                    numColumns));
                }
            }

            return new ColumnsComponent(this);
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

    protected TypeReference<Columns> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    public void beforeUse(final JtContainer container) {
        final JtContainer baseContainer = container.child(getKey());
        this.currentValue = new Columns(baseContainer, numColumns);
    }

    // Helper method for Mustache template to render widths as JSON array
    protected String getWidthsJson() {
        if (widths == null) {
            return null;
        }
        return toJson(widths);
    }


    public static final class Columns implements NotAState, JtLayout {
        private final List<JtContainer> backing;
        private final JtContainer layoutContainer;
        // helper data structure for mustache templates
        private final LinkedHashMap<Integer, JtContainer> indexedColumns;

        private Columns(final JtContainer baseContainer, final int numColumns) {
            this.layoutContainer = baseContainer;
            final List<JtContainer> columnsList = new ArrayList<>();
            for (int i = 0; i < numColumns; i++) {
                // CAUTION - the col_{{ i }} logic is duplicated in this class and both templates
                columnsList.add(baseContainer.child("col_" + i));
            }
            this.backing = columnsList;
            indexedColumns = new LinkedHashMap<>();
            for (int i = 0; i < backing.size(); i++) {
                indexedColumns.put(i, backing.get(i));
            }
        }

        public JtContainer col(final int index) {
            return backing.get(index);
        }

        // helper for mustache templates
        public LinkedHashMap<Integer, JtContainer> indexedColumns() {
            return indexedColumns;
        }

        @Override
        public JtContainer layoutContainer() {
            return layoutContainer;
        }
    }
}
