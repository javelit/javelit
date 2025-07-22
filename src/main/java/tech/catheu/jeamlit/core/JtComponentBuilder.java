package tech.catheu.jeamlit.core;

import java.lang.reflect.Field;
import java.util.StringJoiner;

public interface JtComponentBuilder<B, T extends JtComponent<B>> {
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
            final String baseName = clazz.getName().toLowerCase().replace("$builder", "");
            final Field[] fields = clazz.getDeclaredFields();
            final StringJoiner joiner = new StringJoiner("_", baseName + "_", "");
            // fixme cyril think of a size limit - this could get crazy big and slow - or hash but keep things understandable for the user
            for (final Field field : fields) {
                field.setAccessible(true);
                final Object value = field.get(this);
                if (value != null) {
                    joiner.add(value.toString());
                }
            }

            // fixme cyril think of a size limit - this could get crazy big - or hash but keep things understandable for the user
            return joiner.toString();

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to compute key", e);
        }
    }

    /**
     * Shorthand for build().use()
     */
    default B use() {
        final T component = build();
        return component.use();
    }
}
