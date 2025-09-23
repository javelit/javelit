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
package io.jeamlit.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import static io.jeamlit.core.utils.Preconditions.checkArgument;

public final class JtContainer implements JtComponent.NotAState {

    public static final Set<String> RESERVED_PATHS = Set.of("main", "sidebar");

    public static final JtContainer MAIN = new JtContainer("main", null, false, false);
    public static final JtContainer SIDEBAR = new JtContainer("sidebar", null, false, false);

    private final @Nonnull List<@NotNull String> path;
    private final @Nullable JtContainer parent;
    private final boolean inPlace;
    private final boolean formContainer;

    private JtContainer(final @NotNull String key,
                        @Nullable JtContainer parent,
                        final boolean inPlace,
                        final boolean formContainer) {
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

    public final JtContainer child(final @NotNull String key) {
        return new JtContainer(key, this, false, false);
    }

    public final JtContainer inPlaceChild(final @NotNull String key) {
        return new JtContainer(key, this, true, false);
    }

    public final JtContainer formChild(final @NotNull String key) {
        final String parentFormComponentKey = this.getParentFormComponentKey();
        checkArgument(parentFormComponentKey == null,
                      "Attempting to create a form with key %s in a form %s. A form cannot be embedded inside another form.",
                      key,
                      parentFormComponentKey);
        return new JtContainer(key, this, false, true);
    }

    @Nonnull List<@NotNull String> path() {
        return path;
    }


    boolean isInPlace() {
        return inPlace;
    }

    // returns null if the Container has no parent (if main or sidebar)
    @Nullable JtContainer parent() {
        return parent;
    }

    /**
     * Find the parent form component key by traversing up the container hierarchy
     * If the Container is a form, returns its own key.
     * Return null if there is no form in the parents.
     */
    @Nullable
    public String getParentFormComponentKey() {
        JtContainer current = this;
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
        if (!(obj instanceof JtContainer other)) {
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
