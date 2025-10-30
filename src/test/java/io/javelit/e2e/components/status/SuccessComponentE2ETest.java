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
package io.javelit.e2e.components.status;

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

public class SuccessComponentE2ETest {

    @Test
    void testSuccessDisplay(TestInfo testInfo) {
        JtRunnable app = () -> {
            Jt.success("Operation completed successfully").use();
        };

        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            assertThat(page.locator("jt-callout")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Operation completed successfully")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
