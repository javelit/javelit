package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.StringJoiner;
import java.util.UUID;


// SELF is the self-referential generic to be able to return the correct implementing class in some methods of this abstract class
public abstract class JtComponentBuilder<B, T extends JtComponent<B>, SELF extends JtComponentBuilder<B, T, SELF>> {

    protected @Nullable String key;

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
     * Implementation helper.
     * Returns a random key. It shouldn't be an issue for non-interactive field.
     * If set, the key field is used instead.
     **/
    public String generateKeyForNonInteractive() {
        if (key != null && !key.isBlank()) {
            return key;
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Shorthand for build().use()
     */
    public B use() {
        final T component = build();
        return component.use();
    }

    /**
     * Shorthand for build().use(Layout)
     */
    public B use(final @Nonnull Layout layout) {
        final T component = build();
        return component.use(layout);
    }
}
