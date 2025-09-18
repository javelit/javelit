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
package io.jeamlit.e2e.components.status;

import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

public class ErrorComponentE2ETest {

    @Test
    void testErrorDisplayOnException(TestInfo testInfo) {
        final @Language("java") String app = """
                import io.jeamlit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        throw new RuntimeException("Something went wrong");
                    }
                }
                """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page ->
            assertThat(page.locator("jt-error")).isVisible(WAIT_1_SEC_MAX));
    }

    @Test
    void testErrorDisplay(TestInfo testInfo) {
        final @Language("java") String app = """
                import io.jeamlit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        Jt.error("User generated error").use();
                    }
                }
                """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page ->
            assertThat(page.locator("jt-error")).isVisible(WAIT_1_SEC_MAX));
    }
}
