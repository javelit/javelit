package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Container implements JtComponent.NotAState {

    public static final Set<String> RESERVED_PATHS = Set.of("main", "sidebar");

    protected static final Container MAIN = new Container("main", null, false, false);
    protected static final Container SIDEBAR = new Container("sidebar", null, false, false);

    private final @Nonnull List<@NotNull String> path;
    private final @Nullable Container parent;
    private final boolean inPlace;
    private boolean formContainer = false;

    private Container(final @NotNull String key, @Nullable Container parent, final boolean inPlace, final boolean formContainer) {
        if (key.contains(",")) {
            throw new IllegalArgumentException(
                    "Container path cannot contain a comma. Please remove the comma from your key or container path.");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException(
                    "Container path cannot contain an empty string. Please remove the empty string from your key or container path.");
        }
        this.inPlace = inPlace;
        this.formContainer = formContainer;
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
        return new Container(key, this, false, false);
    }

    public final Container inPlaceChild(final @NotNull String key) {
        return new Container(key, this, true, false);
    }

    public final Container formChild(final @NotNull String key) {
        return new Container(key, this, false, true);
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
    
    /// Find the parent form component key by traversing up the container hierarchy
    /// If the Container is a form, returns its own key.
    /// Return null if there is no form in the parents.
    @Nullable
    public String getParentFormComponentKey() {
        Container current = this;
        while (current != null) {
            if (current.formContainer) {
                // Return the last element of the path (the form container's key)
                return current.path.getLast();
            }
            current = current.parent;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public @Nonnull String frontendDataContainerField() {
        return String.join(",", path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, parent, inPlace);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Container other)) {
            return false;
        }
        return this.path.equals(other.path)
               && Objects.equals(this.parent, other.parent)
               && this.inPlace == other.inPlace;
    }

    @Override
    public String toString() {
        // no parent and inPlace in the string representation, it will be confusing to users
        return String.join("->", path);
    }
}
