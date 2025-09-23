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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import io.jeamlit.core.JtContainer;
import io.jeamlit.core.JtLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public final class ColumnsComponent extends JtComponent<ColumnsComponent.Columns> {

    final int numColumns;
    final @Nullable List<@NotNull Double> widths;

    public enum Gap {
        SMALL,
        MEDIUM,
        LARGE,
        NONE
    }

    final Gap gap;
    final VerticalAlignment verticalAlignment;

    public enum VerticalAlignment {
        TOP,
        CENTER,
        BOTTOM
    }

    final boolean border;

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

        /**
         * The number of columns to create. Must be between 2 and 12 inclusive. Each column will have equal width
         * unless custom widths are specified.
         */
        public Builder numColumns(final int numColumns) {
            if (numColumns < 2 || numColumns > 12) {
                throw new IllegalArgumentException("numColumns must be in [2, 12]");
            }
            this.numColumns = numColumns;
            return this;
        }

        /**
         * A list of relative column widths. The list size must match the number of columns. For example, [0.7, 0.3]
         * creates two columns with 70% and 30% width respectively. If not specified, all columns have equal width.
         */
        public Builder widths(final @Nullable List<@NotNull Double> widths) {
            this.widths = widths;
            return this;
        }

        /**
         * Controls the space between columns. Options are {@code SMALL} ({@code 1rem} gap, default), {@code MEDIUM} ({@code 2rem} gap),
         * {@code LARGE} ({@code 4rem} gap), or {@code NONE} (no gap between columns).
         */
        public Builder gap(final @Nonnull Gap gap) {
            this.gap = gap;
            return this;
        }

        /**
         * The vertical alignment of the content inside the columns. Options are {@code TOP} (default), {@code CENTER}, or {@code BOTTOM}.
         */
        public Builder verticalAlignment(final @Nonnull VerticalAlignment verticalAlignment) {
            this.verticalAlignment = verticalAlignment;
            return this;
        }

        /**
         * Whether to show a border around the column containers.
         * If this is {@code false} (default), no border is shown. If this is {@code true},
         * a border is shown around each column.
         */
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

    @Override
    protected TypeReference<Columns> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    protected void beforeUse(final JtContainer container) {
        final JtContainer baseContainer = container.child(getKey());
        this.currentValue = new Columns(baseContainer, numColumns);
    }

    // Helper method for Mustache template to render widths as JSON array
    String getWidthsJson() {
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
        public Map<Integer, JtContainer> indexedColumns() {
            return indexedColumns;
        }

        @Override
        public JtContainer layoutContainer() {
            return layoutContainer;
        }
    }
}
