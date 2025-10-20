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
import com.microsoft.playwright.assertions.LocatorAssertions;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for toolbar functionality including loading states and menu interactions.
 */
public class ToolbarE2ETest {
    
    @Test
    void testToolbarLoadingAndMenuFunctionality(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) throws InterruptedException {
                    Thread.sleep(1000);
                    Jt.text("App loaded successfully").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Test 1: Loading spinner should be visible initially
            assertThat(page.locator("jt-status-widget")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator(".status-spinner")).isVisible(WAIT_1_SEC_MAX);
            
            // Test 2: Spinner should disappear after 1500ms max (add some buffer)
            assertThat(page.locator("jt-status-widget")).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(2000));
            
            // Test 3: Menu button should be visible
            assertThat(page.locator("jt-toolbar-menu")).isVisible(WAIT_1_SEC_MAX);
            Locator menuButton = page.locator(".menu-button");
            assertThat(menuButton).isVisible(WAIT_1_SEC_MAX);
            
            // Test 4: Clicking rerun should show spinner again
            menuButton.click(WAIT_1_SEC_MAX_CLICK);
            assertThat(page.locator(".menu-dropdown.show")).isVisible(WAIT_1_SEC_MAX);
            page.getByText("Rerun").click(WAIT_1_SEC_MAX_CLICK);
            assertThat(page.locator("jt-status-widget")).isVisible(WAIT_1_SEC_MAX);
            // Wait for rerun to complete
            assertThat(page.locator("jt-status-widget")).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(2000));
            
            // Test 5: Settings modal functionality
            page.locator(".menu-button").click(WAIT_1_SEC_MAX_CLICK);
            page.locator("#toolbar-menu").getByText("Settings", new Locator.GetByTextOptions().setExact(true)).click(WAIT_1_SEC_MAX_CLICK);
            assertThat(page.locator("#settings-modal")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#settings-modal").getByText("Settings")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
