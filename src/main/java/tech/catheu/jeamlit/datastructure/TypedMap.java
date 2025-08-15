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
package tech.catheu.jeamlit.datastructure;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A Map wrapper that provides typed access methods for session state.
 * Implements the full Map interface while adding type-safe getters and compute methods.
 */
public class TypedMap implements Map<String, Object> {
    private final Map<String, Object> delegate;

    public TypedMap(final Map<String, Object> delegate) {
        this.delegate = delegate;
    }

    // Typed getters
    public String getString(String key) {
        return (String) delegate.get(key);
    }

    public String getString(String key, String defaultValue) {
        return (String) delegate.getOrDefault(key, defaultValue);
    }

    public Integer getInt(String key) {
        return (Integer) delegate.get(key);
    }

    public Integer getInt(String key, Integer defaultValue) {
        return (Integer) delegate.getOrDefault(key, defaultValue);
    }

    public Long getLong(String key) {
        return (Long) delegate.get(key);
    }

    public Long getLong(String key, Long defaultValue) {
        return (Long) delegate.getOrDefault(key, defaultValue);
    }

    public Double getDouble(String key) {
        return (Double) delegate.get(key);
    }

    public Double getDouble(String key, Double defaultValue) {
        return (Double) delegate.getOrDefault(key, defaultValue);
    }

    public Boolean getBoolean(String key) {
        return (Boolean) delegate.get(key);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return (Boolean) delegate.getOrDefault(key, defaultValue);
    }

    // Typed compute methods
    public String computeString(String key, BiFunction<String, String, String> remappingFunction) {
        return (String) delegate.compute(key, (k, v) -> remappingFunction.apply(k, (String) v));
    }

    public Integer computeInt(String key, BiFunction<String, Integer, Integer> remappingFunction) {
        return (Integer) delegate.compute(key, (k, v) -> remappingFunction.apply(k, (Integer) v));
    }

    public Long computeLong(String key, BiFunction<String, Long, Long> remappingFunction) {
        return (Long) delegate.compute(key, (k, v) -> remappingFunction.apply(k, (Long) v));
    }

    public Double computeDouble(String key, BiFunction<String, Double, Double> remappingFunction) {
        return (Double) delegate.compute(key, (k, v) -> remappingFunction.apply(k, (Double) v));
    }

    public Boolean computeBoolean(String key, BiFunction<String, Boolean, Boolean> remappingFunction) {
        return (Boolean) delegate.compute(key, (k, v) -> remappingFunction.apply(k, (Boolean) v));
    }

    // Typed computeIfPresent methods
    public String computeIfPresentString(String key, BiFunction<String, String, String> remappingFunction) {
        return (String) delegate.computeIfPresent(key,
                                                  (k, v) -> remappingFunction.apply(k, (String) v));
    }

    public Integer computeIfPresentInt(String key, BiFunction<String, Integer, Integer> remappingFunction) {
        return (Integer) delegate.computeIfPresent(key,
                                                   (k, v) -> remappingFunction.apply(k,
                                                                                     (Integer) v));
    }

    public Long computeIfPresentLong(String key, BiFunction<String, Long, Long> remappingFunction) {
        return (Long) delegate.computeIfPresent(key,
                                                (k, v) -> remappingFunction.apply(k, (Long) v));
    }

    public Double computeIfPresentDouble(String key, BiFunction<String, Double, Double> remappingFunction) {
        return (Double) delegate.computeIfPresent(key,
                                                  (k, v) -> remappingFunction.apply(k, (Double) v));
    }

    public Boolean computeIfPresentBoolean(String key, BiFunction<String, Boolean, Boolean> remappingFunction) {
        return (Boolean) delegate.computeIfPresent(key,
                                                   (k, v) -> remappingFunction.apply(k,
                                                                                     (Boolean) v));
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
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return delegate.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return delegate.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<Object> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
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
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return delegate.remove(key, value);
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public Object replace(String key, Object value) {
        return delegate.replace(key, value);
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ? extends Object> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public Object merge(String key, Object value, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return delegate.merge(key, value, remappingFunction);
    }
}
