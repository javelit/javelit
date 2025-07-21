package tech.catheu.jeamlit.core;

import java.lang.reflect.Field;
import java.util.StringJoiner;

public interface JtComponentBuilder<T extends JtComponent> {
    T build();

    /**
     * Implementation helper.
     * Uses reflection to construct a key value.
     * If there is a key field, considers it is the key override field and returns it.
     * <p>
     * Note: pretty slow and hacky.
     **/
    default String generateKey() {
        try {
            final Class<?> clazz = this.getClass();
            // 1. Try to get a field named "key"
            try {
                final Field keyField = clazz.getDeclaredField("key");
                if (keyField.getType().equals(String.class)) {
                    keyField.setAccessible(true);
                    final String key = (String) keyField.get(this);
                    if (key != null && !key.isBlank()) {
                        return key;
                    }
                }
            } catch (NoSuchFieldException ignored) {
                // fall back to default behavior
            }

            // 2. Fallback: build a key from class name + fields
            final String baseName = clazz.getSimpleName().toLowerCase().replace("builder", "");
            final Field[] fields = clazz.getDeclaredFields();
            final StringJoiner joiner = new StringJoiner("_", baseName + "_", "");
            for (final Field field : fields) {
                field.setAccessible(true);
                final Object value = field.get(this);
                if (value != null) {
                    joiner.add(value.toString());
                }
            }

            return joiner.toString();

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to compute key", e);
        }
    }
}
