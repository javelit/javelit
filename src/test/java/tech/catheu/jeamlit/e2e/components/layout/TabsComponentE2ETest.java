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
import com.microsoft.playwright.Page;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_10_MS_MAX;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for TabsComponent.
 */
public class TabsComponentE2ETest {
    
    @Test
    void testTabSwitching() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            import tech.catheu.jeamlit.components.layout.TabsComponent;
            import java.util.List;
            
            public class TestApp {
                public static void main(String[] args) {
                    TabsComponent.Tabs tabs = Jt.tabs("test-tabs", List.of("Tab 0", "Tab 1", "Tab 2")).use();
                    
                    // Content for Tab 1
                    Jt.text("Content of Tab 0").use(tabs.tab(0));
                    Jt.button("Button in Tab 0").use(tabs.tab(0));
                    
                    // Content for Tab 2
                    Jt.text("Content of Tab 1").use(tabs.tab(1));
                    Jt.button("Button in Tab 1").use(tabs.tab(1));
                    
                    // Content for Tab 3
                    Jt.text("Content of Tab 2").use(tabs.tab(2));
                    Jt.button("Button in Tab 2").use(tabs.tab(2));
                }
            }
            """;

        PlaywrightUtils.runInBrowser(app, page -> {
            
            // Wait for tabs to be visible
            assertThat(page.locator("jt-tabs")).isVisible(WAIT_1_SEC_MAX);
            // Check tab headers are visible
            Page.GetByTextOptions exactTextMatch = new Page.GetByTextOptions().setExact(true);
            assertThat(page.getByText("Tab 0", exactTextMatch)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Tab 1", exactTextMatch)).isVisible(WAIT_1_SEC_MAX);
            final Locator tab2Header = page.getByText("Tab 2", exactTextMatch);
            assertThat(tab2Header).isVisible(WAIT_1_SEC_MAX);
            // Initially, Tab 1 content should be visible
            assertThat(page.getByText("Content of Tab 0")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Content of Tab 1")).not().isVisible(WAIT_10_MS_MAX);
            assertThat(page.getByText("Content of Tab 2")).not().isVisible(WAIT_10_MS_MAX);
            // Click on Tab 2
            tab2Header.click();
            assertThat(page.getByText("Content of Tab 2")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Content of Tab 0")).not().isVisible(WAIT_10_MS_MAX);
            assertThat(page.getByText("Content of Tab 1")).not().isVisible(WAIT_10_MS_MAX);
            
        });
    }
}
