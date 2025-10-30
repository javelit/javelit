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

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for unused components warning feature.
 */
public class UnusedComponentsWarningE2ETest {

    @Test
    void testUnusedComponentWarning(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Hello World");
                }
            }
            """;

        // Verify warning text appears
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify warning text appears
            assertThat(page.getByText("The following components were created but never used")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
