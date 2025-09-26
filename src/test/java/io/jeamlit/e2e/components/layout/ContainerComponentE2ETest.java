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
package io.jeamlit.e2e.components.layout;

import com.microsoft.playwright.Page;
import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_50_MS_MAX;

/**
 * End-to-end tests for ContainerComponent.
 */
public class ContainerComponentE2ETest {
    
    @Test
    void testContainerContent(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import io.jeamlit.core.JtContainer;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Before container").use();
            
                    JtContainer container = Jt.container("test-container").use();
                    Jt.text("Inside container").use(container);
                    Jt.button("Container Button").use(container);
            
                    Jt.text("After container").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for container to be visible
            assertThat(page.locator("jt-container")).isVisible(WAIT_1_SEC_MAX);
            // Check content before container
            assertThat(page.locator("jt-text", new Page.LocatorOptions().setHasText("Before container"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-container", new Page.LocatorOptions().setHasText("Before container"))).not().isVisible(WAIT_50_MS_MAX);
            // Check content inside container
            assertThat(page.locator("jt-container", new Page.LocatorOptions().setHasText("Inside container"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-container", new Page.LocatorOptions().setHasText("Container Button"))).isVisible(WAIT_1_SEC_MAX);
            // Check content after container
            assertThat(page.locator("jt-text", new Page.LocatorOptions().setHasText("After container"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-container", new Page.LocatorOptions().setHasText("After container"))).not().isVisible(WAIT_50_MS_MAX);
            
        });
    }
}
