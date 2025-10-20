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
package io.javelit.core;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.javelit.core.utils.EmojiUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


// SELF is the self-referential generic to be able to return the correct implementing class in some methods of this abstract class
public abstract class JtComponentBuilder<B, T extends JtComponent<B>, SELF extends JtComponentBuilder<B, T, SELF>> {

    private static final Map<Class<?>, Field[]> FIELDS_CACHE = new ConcurrentHashMap<>();

    private static final HashFunction HF = Hashing.murmur3_128(31);

    protected @Nullable String userKey;

    boolean noPersist;

    protected JtComponentBuilder() {
        StateManager.recordComponentInstantiation(usageRecordName());
    }

    // WARNING - will get broken if multiple inheritance level of builders are introduced
    private static Field[] getFields(final @Nonnull Class<?> clazz) {
        // this only retrieves fields defined directly in the implem - does not retrieve JtComponentBuilder inherited field
        final Field[] f = clazz.getDeclaredFields();
        Arrays.sort(f, Comparator.comparing(Field::getName));
        for (final Field field : f) {
            field.setAccessible(true);
        }
        return f;
    }

    /**
     * A string to use as the unique key for the widget.
     * If this is omitted, a key will be generated for the widget based on its content.
     * No two widgets may have the same key.
     */
    @SuppressWarnings("unchecked")
    public SELF key(final @Nonnull String key) {
        if (JtContainer.RESERVED_PATHS.contains(key)) {
            throw new IllegalArgumentException("Component key value `" + key + "` is a reserved value. Please use another key value.");
        }
        this.userKey = key;
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public SELF noPersist() {
        this.noPersist = true;
        return (SELF) this;
    }

    public abstract T build();

    /**
     * Implementation helper.
     * Uses reflection to construct a key value from userKey, className and other fields hash
     * <p>
     * See <a href="https://github.com/streamlit/streamlit/blob/4cc8cbccf529f351a29af88c15685a8a90153dd9/lib/streamlit/elements/lib/utils.py#L153">streamlit implementation</a> for reference.
     **/
    protected String generateInternalKey() {
        try {
            final Class<?> clazz = this.getClass();
            final Field[] fields = FIELDS_CACHE.computeIfAbsent(clazz, JtComponentBuilder::getFields);
            int numInheritedFields = 1;
            final Object[] values = new Object[fields.length + numInheritedFields];
            int i = 0;
            for (final Field field: fields) {
                final Object fieldValue = field.get(this);
                values[i++] = fieldValue;
            }
            // add inherited fields - don't add userKey it's kept it clear, see below
            values[i++] = noPersist;

            final String baseName = clazz.getName().toLowerCase(Locale.ROOT).replace("$builder", "");
            final String pagePrefix = StateManager.pagePrefix();
            // Arrays.deepToString may not be optimal in terms of speed here
            final String valuesHash = HF.hashString(Arrays.deepToString(values), StandardCharsets.UTF_8).toString();
            return "%s%s_%s_%s".formatted(pagePrefix, userKey != null ? userKey : "noCustomKey", baseName, valuesHash);

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to compute key", e);
        }
    }

    /**
     * Put the widget in the app, in the {@code MAIN} container.
     */
    public final B use() {
        StateManager.recordComponentUsed(this.getClass().getSimpleName());
        final T component = build();
        return component.use();
    }

    /**
     * Put the widget in the app, in the provided container.
     */
    public final B use(final @Nonnull JtContainer container) {
        final T component = build();
        return component.use(container);
    }

    protected String usageRecordName() {
        return this.getClass().getName().replace("$Builder", "");
    }

    protected static void ensureIsValidIcon(@org.jetbrains.annotations.Nullable String icon) {
        EmojiUtils.ensureIsValidIcon(icon);
    }
}
