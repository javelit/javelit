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

import com.microsoft.playwright.Locator;
import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for programmatically updating component state via Jt.updateComponentState().
 */
public class UpdateComponentStateE2ETest {

    @Test
    void testUpdateSliderValue(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    double volume = Jt.slider("Volume").key("volume").min(0).max(100).value(50).use();

                    Jt.text("Current volume: " + volume).use();

                    if (Jt.button("Set to Max").use()) {
                        Jt.updateComponentState("volume", 100.0);
                        Jt.rerun();
                    }

                    if (Jt.button("Set to Min").use()) {
                        Jt.updateComponentState("volume", 0.0);
                        Jt.rerun();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for page to load
            assertThat(page.locator("jt-slider")).isVisible(WAIT_1_SEC_MAX);

            // Verify initial state
            assertThat(page.getByText("Current volume: 50.0")).isVisible(WAIT_1_SEC_MAX);
            final Locator slider = page.locator("jt-slider .slider-input");
            assertThat(slider).hasValue("50");

            // Click "Set to Max" button
            page.locator("jt-button").filter(new Locator.FilterOptions().setHasText("Set to Max")).click(WAIT_1_SEC_MAX_CLICK);

            // Verify slider was updated to 100
            assertThat(page.getByText("Current volume: 100.0")).isVisible(WAIT_1_SEC_MAX);
            assertThat(slider).hasValue("100");

            // Click "Set to Min" button
            page.locator("jt-button").filter(new Locator.FilterOptions().setHasText("Set to Min")).click(WAIT_1_SEC_MAX_CLICK);

            // Verify slider was updated to 0
            assertThat(page.getByText("Current volume: 0.0")).isVisible(WAIT_1_SEC_MAX);
            assertThat(slider).hasValue("0");
        });
    }

    @Test
    void testUpdateMultipleTextInputs(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    String name = Jt.textInput("Name").key("name").use();
                    String email = Jt.textInput("Email").key("email").use();

                    Jt.text("Name: " + name).use();
                    Jt.text("Email: " + email).use();

                    if (Jt.button("Clear Form").use()) {
                        Jt.updateComponentState("name", "");
                        Jt.updateComponentState("email", "");
                        Jt.rerun();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for page to load
            assertThat(page.locator("jt-text-input")).isVisible(WAIT_1_SEC_MAX);

            // Fill in the form
            final Locator nameInput = page.locator("jt-text-input").filter(new Locator.FilterOptions().setHasText("Name")).locator("input");
            final Locator emailInput = page.locator("jt-text-input").filter(new Locator.FilterOptions().setHasText("Email")).locator("input");

            nameInput.fill("John Doe");
            emailInput.fill("john@example.com");

            // Verify the values are displayed
            assertThat(page.getByText("Name: John Doe")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Email: john@example.com")).isVisible(WAIT_1_SEC_MAX);

            // Click "Clear Form" button
            page.locator("jt-button").filter(new Locator.FilterOptions().setHasText("Clear Form")).click(WAIT_1_SEC_MAX_CLICK);

            // Verify both inputs are cleared
            assertThat(page.getByText("Name:")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Email:")).isVisible(WAIT_1_SEC_MAX);
            assertThat(nameInput).hasValue("");
            assertThat(emailInput).hasValue("");
        });
    }

    @Test
    void testUpdateComponentStateWithNonexistentKey(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Testing update with nonexistent key").use();

                    if (Jt.button("Update Nonexistent").use()) {
                        try {
                            Jt.updateComponentState("nonexistent_key", "value");
                            Jt.text("ERROR: Should have thrown exception").use();
                        } catch (IllegalStateException e) {
                            Jt.text("Caught expected exception: " + e.getMessage()).use();
                        }
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for page to load
            assertThat(page.getByText("Testing update with nonexistent key")).isVisible(WAIT_1_SEC_MAX);

            // Click button to trigger update with nonexistent key
            page.locator("jt-button").click(WAIT_1_SEC_MAX_CLICK);

            // Verify exception was caught and displayed
            assertThat(page.getByText("Caught expected exception")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("No component with key 'nonexistent_key'")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
