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
package io.jeamlit.e2e.core;

import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.EXACT_MATCH;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchPageE2ETest {

    @Test
    void testProgrammaticPageSwitching(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    var currentPage = Jt.navigation(
                        Jt.page(HomePage.class).home(),
                        Jt.page(ProductsPage.class).urlPath("/products")
                    ).use();
            
                    currentPage.run();
                    Jt.text("Footer - always visible").use();
                }
            
                public static class HomePage {
                    public static void main(String[] args) {
                        Jt.title("Home Page").use();
                        Jt.text("Welcome to the home page!").use();
            
                        // Test programmatic switch to Products page
                        if (Jt.button("Go to Products").use()) {
                            Jt.switchPage(ProductsPage.class);
                        }
            
                        // This should throw since ContactPage is not registered
                        if (Jt.button("Switch to Unregistered Page").use()) {
                            Jt.switchPage(ContactPage.class);
                        }
                    }
                }
            
                public static class ProductsPage {
                    public static void main(String[] args) {
                        Jt.title("Products Page").use();
                        Jt.text("Browse our amazing products!").use();
            
                        // Test programmatic switch back to Home page
                        if (Jt.button("Back to Home").use()) {
                            Jt.switchPage(HomePage.class);
                        }
                    }
                }
            
                public static class ContactPage {
                    public static void main(String[] args) {
                        Jt.text("This class is not registered in the multipage app.").use();
                    }
                }
            }
            """;
        
        PlaywrightUtils.runInDedicatedBrowser(testInfo, app, page -> {
            // Wait for app to load and verify we're on HomePage
            assertThat(page.getByText("Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Welcome to the home page!", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertTrue(page.url().endsWith("/HomePage"));
            
            // Test programmatic switch to Products page via button click
            page.getByText("Go to Products", EXACT_MATCH).click(WAIT_1_SEC_MAX_CLICK);
            
            // Verify we're now on Products page
            assertThat(page.getByText("Products Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Browse our amazing products!", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertTrue(page.url().endsWith("/products"));
            
            // Test programmatic switch back to Home page from Contact page
            page.getByText("Back to Home", EXACT_MATCH).click(WAIT_1_SEC_MAX_CLICK);
            
            // Verify we're back on Home page
            assertThat(page.getByText("Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Welcome to the home page!", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertTrue(page.url().endsWith("/HomePage"));
            
            // Verify footer is persistent across all page switches
            assertThat(page.getByText("Footer - always visible", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);

            // Click the button that tries to switch to an unregistered page
            page.getByText("Switch to Unregistered Page", EXACT_MATCH).click(WAIT_1_SEC_MAX_CLICK);
            // Verify error message appears
            assertThat(page.locator("jt-error")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("IllegalArgumentException")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("This page is not registered")).isVisible(WAIT_1_SEC_MAX);
            // page did not change
            assertTrue(page.url().endsWith("/HomePage"));
        });
    }
}
