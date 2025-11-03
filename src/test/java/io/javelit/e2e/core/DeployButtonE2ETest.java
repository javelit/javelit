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
import com.microsoft.playwright.options.AriaRole;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for the Deploy button functionality.
 */
public class DeployButtonE2ETest {

    @Test
    void testDeployButtonPresenceAndModal(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Hello World").use();
                }
            }
            """;

        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify the app is running
            assertThat(page.getByText("Hello World")).isVisible(WAIT_1_SEC_MAX);

            // Verify deploy button is present
            assertThat(page.locator("#deploy-button")).isVisible(WAIT_1_SEC_MAX);

            // Click the deploy button
            page.locator("#deploy-button").click(WAIT_1_SEC_MAX_CLICK);

            // Verify deploy modal opens with expected content
            assertThat(page.locator("#deploy-modal")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Deploy this app using")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByRole(AriaRole.HEADING).filter(new Locator.FilterOptions().setHasText("Railway"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByRole(AriaRole.HEADING).filter(new Locator.FilterOptions().setHasText("Custom Deployment"))).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
