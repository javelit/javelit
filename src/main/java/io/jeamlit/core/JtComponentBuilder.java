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
package io.jeamlit.core;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import io.jeamlit.core.utils.EmojiUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


// SELF is the self-referential generic to be able to return the correct implementing class in some methods of this abstract class
public abstract class JtComponentBuilder<B, T extends JtComponent<B>, SELF extends JtComponentBuilder<B, T, SELF>> {

    protected @Nullable String key;

    /**
     * A string to use as the unique key for the widget.
     * If this is omitted, a key will be generated for the widget based on its content.
     * No two widgets may have the same key.
     */
    @SuppressWarnings("unchecked")
    public SELF key(final String key) {
        this.key = key;
        return (SELF) this;
    }

    public abstract T build();

    /**
     * Implementation helper.
     * Uses reflection to construct a key value.
     * If set, the key field is used instead.
     * <p>
     * Note: pretty slow and hacky.
     * See streamlit implementation for reference: https://github.com/streamlit/streamlit/blob/4cc8cbccf529f351a29af88c15685a8a90153dd9/lib/streamlit/elements/lib/utils.py#L153
     **/
    public String generateKeyForInteractive() {
        if (key != null && !key.isBlank()) {
            return key;
        }
        try {
            final Class<?> clazz = this.getClass();
            // 2. Fallback: build a key from class name + fields
            final String baseName = clazz.getName().toLowerCase().replace("$builder", "");
            final Field[] fields = clazz.getDeclaredFields();
            Arrays.sort(fields, Comparator.comparing(Field::getName));
            final Object[] values = new Object[fields.length];
            // fixme cyril think of a size limit - this could get crazy big and slow - or hash but keep things understandable for the user
            for (int i = 0; i < fields.length; i++) {
                final Field field = fields[i];
                field.setAccessible(true);
                final Object value = field.get(this);
                values[i] = value;
            }

            return baseName + "_" + Objects.hash(values);

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to compute key", e);
        }
    }

    /**
     * Put the widget in the app, in the MAIN container.
     */
    public B use() {
        final T component = build();
        return component.use();
    }

    /**
     * Put the widget in the app, in the provided container.
     */
    public B use(final @Nonnull JtContainer container) {
        final T component = build();
        return component.use(container);
    }

    protected static void ensureIsValidIcon(@org.jetbrains.annotations.Nullable String icon) {
        EmojiUtils.ensureIsValidIcon(icon);
    }
}
