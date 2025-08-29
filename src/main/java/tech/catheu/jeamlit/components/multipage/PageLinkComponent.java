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

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import tech.catheu.jeamlit.core.Jt;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import static tech.catheu.jeamlit.core.utils.Preconditions.checkArgument;

public final class PageLinkComponent extends JtComponent<JtComponent.NONE> {

    protected final @Nonnull String label;
    protected final @Nonnull String url;
    protected final boolean isExternal;
    protected final String icon;
    protected final String help;
    protected final boolean disabled;
    protected final String width;
    protected final boolean isActive;
    protected final boolean isHomePage;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile(
                "components/multipage/PageLinkComponent.register.html.mustache");
        renderTemplate = mf.compile("components/multipage/PageLinkComponent.render.html.mustache");
    }

    private PageLinkComponent(final Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE, null);

        this.label = markdownToHtml(builder.label, true);
        this.url = builder.url;
        this.isExternal = builder.isExternal;
        this.icon = builder.icon;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.width = builder.width;
        this.isHomePage = builder.isHomePage;

        // Determine if this link is active (current page matches target)
        if (disabled || isExternal) {
            this.isActive = false;
        } else {
            final String currentPath = Jt.urlPath();
            this.isActive = (isHomePage && "/".equals(currentPath)) || url.equals(currentPath);
        }
    }

    public static class Builder extends JtComponentBuilder<NONE, PageLinkComponent, Builder> {
        @Language("markdown")
        private final @Nonnull String label;
        private final @Nonnull String url;
        private final @Nullable Class<?> pageClass;
        private final boolean isHomePage;
        private final boolean isExternal;
        private String icon;
        private String help;
        private boolean disabled;
        private String width = "content"; // content, stretch, or pixel value

        // Constructor for internal page links
        public Builder(final @Nonnull Class<?> pageClass) {
            this.pageClass = pageClass;
            this.isExternal = false;

            final NavigationComponent nav = getNavigationComponent();
            checkArgument(nav != null,
                          "No navigation component found in the app. Cannot create a link to an app page when Jt.navigation is not used. Use a direct String link or introduce Jt.navigation in your app.");
            final JtPage page = nav.getPageFor(pageClass);
            checkArgument(page != null,
                          "Unknown page for pageClass %s. Please provide a class that is used as a page in Jt.navigation(...).",
                          pageClass);
            this.label = page.title();
            this.url = page.urlPath();
            this.isHomePage = page.isHome();

        }

        // Constructor for external links
        public Builder(final @Nonnull String url, final @Language("markdown") @Nonnull String label) {
            checkArgument(!url.isBlank(), "URL cannot be null or empty");
            checkArgument(!label.isBlank(), "Label cannot be null or empty");
            this.url = url;
            this.label = label;
            this.pageClass = null;
            this.isExternal = true;
            this.isHomePage = false;
        }

        public Builder icon(final String icon) {
            ensureIsValidIcon(icon);
            this.icon = icon;
            return this;
        }

        public Builder help(final String help) {
            this.help = help;
            return this;
        }

        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder width(final @Nonnull String width) {
            checkArgument("content".equals(width) || "stretch".equals(width) || width.matches("\\d+"),
                          "Width must be 'content', 'stretch', or a pixel value (integer)");
            this.width = width;
            return this;
        }

        /**
         * Set width as integer pixels.
         *
         * @param widthPixels Width in pixels (must be non-negative)
         * @return this builder
         */
        public Builder width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        @Override
        public PageLinkComponent build() {
            return new PageLinkComponent(this);
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
    protected TypeReference<NONE> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
