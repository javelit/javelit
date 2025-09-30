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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.jeamlit.core.Jt;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import io.jeamlit.core.JtContainer;
import io.jeamlit.core.Shared;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public final class NavigationComponent extends JtComponent<JtPage> {

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    final List<JtPage> pages;
    final JtPage home;
    NavigationPosition position;

    private final Map<String, Class<?>> classNameToClass = new HashMap<>();

    public enum NavigationPosition {
        SIDEBAR,
        HIDDEN,
        TOP
    }

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/multipage/NavigationComponent.register.html.mustache");
        renderTemplate = mf.compile("components/multipage/NavigationComponent.render.html.mustache");
    }

    private NavigationComponent(final Builder builder) {
        super(UNIQUE_NAVIGATION_COMPONENT_KEY, null, // set later in this constructor
              null, builder.position == NavigationPosition.HIDDEN ? JtContainer.MAIN : JtContainer.SIDEBAR);
        final List<JtPage.Builder> homePages = builder.pageBuilders.stream().filter(JtPage.Builder::isHome).toList();
        if (homePages.isEmpty()) {
            JtPage.Builder firstPageBuilder = builder.pageBuilders.getFirst();
            firstPageBuilder.home();
        } else if (homePages.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple pages are defined as home: %s. Only one page should be defined as home.".formatted(String.join(
                            ", ",
                            homePages.stream().map(e -> e.page().getSimpleName()).toList())));

        }
        builder.pageBuilders.forEach(e -> classNameToClass.put(e.page().getName(), e.page()));
        this.pages = builder.pageBuilders.stream().map(JtPage.Builder::build).collect(Collectors.toList());
        this.home = this.pages.stream().filter(JtPage::isHome).findFirst().orElseThrow(() -> new RuntimeException("Home page not found. Implementation error. Please reach out to support."));
        this.position = builder.position;

        // Set initial page based on current URL, not always home
        final String currentPath = getCurrentPath();
        this.currentValue = getPageFor(currentPath);
    }

    public JtPage getHome() {
        return home;
    }

    /**
     * Determines the initial page based on current URL path.
     * Falls back to home page if no URL match is found.
     */
    private JtPage getPageFor(final @Nonnull String urlPath) {
        if (urlPath.isBlank() || "/".equals(urlPath)) {
            return home;
        }
        for (final JtPage page : pages) {
            if (page.urlPath().equals(urlPath)) {
                return page;
            }
        }
        // 404
        return JtPage.builder(NotFoundPageApp.class).title("Page not found").urlPath(urlPath).build();
    }

    public @Nullable JtPage getPageFor(final @Nullable Class<?> pageApp) {
        if (pageApp == null) {
            return home;
        }
        for (final JtPage page : pages) {
            if (page.pageApp().equals(pageApp)) {
                return page;
            }
        }
        // unknow app
        return null;
    }


    public static class Builder extends JtComponentBuilder<JtPage, NavigationComponent, Builder> {

        private final List<JtPage.Builder> pageBuilders = new ArrayList<>();
        private NavigationPosition position;

        public Builder(JtPage.Builder... pages) {
            Collections.addAll(this.pageBuilders, pages);
        }

        /**
         * Adds a page to the navigation. Pages can be added individually using this method or passed in the constructor.
         */
        public Builder addPage(final @Nonnull JtPage.Builder page) {
            pageBuilders.add(page);
            return this;
        }

        /**
         * Hides the navigation menu from the user interface. The pages will still be accessible programmatically,
         * but no navigation UI will be displayed. Useful for programmatic navigation or single-page apps.
         */
        public Builder hidden() {
            position = NavigationPosition.HIDDEN;
            return this;
        }


        @Override
        public NavigationComponent build() {
            return new NavigationComponent(this);
        }
    }


    @Override
    protected String register() {
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected TypeReference<JtPage> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    protected void beforeUse(@NotNull JtContainer container) {
        if (pages.size() <= 1 || position == NavigationPosition.HIDDEN) {
            position = NavigationPosition.HIDDEN;
            return;
        }

        if (container.equals(JtContainer.SIDEBAR)) {
            position = NavigationPosition.SIDEBAR;
        } else if (container.equals(JtContainer.MAIN)) {
            position = NavigationPosition.TOP;
            throw new UnsupportedOperationException(
                    "Navigation component in the main container is not supported yet. Please reach out to support for more information.");
        } else {
            throw new IllegalArgumentException(
                    "Navigation component can only be used within the SIDEBAR (JtContainer.SIDEBAR) or the MAIN (JtContainer.MAIN) containers.");
        }
    }

//    @Override
//    protected JtPage convert(Object rawValue) {
//        try {
//            final FrontendJtPage frontendJtPage = Shared.OBJECT_MAPPER.convertValue(rawValue, FrontendJtPage.class);
//            final Class<?> pageApp = classNameToClass.get(frontendJtPage.fullyQualifiedName());
//            if (pageApp != null) {
//                return getPageFor(pageApp);
//            } else {
//                // 404
//                return JtPage.builder(NotFoundPageApp.class).title("Page not found").build();
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException(
//                    "Failed to parse input widget value coming from the app. Please reach out to support.",
//                    e);
//        }
//    }

    public String getPagesJson() {
        try {
            return Shared.OBJECT_MAPPER.writeValueAsString(pages.stream().map(FrontendJtPage::from).toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize pages", e);
        }
    }

    public String getCurrentValueJson() {
        try {
            return Shared.OBJECT_MAPPER.writeValueAsString(FrontendJtPage.from(currentValue));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize currentValue", e);
        }
    }


    private record FrontendJtPage(@Nonnull String fullyQualifiedName, @Nonnull String title, @Nonnull String icon,
                                  @Nonnull String urlPath, boolean isHome,
                                  // section path: List.of("Admin", "Users") would put the page in section Admin, subsection Users, etc...
                                  List<String> section) {
        private static FrontendJtPage from(final @Nonnull JtPage page) {
            return new FrontendJtPage(page.pageApp().getName(),
                                      page.title(),
                                      page.icon(),
                                      page.urlPath(),
                                      page.isHome(),
                                      page.section());
        }
    }

    public static class NotFoundPageApp {

        public static void main(String[] args) {
            Jt.title("Page Not Found.").use();
            if (Jt.button("Go to home").use()) {
                Jt.switchPage(null);
            }
        }
    }
}
