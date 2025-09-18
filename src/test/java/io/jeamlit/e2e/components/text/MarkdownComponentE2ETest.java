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

import com.microsoft.playwright.Page;
import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT;

/**
 * End-to-end tests for MarkdownComponent.
 */
public class MarkdownComponentE2ETest {

    @Test
    void testMarkdown_SimpleMarkdown(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.markdown("# Hello Markdown\\n\\nThis is **bold** and *italic* text.\\n\\n- List item 1\\n- List item 2").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for Markdown component to be visible
            assertThat(page.locator("jt-markdown")).isVisible(WAIT_1_SEC_MAX);
            
            // Check that markdown content is rendered properly
            assertThat(page.locator("jt-markdown h1")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown h1")).hasText("Hello Markdown", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown strong")).hasText("bold", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown em")).hasText("italic", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown ul")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown li").first()).hasText("List item 1", WAIT_1_SEC_MAX_TEXT);
        });
    }

    @Test
    void testMarkdown_WithWidth(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.markdown("## Markdown with width").width(500).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for Markdown component to be visible
            assertThat(page.locator("jt-markdown")).isVisible(WAIT_1_SEC_MAX);
            
            // Check width attribute is set
            assertThat(page.locator("jt-markdown")).hasAttribute("width", "500", WAIT_1_SEC_MAX_ATTRIBUTE);
            
            // Check content is rendered
            assertThat(page.locator("jt-markdown h2")).hasText("Markdown with width", WAIT_1_SEC_MAX_TEXT);
        });
    }

    @Test
    void testMarkdown_WithHelp(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.markdown("Some content").help("This is help text").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for Markdown component to be visible
            assertThat(page.locator("jt-markdown")).isVisible(WAIT_1_SEC_MAX);
            
            // Check that help tooltip is present
            assertThat(page.locator("jt-markdown jt-tooltip")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testMarkdown_ComplexMarkdown(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.markdown(\"\"\"
                        # Main Title

                        This is a paragraph with [a link](https://example.com).

                        ## Code Example

                        Here's some `inline code` and a code block:

                        ```java
                        System.out.println("Hello World");
                        ```

                        > This is a blockquote
                        \"\"\").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for Markdown component to be visible
            assertThat(page.locator("jt-markdown")).isVisible(WAIT_1_SEC_MAX);
            
            // Check various markdown elements
            assertThat(page.locator("jt-markdown h1")).hasText("Main Title", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown h2")).hasText("Code Example", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown a")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown code", new Page.LocatorOptions().setHasText("inline code"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown code", new Page.LocatorOptions().setHasText("System.out.println(\"Hello World\");"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown pre")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown blockquote")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
