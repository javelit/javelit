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
package io.javelit.e2e.core;

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.EXACT_MATCH;
import static io.javelit.e2e.helpers.PlaywrightUtils.TEST_PROXY_PREFIX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchPageE2ETest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testProgrammaticPageSwitching(final boolean proxied, final TestInfo testInfo) {
        JtRunnable app = () -> {
            var currentPage = Jt.navigation(
                Jt.page("/home", SwitchPageE2ETest::home).title("Home Page").home(),
                Jt.page("/products", SwitchPageE2ETest::products)
            ).use();

            currentPage.run();
            Jt.text("Footer - always visible").use();
        };

        PlaywrightUtils.runInBrowser(testInfo, app, true, proxied, page -> {
            final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

            // Wait for app to load and verify we're on HomePage
            assertThat(page.getByText("The Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Welcome to the home page!", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertTrue(page.url().contains(pathPrefix + "/home"));

            // Test programmatic switch to Products page via button click
            page.getByText("Go to Products", EXACT_MATCH).click(WAIT_1_SEC_MAX_CLICK);

            // Verify we're now on Products page
            assertThat(page.getByText("Products Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Browse our amazing products!", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertTrue(page.url().contains(pathPrefix + "/products"));

            // Test programmatic switch back to Home page from Contact page
            page.getByText("Back to Home", EXACT_MATCH).click(WAIT_1_SEC_MAX_CLICK);

            // Verify we're back on Home page
            assertThat(page.getByText("Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Welcome to the home page!", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
            assertTrue(page.url().contains(pathPrefix + "/home"));

            // Verify footer is persistent across all page switches
            assertThat(page.getByText("Footer - always visible", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);

            // Click the button that tries to switch to an unregistered page
            page.getByText("Switch to Unregistered Page", EXACT_MATCH).click(WAIT_1_SEC_MAX_CLICK);
            // Verify error message appears
            assertThat(page.locator("jt-callout")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("IllegalArgumentException").first()).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("This page is not registered").first()).isVisible(WAIT_1_SEC_MAX);
            // page did not change
            assertTrue(page.url().contains(pathPrefix + "/home"));
        });
    }


    private static void home() {
        Jt.title("The Home Page").use();
        Jt.text("Welcome to the home page!").use();

        // Test programmatic switch to Products page
        if (Jt.button("Go to Products").use()) {
            Jt.switchPage("/products");
        }

        // This should throw since ContactPage is not registered
        if (Jt.button("Switch to Unregistered Page").use()) {
            Jt.switchPage("/unregistered");
        }
    }

    private static void products() {
        Jt.title("Products Page").use();
        Jt.text("Browse our amazing products!").use();

        // Test programmatic switch back to Home page
        if (Jt.button("Back to Home").use()) {
            Jt.switchPage("/home");
        }
    }
}
