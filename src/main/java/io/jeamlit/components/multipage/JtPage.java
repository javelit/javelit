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

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static io.jeamlit.core.utils.EmojiUtils.ensureIsValidIcon;

public record JtPage(@Nonnull String fullyQualifiedName, @Nonnull String title, @Nonnull String icon,
                     @Nonnull String urlPath, boolean isHome,
                     // section path: List.of("Admin", "Users") would put the page in section Admin, subsection Users, etc...
                     List<String> section) {

    public static Builder builder(@Nonnull Class<?> page) {
        return new Builder(page);
    }

    public static class Builder {
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

        /**  make the page the default homepage */
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
            return new JtPage(pageApp.getName(), title, icon, urlPath, isHome, section);
        }

        // used internally by the navigation component to modify some pages if necessary
        protected boolean isHome() {
            return isHome;
        }


        protected Class<?> page() {
            return this.pageApp;
        }
    }
}
