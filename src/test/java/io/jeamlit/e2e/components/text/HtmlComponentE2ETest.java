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
package io.jeamlit.e2e.components.text;

import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_HIDDEN;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT;


public class HtmlComponentE2ETest {

    @Test
    void testHtml_SimpleHtmlString(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.html("<h2>Hello HTML</h2><p>This is a <strong>test</strong>!</p>").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for HTML component to be visible
            assertThat(page.locator("jt-html")).isVisible(WAIT_1_SEC_MAX);
            
            // Check that HTML content is rendered (should contain h2 and p elements)
            assertThat(page.locator("jt-html h2")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-html h2")).hasText("Hello HTML", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-html p")).hasText("This is a test!", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-html strong")).hasText("test", WAIT_1_SEC_MAX_TEXT);
        });
    }

    @Test
    void testHtml_WithWidth(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.html("<div>Content with width</div>").width(400).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for HTML component to be visible
            assertThat(page.locator("jt-html")).isVisible(WAIT_1_SEC_MAX);
            
            // Check width attribute is set
            assertThat(page.locator("jt-html")).hasAttribute("width", "400", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @Test
    void testHtml_ScriptSanitization(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.html("<p>Safe content</p><script>alert('dangerous');</script>").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for HTML component to be visible
            assertThat(page.locator("jt-html")).isVisible(WAIT_1_SEC_MAX);
            
            // Check that safe content is rendered
            assertThat(page.locator("jt-html p")).hasText("Safe content", WAIT_1_SEC_MAX_TEXT);
            
            // Check that script tag is NOT present (sanitized)
            assertThat(page.locator("jt-html script")).isHidden(WAIT_1_SEC_MAX_HIDDEN);
        });
    }
}
