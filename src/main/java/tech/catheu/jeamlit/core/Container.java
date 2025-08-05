package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Container implements JtComponent.NotAState {

    public static final Set<String> RESERVED_PATHS = Set.of("main", "sidebar");

    protected static final Container MAIN = new Container("main", null, false);
    protected static final Container SIDEBAR = new Container("sidebar", null, false);

    private final @Nonnull List<@NotNull String> path;
    private final @Nullable Container parent;
    private final boolean inPlace;

    private Container(final @NotNull String key, @Nullable Container parent, final boolean inPlace) {
        if (key.contains(",")) {
            throw new IllegalArgumentException(
                    "Container path cannot contain a comma. Please remove the comma from your key or container path.");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException(
                    "Container path cannot contain an empty string. Please remove the empty string from your key or container path.");
        }
        this.inPlace = inPlace;
        this.parent = parent;
        if (this.parent == null) {
            this.path = List.of(key);
        } else {
            final ArrayList<String> tempPath = new ArrayList<>(parent.path.size() + 1);
            tempPath.addAll(parent.path);
            tempPath.add(key);
            this.path = List.copyOf(tempPath);
        }
    }

    public final Container child(final @NotNull String key) {
        return new Container(key, this, false);
    }

    public final Container inPlaceChild(final @NotNull String key) {
        return new Container(key, this, true);
    }

    protected @Nonnull List<@NotNull String> path() {
        return path;
    }


    protected boolean isInPlace() {
        return inPlace;
    }

    // returns null if the Container has no parent (if main or sidebar)
    protected final @Nullable Container parent() {
        return parent;
    }

    @SuppressWarnings("unused")
    public @Nonnull String frontendDataContainerField() {
        return String.join(",", path);
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
