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
package io.jeamlit.datastructure;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * A Map wrapper that provides typed access methods for session state.
 * Implements the full Map interface while adding type-safe getters and compute methods.
 * Supports optional key prefixing for page-scoped state management.
 */
public class TypedMap implements Map<String, Object> {
    private final Map<String, Object> delegate;
    private final String prefix;

    public TypedMap(final Map<String, Object> delegate) {
        this(delegate, "");
    }

    public TypedMap(final Map<String, Object> delegate, final String prefix) {
        this.delegate = delegate;
        this.prefix = prefix;
    }

    private String prefixKey(String key) {
        return prefix + key;
    }

    // Typed getters
    public String getString(String key) {
        return (String) delegate.get(prefixKey(key));
    }

    public String getString(String key, String defaultValue) {
        return (String) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public Integer getInt(String key) {
        return (Integer) delegate.get(prefixKey(key));
    }

    public Integer getInt(String key, Integer defaultValue) {
        return (Integer) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public Long getLong(String key) {
        return (Long) delegate.get(prefixKey(key));
    }

    public Long getLong(String key, Long defaultValue) {
        return (Long) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public Double getDouble(String key) {
        return (Double) delegate.get(prefixKey(key));
    }

    public Double getDouble(String key, Double defaultValue) {
        return (Double) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public Boolean getBoolean(String key) {
        return (Boolean) delegate.get(prefixKey(key));
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return (Boolean) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public String getOrDefaultString(String key, String defaultValue) {
        return (String) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public Integer getOrDefaultInt(String key, Integer defaultValue) {
        return (Integer) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public Long getOrDefaultLong(String key, Long defaultValue) {
        return (Long) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public Double getOrDefaultDouble(String key, Double defaultValue) {
        return (Double) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public Boolean getOrDefaultBoolean(String key, Boolean defaultValue) {
        return (Boolean) delegate.getOrDefault(prefixKey(key), defaultValue);
    }

    public String putIfAbsentString(String key, String value) {
        return (String) delegate.putIfAbsent(prefixKey(key), value);
    }

    public Integer putIfAbsentInt(String key, Integer value) {
        return (Integer) delegate.putIfAbsent(prefixKey(key), value);
    }

    public Long putIfAbsentLong(String key, Long value) {
        return (Long) delegate.putIfAbsent(prefixKey(key), value);
    }

    public Double putIfAbsentDouble(String key, Double value) {
        return (Double) delegate.putIfAbsent(prefixKey(key), value);
    }

    public Boolean putIfAbsentBoolean(String key, Boolean value) {
        return (Boolean) delegate.putIfAbsent(prefixKey(key), value);
    }

    // Typed compute methods
    public String computeString(String key, BiFunction<String, String, String> remappingFunction) {
        return (String) delegate.compute(prefixKey(key), (k, v) -> remappingFunction.apply(k, (String) v));
    }

    public Integer computeInt(String key, BiFunction<String, Integer, Integer> remappingFunction) {
        return (Integer) delegate.compute(prefixKey(key), (k, v) -> remappingFunction.apply(k, (Integer) v));
    }

    public Long computeLong(String key, BiFunction<String, Long, Long> remappingFunction) {
        return (Long) delegate.compute(prefixKey(key), (k, v) -> remappingFunction.apply(k, (Long) v));
    }

    public Double computeDouble(String key, BiFunction<String, Double, Double> remappingFunction) {
        return (Double) delegate.compute(prefixKey(key), (k, v) -> remappingFunction.apply(k, (Double) v));
    }

    public Boolean computeBoolean(String key, BiFunction<String, Boolean, Boolean> remappingFunction) {
        return (Boolean) delegate.compute(prefixKey(key), (k, v) -> remappingFunction.apply(k, (Boolean) v));
    }

    // Typed computeIfPresent methods
    public String computeIfPresentString(String key, BiFunction<String, String, String> remappingFunction) {
        return (String) delegate.computeIfPresent(prefixKey(key),
                                                  (k, v) -> remappingFunction.apply(k, (String) v));
    }

    public Integer computeIfPresentInt(String key, BiFunction<String, Integer, Integer> remappingFunction) {
        return (Integer) delegate.computeIfPresent(prefixKey(key),
                                                   (k, v) -> remappingFunction.apply(k,
                                                                                     (Integer) v));
    }

    public Long computeIfPresentLong(String key, BiFunction<String, Long, Long> remappingFunction) {
        return (Long) delegate.computeIfPresent(prefixKey(key),
                                                (k, v) -> remappingFunction.apply(k, (Long) v));
    }

    public Double computeIfPresentDouble(String key, BiFunction<String, Double, Double> remappingFunction) {
        return (Double) delegate.computeIfPresent(prefixKey(key),
                                                  (k, v) -> remappingFunction.apply(k, (Double) v));
    }

    public Boolean computeIfPresentBoolean(String key, BiFunction<String, Boolean, Boolean> remappingFunction) {
        return (Boolean) delegate.computeIfPresent(prefixKey(key),
                                                   (k, v) -> remappingFunction.apply(k,
                                                                                     (Boolean) v));
    }

    // Typed computeIfAbsent methods
    public String computeIfAbsentString(String key, Function<String, String> mappingFunction) {
        return (String) delegate.computeIfAbsent(prefixKey(key), mappingFunction);
    }

    public Integer computeIfAbsentInt(String key, Function<String, Integer> mappingFunction) {
        return (Integer) delegate.computeIfAbsent(prefixKey(key), mappingFunction);
    }

    public Long computeIfAbsentLong(String key, Function<String, Long> mappingFunction) {
        return (Long) delegate.computeIfAbsent(prefixKey(key), mappingFunction);
    }

    public Double computeIfAbsentDouble(String key, Function<String, Double> mappingFunction) {
        return (Double) delegate.computeIfAbsent(prefixKey(key), mappingFunction);
    }

    // Delegate all Map methods
    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return delegate.containsKey(prefixKey((String) key));
        }
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            return delegate.get(prefixKey((String) key));
        }
        return delegate.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return delegate.put(prefixKey(key), value);
    }

    @Override
    public Object remove(Object key) {
        if (key instanceof String) {
            return delegate.remove(prefixKey((String) key));
        }
        return delegate.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends Object> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public @NotNull Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public @NotNull Collection<Object> values() {
        return delegate.values();
    }

    @Override
    public @NotNull Set<Entry<String, Object>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TypedMap otherMap) {
            return delegate.equals(otherMap.delegate);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    // Additional Map methods from Java 8+
    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        if (key instanceof String) {
            return delegate.getOrDefault(prefixKey((String) key), defaultValue);
        }
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        delegate.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ? extends Object> function) {
        delegate.replaceAll(function);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return delegate.putIfAbsent(prefixKey(key), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key instanceof String) {
            return delegate.remove(prefixKey((String) key), value);
        }
        return delegate.remove(key, value);
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        return delegate.replace(prefixKey(key), oldValue, newValue);
    }

    @Override
    public Object replace(String key, Object value) {
        return delegate.replace(prefixKey(key), value);
    }

    @Override
    public Object computeIfAbsent(String key, @NotNull Function<? super String, ? extends Object> mappingFunction) {
        return delegate.computeIfAbsent(prefixKey(key), mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key,
                                   @NotNull BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
        return delegate.computeIfPresent(prefixKey(key), remappingFunction);
    }

    @Override
    public Object compute(String key, @NotNull BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
        return delegate.compute(prefixKey(key), remappingFunction);
    }

    @Override
    public Object merge(String key,
                        @NotNull Object value,
                        @NotNull BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return delegate.merge(prefixKey(key), value, remappingFunction);
    }
}
