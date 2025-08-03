package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Container implements JtComponent.NotAState {

    public static final Set<String> RESERVED_PATHS = Set.of("main", "sidebar");

    static final Container MAIN = new Container(List.of("main"));

    static final Container SIDEBAR = new Container(List.of("sidebar"));

    protected @Nonnull List<@NotNull String> path() {
        return path;
    }

    @SuppressWarnings("unused")
    public @Nonnull String frontendDataContainerField() {
        return String.join(",", path);
    }

    private final @Nonnull List<@NotNull String> path;

    protected Container(@Nonnull List<@NotNull String> path) {
        final boolean containsComma = path.stream().anyMatch(e -> e.contains(","));
        if (containsComma) {
            throw new IllegalArgumentException(
                    "Container path cannot contain a comma. Please remove the comma from your key or container path.");
        }
        final boolean containsEmpty = path.stream().anyMatch(String::isEmpty);
        if (containsEmpty) {
            throw new IllegalArgumentException(
                    "Container path cannot contain an empty string. Please remove the empty string from your key or container path.");
        }
        this.path = List.copyOf(path);
    }


    public final Container with(final @NotNull String key) {
        final ArrayList<String> res = new ArrayList<>(path.size());
        res.addAll(path);
        res.add(key);
        return new Container(res);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Container)) {
            return false;
        }
        return path.equals(((Container) obj).path);
    }

    @Override
    public String toString() {
        return String.join("->", path);
    }
}
