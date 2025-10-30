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

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for CodeComponent.
 */
public class CodeComponentE2ETest {

    @Test
    void testCodeVariations(TestInfo testInfo) {
        JtRunnable app = () -> {
            // Test 1: Basic code display
            Jt.code("""
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """).use();

            // Test 2: Language null
            Jt.code("function example() { return 'no highlighting'; }")
                .language(null)
                .use();

            // Test 3: Language JSON
            Jt.code("{\"name\": \"test\", \"value\": 123}")
                .language("json")
                .use();

            // Test 4: Line numbers enabled
            Jt.code("line 1\nline 2\nline 3")
                .lineNumbers(true)
                .use();

            // Test 5: Wrap lines enabled
            Jt.code("This is a very long line that should wrap when wrap lines is enabled and demonstrates the line wrapping functionality")
                .wrapLines(true)
                .use();

            // Test 6: Custom dimensions
            Jt.code("console.log('Custom dimensions test');")
                .width(500)
                .height(150)
                .use();
        };

        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify all 6 code components are rendered
            assertThat(page.locator("#app jt-internal-code").nth(0)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-internal-code").nth(1)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-internal-code").nth(2)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-internal-code").nth(3)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-internal-code").nth(4)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-internal-code").nth(5)).isVisible(WAIT_1_SEC_MAX);

            // Test 1: Basic code display
            assertThat(page.getByText("public class HelloWorld")).isVisible(WAIT_1_SEC_MAX);

            // Test 2: Language null
            assertThat(page.locator("#app jt-internal-code").nth(1).locator("pre code.language-none")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("function example()")).isVisible(WAIT_1_SEC_MAX);

            // Test 3: Language JSON
            assertThat(page.getByText("123")).isVisible(WAIT_1_SEC_MAX);

            // Test 4: Line numbers enabled
            assertThat(page.locator("#app jt-internal-code").nth(3)).hasAttribute("line-numbers", "");
            assertThat(page.getByText("line 2")).isVisible(WAIT_1_SEC_MAX);

            // Test 5: Wrap lines enabled
            assertThat(page.locator("#app jt-internal-code").nth(4)).hasAttribute("wrap-lines", "");
            assertThat(page.getByText("This is a very long line")).isVisible(WAIT_1_SEC_MAX);

            // Test 6: Custom dimensions
            var theCodeComponent = page.locator("#app jt-internal-code").nth(5);
            assertThat(theCodeComponent).hasAttribute("width", "500");
            assertThat(theCodeComponent).hasAttribute("height", "150");

            theCodeComponent.waitFor();
            String computedWidth = theCodeComponent.evaluate("el => getComputedStyle(el).getPropertyValue('--code-width')").toString();
            String computedHeight = theCodeComponent.evaluate("el => getComputedStyle(el).getPropertyValue('--code-height')").toString();

            assertEquals("500px", computedWidth);
            assertEquals("150px", computedHeight);

            assertThat(page.getByText("console.log")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
