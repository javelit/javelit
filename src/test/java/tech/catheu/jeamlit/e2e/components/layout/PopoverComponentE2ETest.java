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
package tech.catheu.jeamlit.e2e.components.layout;

import com.microsoft.playwright.Locator;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_50_MS_MAX;

/**
 * End-to-end tests for PopoverComponent.
 */
public class PopoverComponentE2ETest {
    
    @Test
    void testPopoverToggle() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            import tech.catheu.jeamlit.core.JtContainer;
            
            public class TestApp {
                public static void main(String[] args) {
                    JtContainer popoverContainer = Jt.popover("test-popover", "Click me").use();
                    Jt.text("Content inside popover").use(popoverContainer);
                    Jt.button("Popover Button").use(popoverContainer);
                    
                    Jt.text("Content outside popover").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(app, page -> {
            // Wait for popover to be visible
            assertThat(page.locator("jt-popover")).isVisible(WAIT_1_SEC_MAX);
            // Check popover trigger is visible
            final Locator popoverButton = page.getByText("Click me");
            assertThat(popoverButton).isVisible(WAIT_1_SEC_MAX);
            // Check content outside popover is visible
            final Locator outsideText = page.getByText("Content outside popover");
            assertThat(outsideText).isVisible(WAIT_1_SEC_MAX);
            // Click to open popover
            popoverButton.click();
            // Check that popover content is now visible
            assertThat(page.getByText("Content inside popover")).isVisible(WAIT_1_SEC_MAX);
            // click outside
            outsideText.click();
            // Check that popover content is not visible anymore
            assertThat(page.getByText("Content inside popover")).not().isVisible(WAIT_50_MS_MAX);

        });
    }
}
