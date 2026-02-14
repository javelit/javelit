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
package io.javelit.core;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.javelit.core.helpers.OAuth2Configuration;
import io.javelit.core.helpers.OAuth2Workflow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public final class NavigationComponent extends JtComponent<JtPage> {

  private static final Mustache registerTemplate;
  private static final Mustache renderTemplate;

  final List<JtPage> pages;
  final JtPage home;
  NavigationPosition position;

  final OAuth2Configuration oAuth2Configuration;

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
    super(builder, null, // set later in this constructor
          null, builder.position == NavigationPosition.HIDDEN ? JtContainer.MAIN : JtContainer.SIDEBAR);

    this.oAuth2Configuration = builder.oAuth2Configuration;
    List<JtPage.Builder> pageBuilders = builder.pageBuilders;
    if(this.oAuth2Configuration != null) {
      if(!Jt.isLoggedIn()){
        pageBuilders = new OAuth2Workflow(oAuth2Configuration).getLoginPages();
        builder.hidden();
      } else {
        Jt.text( "Welcome " + OAuth2Workflow.getCurrentUser().name()).use(builder.position == NavigationPosition.HIDDEN ? JtContainer.MAIN : JtContainer.SIDEBAR);
        pageBuilders.add(JtPage.builder("/logout", () -> {
          Jt.sessionState().clear();
          Jt.rerun(true);
        } ));
      }
    
    }

    final List<JtPage.Builder> homePages = pageBuilders.stream().filter(JtPage.Builder::isHome).toList();

    if (homePages.isEmpty()) {
      JtPage.Builder firstPageBuilder = pageBuilders.getFirst();
      firstPageBuilder.home();
    } else if (homePages.size() > 1) {
      throw new IllegalArgumentException(
          "Multiple pages are defined as home: %s. Only one page should be defined as home.".formatted(String.join(
              ", ",
              homePages.stream().filter(JtPage.Builder::isHome).map(JtPage.Builder::urlPath).toList())));

    }
    this.pages = pageBuilders.stream().map(JtPage.Builder::build).collect(Collectors.toList());
    this.home = this.pages
        .stream()
        .filter(JtPage::isHome)
        .findFirst()
        .orElseThrow(() -> new RuntimeException(
            "Home page not found. Implementation error. Please reach out to support."));
    this.position = builder.position;

    // Set initial page based on current URL, not always home
    final String currentPath = getCurrentPath();
    JtPage page = getPageFor(currentPath);
    if (page == null) {
      page = build404(currentPath);
    }

    this.currentValue = page;
    
  }

  private static @NotNull JtPage build404(String currentPath) {
    return JtPage.builder(currentPath, () -> {
      Jt.title("Page Not Found.").use();
      if (Jt.button("Go to home").use()) {
        Jt.switchPage(null);
      }
    }).title("Page not found").build();
  }

  public JtPage getHome() {
    return home;
  }

  /**
   * Determines the initial page based on current URL path.
   * Falls back to home page if no URL match is found.
   */
  public @Nullable JtPage getPageFor(final @Nullable String urlPath) {
    if (urlPath == null || urlPath.isBlank() || "/".equals(urlPath)) {
      return home;
    }
    for (final JtPage page : pages) {
      if (page.urlPath().equals(urlPath)) {
        return page;
      }
    }
    // unknown
    return null;
  }


  public static class Builder extends JtComponentBuilder<JtPage, NavigationComponent, Builder> {

    private final List<JtPage.Builder> pageBuilders = new ArrayList<>();
    private NavigationPosition position;
    private OAuth2Configuration oAuth2Configuration;

    public Builder(JtPage.Builder... pages) {
      this.userKey = JtComponent.UNIQUE_NAVIGATION_COMPONENT_KEY;
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
      position = NavigationComponent.NavigationPosition.HIDDEN;
      return this;
    }

    public Builder withOauth2(final @NotNull OAuth2Configuration.Builder oAuth2ConfigurationBuilder) {
      this.oAuth2Configuration = oAuth2ConfigurationBuilder.build();
      return this;
    }

    @Override
    protected String generateInternalKey() {
      return JtComponent.UNIQUE_NAVIGATION_COMPONENT_KEY;
    }

    @Override
    public Builder key(final @NotNull String key) {
      throw new UnsupportedOperationException(
          "The key of the navigation component cannot be modified. It is JtComponent.UNIQUE_NAVIGATION_COMPONENT_KEY");
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


  private record FrontendJtPage(@Nonnull String title, @Nonnull String icon,
                                @Nonnull String urlPath, boolean isHome,
                                // section path: List.of("Admin", "Users") would put the page in section Admin, subsection Users, etc...
                                List<String> section) {
    private static FrontendJtPage from(final @Nonnull JtPage page) {
      return new FrontendJtPage(page.title(),
                                page.icon(),
                                page.urlPath(),
                                page.isHome(),
                                page.section());
    }
  }
}
