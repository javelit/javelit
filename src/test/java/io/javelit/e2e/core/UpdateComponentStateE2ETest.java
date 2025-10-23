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

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for programmatically updating component state via Jt.setComponentState().
 */
public class UpdateComponentStateE2ETest {

    /**
     * Error when updating after render
     * This test verifies that attempting to update a component's state after it has been rendered
     * throws an IllegalArgumentException.
     */
    @Test
    void testErrorWhenUpdatingAfterRender(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    String name = Jt.textInput("Name").key("name").use();

                    // These buttons will error because their nested code changes
                    // a widget's state after that widget within the script.
                    if (Jt.button("Clear name").use()) {
                        Jt.setComponentState("name", "");
                    }

                    Jt.text("Hello " + (name != null && !name.isEmpty() ? name : "unknown")).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for page to load
            assertThat(page.locator("jt-text-input")).isVisible(WAIT_1_SEC_MAX);

            // Enter a name
            final Locator nameInput = page.locator("jt-text-input input");
            nameInput.fill("John");
            nameInput.press("Enter");

            // Verify initial greeting
            assertThat(page.getByText("Hello John")).isVisible(WAIT_1_SEC_MAX);

            // Click "Clear name" button - this should trigger an error
            page.getByText("Clear name").click(WAIT_1_SEC_MAX_CLICK);

            // Verify an exception is reported with "IllegalArgumentException"
            assertThat(page.locator("jt-callout")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("IllegalArgumentException", new Page.GetByTextOptions().setExact(false)).first()).isVisible(WAIT_1_SEC_MAX);
        });
    }

    /**
     * Using session state flag for button
     * This test verifies that updating component state before rendering works correctly
     * by using a session state flag to control when the update happens.
     */
    @Test
    void testUsingSessionStateFlag(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    if (Jt.componentsState().getBoolean("clear", false)) {
                        Jt.setComponentState("name", "");
                    }

                    String name = Jt.textInput("Name").key("name").use();
                    Jt.button("Clear name").key("clear").use();

                    Jt.text("Hello " + (name != null && !name.isEmpty() ? name : "unknown")).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for page to load
            assertThat(page.locator("jt-text-input")).isVisible(WAIT_1_SEC_MAX);

            // Enter a name
            final Locator nameInput = page.locator("jt-text-input input");
            nameInput.fill("Alice");
            nameInput.press("Enter");

            // Verify initial greeting
            assertThat(page.getByText("Hello Alice")).isVisible(WAIT_1_SEC_MAX);

            // Click "Clear name" button
            page.getByText("Clear name").click(WAIT_1_SEC_MAX_CLICK);

            // Verify name was cleared
            assertThat(page.getByText("Hello unknown")).isVisible(WAIT_1_SEC_MAX);
            assertThat(nameInput).hasValue("");
        });
    }

    /**
     * Using callbacks
     * This test verifies that updating component state works correctly when done via
     * button onClick callbacks.
     */
    @Test
    void testUsingCallbacks(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    String name = Jt.textInput("Name").key("name").use();

                    Jt.button("Clear name")
                        .onClick(button -> Jt.setComponentState("name", ""))
                        .use();

                    Jt.text("Hello " + (name != null && !name.isEmpty() ? name : "unknown")).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for page to load
            assertThat(page.locator("jt-text-input")).isVisible(WAIT_1_SEC_MAX);

            // Enter a name
            final Locator nameInput = page.locator("jt-text-input input");
            nameInput.fill("Bob");
            nameInput.press("Enter");

            // Verify initial greeting
            assertThat(page.getByText("Hello Bob")).isVisible(WAIT_1_SEC_MAX);

            // Click "Clear name" button
            page.getByText("Clear name").click(WAIT_1_SEC_MAX_CLICK);

            // Verify name was cleared
            assertThat(page.getByText("Hello unknown")).isVisible(WAIT_1_SEC_MAX);
            assertThat(nameInput).hasValue("");
        });
    }

    /**
     * Using containers
     * This test verifies that updating component state before rendering works correctly
     * even when components are rendered out of logical order using containers.
     */
    @Test
    void testUsingContainers(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import io.javelit.core.JtContainer;

            public class TestApp {
                public static void main(String[] args) {
                    JtContainer begin = Jt.container().use();

                    if (Jt.button("Clear name").use()) {
                        Jt.setComponentState("name", "");
                    }

                    // The widget is second in logic, but first in display
                    String name = Jt.textInput("Name").key("name").use(begin);

                    Jt.text("Hello " + (name != null && !name.isEmpty() ? name : "unknown")).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for page to load
            assertThat(page.locator("jt-text-input")).isVisible(WAIT_1_SEC_MAX);

            // Enter a name
            final Locator nameInput = page.locator("jt-text-input input");
            nameInput.fill("Charlie");
            nameInput.press("Enter");

            // Verify initial greeting
            assertThat(page.getByText("Hello Charlie")).isVisible(WAIT_1_SEC_MAX);

            // Click "Clear name" button
            page.getByText("Clear name").click(WAIT_1_SEC_MAX_CLICK);

            // Verify name was cleared
            assertThat(page.getByText("Hello unknown")).isVisible(WAIT_1_SEC_MAX);
            assertThat(nameInput).hasValue("");
        });
    }
}
