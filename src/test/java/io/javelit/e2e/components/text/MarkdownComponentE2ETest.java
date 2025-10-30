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
package io.javelit.e2e.components.text;

import com.microsoft.playwright.Locator;
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT;

/**
 * End-to-end tests for MarkdownComponent.
 */
public class MarkdownComponentE2ETest {

    @Test
    void testMarkdownVariations(TestInfo testInfo) {
        JtRunnable app = () -> {
            // Test 1: Simple markdown
            Jt.markdown("# Hello Markdown\n\nThis is **bold** and *italic* text.\n\n- List item 1\n- List item 2").use();

            // Test 2: With width
            Jt.markdown("## Markdown with width").width(500).use();

            // Test 3: With help
            Jt.markdown("Some content").help("This is help text").use();

            // Test 4: Complex markdown
            Jt.markdown("""
                # Main Title

                This is a paragraph with [a link](https://example.com).

                ## Code Example

                Here's some `inline code` and a code block:

                ```java
                System.out.println("Hello World");
                ```

                > This is a blockquote
                """).use();
        };

        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify all 4 markdown components are rendered
            assertThat(page.locator("jt-markdown").nth(0)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(1)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(2)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(3)).isVisible(WAIT_1_SEC_MAX);

            // Test 1: Simple markdown
            assertThat(page.locator("jt-markdown").nth(0).locator("h1")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(0).locator("h1")).hasText("Hello Markdown", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown").nth(0).locator("strong")).hasText("bold", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown").nth(0).locator("em")).hasText("italic", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown").nth(0).locator("ul")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(0).locator("li").first()).hasText("List item 1", WAIT_1_SEC_MAX_TEXT);

            // Test 2: With width
            assertThat(page.locator("jt-markdown").nth(1)).hasAttribute("width", "500", WAIT_1_SEC_MAX_ATTRIBUTE);
            assertThat(page.locator("jt-markdown").nth(1).locator("h2")).hasText("Markdown with width", WAIT_1_SEC_MAX_TEXT);

            // Test 3: With help
            assertThat(page.locator("jt-markdown").nth(2).locator("jt-tooltip")).isVisible(WAIT_1_SEC_MAX);

            // Test 4: Complex markdown
            assertThat(page.locator("jt-markdown").nth(3).locator("h1")).hasText("Main Title", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown").nth(3).locator("h2")).hasText("Code Example", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("jt-markdown").nth(3).locator("a")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(3).locator("code", new Locator.LocatorOptions().setHasText("inline code"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(3).locator("code", new Locator.LocatorOptions().setHasText("System.out.println(\"Hello World\");"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(3).locator("pre")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-markdown").nth(3).locator("blockquote")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
