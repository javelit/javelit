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
package io.jeamlit.e2e.components.multipage;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.EXACT_MATCH;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for PageLinkComponent.
 */
public class PageLinkComponentE2ETest {

    @Test
    void testBasicFunctionality(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.navigation(
                        Jt.page(Home.class).title("Home").home(),
                        Jt.page(About.class).title("About").urlPath("/about")
                    ).use();
            
                    Jt.text("Navigation Links:").use();
            
                    // Internal page links with various configurations
                    Jt.pageLink(Home.class).icon("🏠").use();
                    Jt.pageLink(About.class).icon("ℹ️").width("stretch").use();
            
                    // External link
                    Jt.pageLink("https://example.com", "External Link").icon("🌐").use();
            
                    // Disabled link
                    Jt.pageLink(About.class).disabled(true).use();
                }
            
                public static class Home {
                    public static void main(String[] args) {
                        Jt.text("Home page content").use();
                        Jt.pageLink(About.class).use();
                    }
                }
            
                public static class About {
                    public static void main(String[] args) {
                        Jt.text("About page content").use();
                        Jt.pageLink(Home.class).use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify page links are visible
            assertThat(page.locator("jt-page-link").first()).isVisible(WAIT_1_SEC_MAX);

            // Navigate to About page
            page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText("About")).first().click();
            assertThat(page.getByText("About page content", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page).hasURL(Pattern.compile(".*/about"));

            // Test internal navigation - click Home link
            page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText("🏠 Home")).click();
            assertThat(page.getByText("Home page content", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page).hasURL(Pattern.compile(".*/Home"));
            
            // Test icons are present
            assertThat(page.locator("jt-page-link .emoji-icon").first()).isVisible(WAIT_1_SEC_MAX);
            
            // Test external link has correct attributes
            assertThat(page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText("External Link")))
                .hasAttribute("href", "https://example.com");
            assertThat(page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText("External Link")))
                .hasAttribute("target", "_blank");
            
            // Test disabled link
            assertThat(page.locator("jt-page-link[disabled]")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testActiveStateDetection(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.navigation(
                        Jt.page(Home.class).title("Home").home(),
                        Jt.page(About.class).title("About").urlPath("/about")
                    ).use();
            
                    Jt.pageLink(Home.class).icon("🏠").use();
                    Jt.pageLink(About.class).icon("ℹ️").use();
                }
            
                public static class Home {
                    public static void main(String[] args) {
                        Jt.text("Home page").use();
                    }
                }
            
                public static class About {
                    public static void main(String[] args) {
                        Jt.text("About page").use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Start on home page
            assertThat(page).hasURL(Pattern.compile(".*/Home"));
            assertThat(page.getByText("Home page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            
            // Home link should have active state
            assertThat(page.locator("jt-page-link").first()).hasAttribute("is-active", "");
            
            // Navigate to About page
            page.getByText("ℹ️ About", EXACT_MATCH).click();
            // Should be on about page with correct active state
            assertThat(page).hasURL(Pattern.compile(".*/about"));
            assertThat(page.getByText("About page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            
            // Now About link should be active, Home link should not be
            assertThat(page.locator("jt-page-link").last()).hasAttribute("is-active", "");
            assertThat(page.locator("jt-page-link").first()).not().hasAttribute("is-active", "");
            
            // Navigate back to Home
            page.getByText("🏠 Home", EXACT_MATCH).click();
            
            // Should be back on root URL with Home active again
            assertThat(page).hasURL(Pattern.compile(".*/Home"));
            assertThat(page.getByText("Home page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-page-link").first()).hasAttribute("is-active", "");
            assertThat(page.locator("jt-page-link").last()).not().hasAttribute("is-active", "");
        });
    }
}
