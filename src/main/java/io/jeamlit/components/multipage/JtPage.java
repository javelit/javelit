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
package io.jeamlit.components.multipage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import io.jeamlit.core.PageRunException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static io.jeamlit.core.utils.EmojiUtils.ensureIsValidIcon;

public final class JtPage {
    @Nonnull private final Class<?> pageApp;
    @Nonnull private final String title;
    @Nonnull private final String icon;
    @Nonnull private final String urlPath;
    private final boolean isHome;
    private final List<String> section;

    private JtPage(final @Nonnull Builder builder) {
        this.pageApp = builder.pageApp;
        this.title = builder.title;
        this.icon = builder.icon;
        this.urlPath = builder.urlPath;
        this.isHome = builder.isHome;
        this.section = builder.section;
    }

    public static Builder builder(@Nonnull Class<?> page) {
        return new Builder(page);
    }

    // for the moment there is no known public case for getting this field
    // the user should call run() instead
    @Nonnull
    Class<?> pageApp() {
        return pageApp;
    }

    @Nonnull
    public String title() {
        return title;
    }

    @Nonnull
    public String icon() {
        return icon;
    }

    @Nonnull
    public String urlPath() {
        return urlPath;
    }

    public boolean isHome() {
        return isHome;
    }

    public List<String> section() {
        return section;
    }

    public void run() {
        callMainMethod(pageApp);
    }

    private static void callMainMethod(final @Nonnull Class<?> clazz) {
        final Method pageMethod;
        try {
            pageMethod = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new PageRunException(e);
        }
        try {
            pageMethod.invoke(null, new Object[]{new String[]{}});
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PageRunException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (JtPage) obj;
        return Objects.equals(this.pageApp, that.pageApp) && Objects.equals(this.title, that.title) && Objects.equals(
                this.icon,
                that.icon) && Objects.equals(this.urlPath,
                                             that.urlPath) && this.isHome == that.isHome && Objects.equals(this.section,
                                                                                                           that.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageApp, title, icon, urlPath, isHome, section);
    }

    @Override
    public String toString() {
        return "JtPage[" + "pageApp=" + pageApp + ", " + "title=" + title + ", " + "icon=" + icon + ", " + "urlPath=" + urlPath + ", " + "isHome=" + isHome + ", " + "section=" + section + ']';
    }


    public static final class Builder {
        private final @Nonnull Class<?> pageApp;
        private String title;
        private String icon;
        private String urlPath;
        private boolean isHome;
        private List<String> section;

        public Builder(@Nonnull Class<?> pageApp) {
            this.pageApp = pageApp;
        }

        public Builder title(final @Nonnull String title) {
            this.title = title;
            return this;
        }

        /**
         * An icon to display with the error message. The following values are valid:
         * <ul>
         *     <li>A single-character emoji. For example: {@code 🔥}. Emoji short codes are not supported.</li>
         *     <li>An icon from the Material Symbols library (rounded style) in the format {@code :icon_name:} where {@code icon_name} is the name of the icon in snake case. For example: {@code :search:}. See full list of icons <a href="https://fonts.google.com/icons?icon.set=Material+Symbols&icon.style=Rounded&selected=Material+Symbols+Rounded:search:FILL@0;wght@400;GRAD@0;opsz@24&icon.size=24&icon.color=%231f1f1f" target="_blank">here</a>.</li>
         * </ul>
         * If {@code null} (default), no icon is displayed.
         */
        public Builder icon(final @Nullable String icon) {
            ensureIsValidIcon(icon);
            this.icon = icon;
            return this;
        }

        public Builder urlPath(final @Nonnull String urlPath) {
            String cleanedUrl = urlPath.trim();
            if (!cleanedUrl.startsWith("/")) {
                cleanedUrl = "/" + urlPath;
            }
            if (cleanedUrl.endsWith("/")) {
                cleanedUrl = cleanedUrl.substring(0, cleanedUrl.length() - 1);
            }
            this.urlPath = cleanedUrl;

            return this;
        }

        /**
         * make the page the default homepage
         */
        public Builder home() {
            this.isHome = true;
            return this;
        }

        // TODO later - support subsections
        // public Builder section(final @Nonnull List<String> section) {
        //     this.section = section;
        //     return this;
        // }

        public Builder section(final @Nonnull String section) {
            this.section = List.of(section);
            return this;
        }

        public JtPage build() {
            if (title == null) {
                title = pageApp.getSimpleName();
            }
            if (urlPath == null) {
                urlPath = "/" + pageApp.getSimpleName();
            }
            return new JtPage(this);
        }

        // used internally by the navigation component to modify some pages if necessary
        boolean isHome() {
            return isHome;
        }


        Class<?> page() {
            return this.pageApp;
        }
    }
}
