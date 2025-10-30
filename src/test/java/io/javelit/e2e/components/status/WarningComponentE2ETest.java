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

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

public class WarningComponentE2ETest {

    @Test
    void testWarningDisplay(TestInfo testInfo) {
        final @Language("java") String app = """
                import io.javelit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        Jt.warning("This is a warning message").use();
                    }
                }
                """;

        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            assertThat(page.locator("jt-callout")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("This is a warning message")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
