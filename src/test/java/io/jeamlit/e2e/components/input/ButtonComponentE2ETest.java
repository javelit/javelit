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
package io.jeamlit.e2e.components.input;

import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_100_MS_MAX_CLICK;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_50_MS_MAX;

/**
 * End-to-end tests for ButtonComponent.
 */
public class ButtonComponentE2ETest {
    
    @Test
    void testButtonClick(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    if (Jt.button("Click Me").use()) {
                        Jt.text("Button was clicked!").use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // button exists
            assertThat(page.locator("jt-button")).isVisible(WAIT_1_SEC_MAX);
            // button text is correct
            assertThat(page.getByText("Click Me")).isVisible(WAIT_1_SEC_MAX);
            // "Button was clicked" text is not visible
            assertThat(page.getByText("Button was clicked!")).not().isVisible(WAIT_50_MS_MAX);
            // Click the button
            page.locator("jt-button button").click(WAIT_100_MS_MAX_CLICK);
            // "Button was clicked" new text is now visible
            assertThat(page.getByText("Button was clicked!")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
