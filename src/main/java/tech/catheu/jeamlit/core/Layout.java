package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Layout implements JtComponent.NotAState {

    private static final Set<String> RESERVED_PATHS = Set.of("main", "sidebar");

    static final Layout MAIN =  new Layout(List.of("main"));

    static final Layout SIDEBAR =  new Layout(List.of("sidebar"));

    protected @Nonnull List<@NotNull String> path() {
        return path;
    }

    @SuppressWarnings("unused")
    public @Nonnull String frontendDataLayoutField() {
        return String.join(",", path);
    }

    private final @Nonnull List<@NotNull String> path;

    protected Layout(@Nonnull List<@NotNull String> path) {
        final boolean containsComma = path.stream().anyMatch(e -> e.contains(","));
        if (containsComma) {
            throw new IllegalArgumentException("Layout path cannot contain a comma. Please remove the comma from your key or layout path.");
        }
        final boolean containsEmpty = path.stream().anyMatch(String::isEmpty);
        if (containsEmpty) {
            throw new IllegalArgumentException("Layout path cannot contain an empty string. Please remove the empty string from your key or layout path.");
        }
        this.path = List.copyOf(path);
    }


    public final Layout with(final @NotNull String key) {
        final ArrayList<String> res = new ArrayList<>(path.size());
        res.addAll(path);
        res.add(key);
        return new Layout(res);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return path.equals(obj);
    }

    @Override
    public String toString() {
        return String.join("->", path);
    }
}
