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
package io.javelit.e2e.components.layout;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for ColumnsComponent.
 */
public class ColumnsComponentE2ETest {
    
    @Test
    void testColumnsLayout(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import io.javelit.components.layout.ColumnsComponent;
            
            public class TestApp {
                public static void main(String[] args) {
                    ColumnsComponent.Columns cols = Jt.columns(2).use();
            
                    // Add content to first column
                    Jt.text("Column 0 Content").use(cols.col(0));
                    Jt.button("Button 0").use(cols.col(0));
            
                    // Add content to second column
                    Jt.text("Column 1 Content").use(cols.col(1));
                    Jt.button("Button 1").use(cols.col(1));
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for columns component to be visible
            assertThat(page.locator("jt-columns")).isVisible(WAIT_1_SEC_MAX);
            // there are 2 columns
            assertThat(page.locator("div[slot='col_0']")).isVisible(WAIT_1_SEC_MAX);
            // ensure content is in correct column for col 0
            assertThat(page.locator("div[slot='col_0']", new Page.LocatorOptions().setHasText("Column 0 Content"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("div[slot='col_0']", new Page.LocatorOptions().setHasText("Button 0"))).isVisible(WAIT_1_SEC_MAX);
            // ensure content is in correct column for col 1
            final Locator secondCol = page.locator("div[slot='col_1']");
            assertThat(secondCol).isVisible();
            assertThat(page.locator("div[slot='col_1']", new Page.LocatorOptions().setHasText("Column 1 Content"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("div[slot='col_1']", new Page.LocatorOptions().setHasText("Button 1"))).isVisible(WAIT_1_SEC_MAX);
            
        });
    }
}
