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
package io.jeamlit.components.data;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import io.jeamlit.core.Shared;

import static io.jeamlit.core.utils.Preconditions.checkArgument;

public class TableComponent extends JtComponent<JtComponent.NONE> {

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/data/TableComponent.register.html.mustache");
        renderTemplate = mf.compile("components/data/TableComponent.render.html.mustache");
    }

    final @Nonnull List<String> columns;
    final @Nonnull String[][] values;

    private TableComponent(final @Nonnull Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE_VALUE, null);
        this.columns = builder.columns;
        this.values = builder.values;
    }

    public static class Builder extends JtComponentBuilder<JtComponent.NONE, TableComponent, Builder> {

        final Map<String, SequencedCollection<Object>> col2ListData;
        final Map<String, Object[]> col2ArrayData;
        final SequencedCollection<Object> listOfObjs;
        final Object[] arrayOfObjs;

        List<String> columns = null;
        String[][] values = null;

        private Builder(final @jakarta.annotation.Nullable Map<String, SequencedCollection<Object>> col2ListData,
                        final @jakarta.annotation.Nullable Map<String, Object[]> col2ArrayData,
                        final @jakarta.annotation.Nullable SequencedCollection<Object> objsList,
                        final @jakarta.annotation.Nullable Object[] objsArray) {
            this.col2ListData = col2ListData;
            this.col2ArrayData = col2ArrayData;
            this.listOfObjs = objsList;
            this.arrayOfObjs = objsArray;
        }

        /**
         * Creates a table from a map where each key is a column name and each value is a list of column data.
         * All columns must have the same number of elements.
         */
        @SuppressWarnings("unchecked")
        public static <Values extends @NotNull SequencedCollection<@Nullable Object>> Builder ofColumnsLists(@Nonnull Map<@NotNull String, Values> col2List) {
            return new Builder((Map<String, SequencedCollection<Object>>) col2List, null, null, null);
        }

        /**
         * Creates a table from a map where each key is a column name and each value is an array of column data.
         * All columns must have the same number of elements.
         */
        public static Builder ofColumnsArrays(@Nonnull Map<@NotNull String, @NotNull Object[]> col2Array) {
            return new Builder(null, col2Array, null, null);
        }

        /**
         * Creates a table from a list of objects, where each object represents a row and object properties become columns.
         * Objects are serialized to extract their fields as table columns.
         */
        public static Builder ofObjsList(@Nonnull SequencedCollection<Object> objsList) {
            return new Builder(null, null, objsList, null);
        }

        /**
         * Creates a table from an array of objects, where each object represents a row and object properties become columns.
         * Objects are serialized to extract their fields as table columns.
         */
        public static Builder ofObjsArray(@Nonnull Object[] objsArray) {
            return new Builder(null, null, null, objsArray);
        }

        @Override
        public TableComponent build() {
            Map<String, Object[]> colName2Column;
            if (col2ListData != null) {
                // use of LinkedHashMap to respect order if the user passed an ordered map
                colName2Column = new LinkedHashMap<>();
                for (final Map.Entry<String, SequencedCollection<Object>> entry : col2ListData.entrySet()) {
                    colName2Column.put(entry.getKey(), entry.getValue().toArray());
                }
            } else {
                colName2Column = col2ArrayData;
            }
            if (colName2Column != null) {
                this.columns = new ArrayList<>(colName2Column.keySet());
                if (!this.columns.isEmpty()) {
                    // will ensure all columns have the same size
                    final int valueCount = colName2Column.get(columns.getFirst()).length;
                    this.columns.forEach(colName -> {
                        final int colLength = colName2Column.get(colName).length;
                        checkArgument(colLength == valueCount,
                                      "Columns must have the same size. %s has size %s, %s has size %s.",
                                      columns.getFirst(),
                                      valueCount,
                                      colName,
                                      colLength);
                    });

                    this.values = new String[valueCount][];
                    for (int i = 0; i < valueCount; i++) {
                        final int idx = i;
                        this.values[i] = columns
                                .stream()
                                .map(colName2Column::get)
                                .map(e -> e[idx])
                                .map(v -> v == null ? null : String.valueOf(v))
                                .toArray(String[]::new);
                    }
                } else {
                    this.values = new String[0][0];
                }
            } else if (arrayOfObjs != null || listOfObjs != null) {
                final Object itertableOfObject = arrayOfObjs != null ? arrayOfObjs : listOfObjs;
                final List<Map<String, Object>> l = Shared.OBJECT_MAPPER.convertValue(itertableOfObject,
                                                                                      new TypeReference<>() {
                                                                                      });
                this.columns = l.stream().flatMap(m -> m.keySet().stream()).distinct().toList();
                this.values = l
                        .stream()
                        .map(m -> columns
                                .stream()
                                .map(m::get)
                                .map(v -> v == null ? null : String.valueOf(v))
                                .toArray(String[]::new))
                        .toArray(String[][]::new);
            }
            return new TableComponent(this);
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


    @SuppressWarnings("unused")     // used in templates
    String getColumnsJson() {
        return toJson(columns);
    }

    @SuppressWarnings("unused")     // used in templates
    String getValuesJson() {
        return toJson(values);
    }

}
