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
package tech.catheu.jeamlit.components.multipage;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import tech.catheu.jeamlit.core.Jt;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import tech.catheu.jeamlit.core.JtContainer;
import tech.catheu.jeamlit.core.PageRunException;
import tech.catheu.jeamlit.core.Shared;

import static tech.catheu.jeamlit.core.utils.Preconditions.checkArgument;

public final class NavigationComponent extends JtComponent<JtPage> {

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    protected final List<JtPage> pages;
    protected final JtPage home;
    protected NavigationPosition position;
    protected final JtPage pageNotFound = JtPage.builder(NotFoundPage.class).title("Page not found").build();

    public static class NotFoundPage {
        public static void main(String[] args) {
            Jt.title("Page Not Found.");
        }
    }

    private final Map<String, Class<?>> classNameToClass = new HashMap<>();

    public enum NavigationPosition {
        SIDEBAR,
        TOP_IN_MAIN,
        HIDDEN
    }

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile(
                "components/multipage/NavigationComponent.register.html.mustache");
        renderTemplate = mf.compile("components/multipage/NavigationComponent.render.html.mustache");
    }

    private NavigationComponent(final Builder builder) {
        super("THERE_CAN_ONLY_BE_ONE_NAVIGATION_COMPONENT",
              null, // set later in this constructor
              null,
              JtContainer.SIDEBAR);
        final List<JtPage.Builder> homePages = builder.pageBuilders.stream()
                .filter(JtPage.Builder::isHome)
                .toList();
        if (homePages.isEmpty()) {
            builder.pageBuilders.getFirst().home();
        } else if (homePages.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple pages are defined as home: %s. Only one page should be defined as home."
                            .formatted(String.join(", ",
                                                   homePages.stream()
                                                           .map(e -> e.page().getSimpleName())
                                                           .toList())));

        }
        this.home = homePages.getFirst().build();
        builder.pageBuilders.forEach(e -> classNameToClass.put(e.page().getName(), e.page()));
        this.pages = builder.pageBuilders.stream().map(JtPage.Builder::build)
                .collect(Collectors.toList());
        this.position = builder.position;

        // Set initial page based on current URL, not always home
        this.currentValue = determineInitialPage();
    }

    /**
     * Determines the initial page based on current URL path.
     * Falls back to home page if no URL match is found.
     */
    private JtPage determineInitialPage() {
        final @Nonnull String currentPath = this.getCurrentPath();
        if (currentPath.isBlank() || "/".equals(currentPath)) {
            return home;
        }
        for (final JtPage page : pages) {
            if (page.url().equals(currentPath)) {
                return page;
            }
        }
        // 404
        return null;
    }


    public static class Builder extends JtComponentBuilder<JtPage, NavigationComponent, Builder> {

        private final List<JtPage.Builder> pageBuilders = new ArrayList<>();
        private NavigationPosition position;

        public Builder(JtPage.Builder... pages) {
            Collections.addAll(this.pageBuilders, pages);
        }

        public Builder addPage(final @Nonnull JtPage.Builder page) {
            pageBuilders.add(page);
            return this;
        }

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

        if (container == JtContainer.SIDEBAR) {
            position = NavigationPosition.SIDEBAR;
        } else if (container == JtContainer.MAIN) {
            position = NavigationPosition.TOP_IN_MAIN;
            throw new UnsupportedOperationException(
                    "Navigation component in the main container is not supported yet. Please reach out to support for more information.");
        } else {
            throw new IllegalArgumentException(
                    "Navigation component can only be used within the SIDEBAR (JtContainer.SIDEBAR) or the MAIN (JtContainer.MAIN) containers.");
        }
    }

    @Override
    protected void afterUse(@NotNull JtContainer container) {
        if (currentValue != null) {
            final Class<?> clazz = classNameToClass.get(currentValue.fullyQualifiedName());
            checkArgument(clazz != null,
                          "Unknown page: %s. Please reach out to support",
                          currentValue.fullyQualifiedName());
            callMainMethod(clazz);
        } else {
            // TODO can be improved and made customizable later
            Jt.title("Page Not Found.").use();
        }
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


    public String getPagesJson() {
        try {
            return Shared.OBJECT_MAPPER.writeValueAsString(pages);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize pages", e);
        }
    }

    public String getCurrentValueJson() {
        try {
            return Shared.OBJECT_MAPPER.writeValueAsString(currentValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize currentValue", e);
        }
    }


}
