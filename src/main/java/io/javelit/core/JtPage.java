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

import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static io.javelit.core.Shared.RESERVED_URL_PATHS;
import static io.javelit.core.utils.EmojiUtils.ensureIsValidIcon;

public final class JtPage {
  @Nonnull private final JtRunnable pageApp;
  @Nonnull private final String title;
  @Nonnull private final String icon;
  @Nonnull private final String urlPath;
  private final boolean isHome;
  private final boolean noPersistWhenLeft;
  private final List<String> section;

  private JtPage(final @Nonnull Builder builder) {
    this.pageApp = builder.pageApp;
    this.title = builder.title;
    this.icon = builder.icon;
    this.urlPath = builder.urlPath;
    this.isHome = builder.isHome;
    this.noPersistWhenLeft = builder.noPersistWhenLeft;
    this.section = builder.section;
  }

  public static Builder builder(final @Nonnull String path, final @Nonnull JtRunnable page) {
    return new Builder(path, page);
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

  public boolean isNoPersistWhenLeft() {
    return noPersistWhenLeft;
  }

  public List<String> section() {
    return section;
  }

  public void run() {
    try {
      StateManager.setPageContext(this);
      pageApp.run();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      StateManager.clearPageContext();
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
                                     that.urlPath) && this.isHome == that.isHome && this.noPersistWhenLeft == that.noPersistWhenLeft && Objects.equals(
        this.section,
        that.section);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pageApp, title, icon, urlPath, isHome, noPersistWhenLeft, section);
  }

  @Override
  public String toString() {
    return "JtPage[" + "pageApp=" + pageApp + ", " + "title=" + title + ", " + "icon=" + icon + ", " + "urlPath=" + urlPath + ", " + "isHome=" + isHome + ", " + "noPersistWhenLeft=" + noPersistWhenLeft + ", " + "section=" + section + ']';
  }


  public static final class Builder {
    private final @Nonnull JtRunnable pageApp;
    private final String urlPath;
    private String title;
    private String icon;
    private boolean isHome;
    private boolean noPersistWhenLeft;
    private List<String> section;

    private Builder(final @Nonnull String path, final @Nonnull JtRunnable pageApp) {
      this.urlPath = cleanPath(path);
      this.pageApp = pageApp;
      this.title = pathToTitle(this.urlPath);
    }

    private String cleanPath(final @Nonnull String urlPath) {
      String cleanedUrl = urlPath.trim();
      if (!cleanedUrl.startsWith("/")) {
        cleanedUrl = "/" + urlPath;
      }
      if (cleanedUrl.endsWith("/")) {
        cleanedUrl = cleanedUrl.substring(0, cleanedUrl.length() - 1);
      }

      checkArgument(!RESERVED_URL_PATHS.contains(cleanedUrl),
                    "The path %s is a reserved path. Please use a different path.",
                    cleanedUrl);

      return cleanedUrl;
    }

    /**
     * The display title of the page. If not passed, the title is inferred from the path.
     */
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

    /**
     * Make the page the default homepage
     */
    public Builder home() {
      this.isHome = true;
      return this;
    }

    /**
     * Mark this page to clear all its state when the user navigates to another page.
     * While on the page, components persist normally, based on user-provided keys.
     * When leaving the page, all page-scoped state is cleared.
     */
    public Builder noPersistWhenLeft() {
      this.noPersistWhenLeft = true;
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

    JtPage build() {
      return new JtPage(this);
    }

    @VisibleForTesting
    static String pathToTitle(final @Nonnull String path) {
      final String cleaned = path.startsWith("/") ? path.substring(1) : path;
      if (cleaned.isEmpty()) {
        return "Home";
      }
      // Split by - and _, filter empty, capitalize each word
      String[] words = cleaned.split("[-_/]+");
      StringBuilder result = new StringBuilder();

      for (final String word : words) {
        if (!word.isEmpty()) {
          if (!result.isEmpty()) {
            result.append(" ");
          }
          result.append(Character.toUpperCase(word.charAt(0)));
          if (word.length() > 1) {
            result.append(word.substring(1));
          }
        }
      }

      return result.toString();
    }


    // used internally by the navigation component to modify some pages if necessary
    boolean isHome() {
      return isHome;
    }

    String urlPath() {
      return urlPath;
    }
  }
}
