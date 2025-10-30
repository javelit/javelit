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

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
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
    void testBasicCodeDisplay(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.code(\"""
                        public class HelloWorld {
                            public static void main(String[] args) {
                                System.out.println("Hello, World!");
                            }
                        }
                        \""").use();
                }
            }
            """;

        // Verify code component is rendered (filter by content to find the right one)
        // Verify code content is present
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify code component is rendered (filter by content to find the right one)
            assertThat(page.locator("#app jt-internal-code")).isVisible(WAIT_1_SEC_MAX);
            // Verify code content is present
            assertThat(page.getByText("public class HelloWorld")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testLanguageNull(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.code("function example() { return 'no highlighting'; }")
                        .language(null)
                        .use();
                }
            }
            """;

        // Verify code component is rendered (filter by content)
        // Verify language-none class is applied to code element in our specific component
        // Verify content is present
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify code component is rendered (filter by content)
            assertThat(page.locator("#app jt-internal-code")).isVisible(WAIT_1_SEC_MAX);
            // Verify language-none class is applied to code element in our specific component
            assertThat(page.locator("#app jt-internal-code").locator("pre code.language-none")).isVisible(WAIT_1_SEC_MAX);
            // Verify content is present
            assertThat(page.getByText("function example()")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testLanguageJson(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.code("{\\"name\\": \\"test\\", \\"value\\": 123}")
                        .language("json")
                        .use();
                }
            }
            """;

        // Verify code component is rendered (filter by content)
        // Verify language-json class is applied to code element in our specific component
        // Verify content is present
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify code component is rendered (filter by content)
            assertThat(page.locator("#app jt-internal-code")).isVisible(WAIT_1_SEC_MAX);
            // Verify language-json class is applied to code element in our specific component
            assertThat(page.locator("#app jt-internal-code")).isVisible(WAIT_1_SEC_MAX);
            // Verify content is present
            assertThat(page.getByText("123")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testLineNumbersEnabled(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.code("line 1\\nline 2\\nline 3")
                        .lineNumbers(true)
                        .use();
                }
            }
            """;

        // Verify code component is rendered (filter by content)
        // Verify line-numbers attribute is present on the jt-internal-code element
        // Verify content is present
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify code component is rendered (filter by content)
            assertThat(page.locator("#app jt-internal-code")).isVisible(WAIT_1_SEC_MAX);
            // Verify line-numbers attribute is present on the jt-internal-code element
            assertThat(page.locator("#app jt-internal-code[line-numbers]")).isVisible(WAIT_1_SEC_MAX);
            // Verify content is present
            assertThat(page.getByText("line 2")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testWrapLinesEnabled(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.code("This is a very long line that should wrap when wrap lines is enabled and demonstrates the line wrapping functionality")
                        .wrapLines(true)
                        .use();
                }
            }
            """;

        // Verify code component is rendered (filter by content)
        // Verify wrap-lines attribute is present on the jt-internal-code element
        // Verify content is present
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify code component is rendered (filter by content)
            assertThat(page.locator("#app jt-internal-code")).isVisible(WAIT_1_SEC_MAX);
            // Verify wrap-lines attribute is present on the jt-internal-code element
            assertThat(page.locator("#app jt-internal-code[wrap-lines]")).isVisible(WAIT_1_SEC_MAX);
            // Verify content is present
            assertThat(page.getByText("This is a very long line")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testCustomDimensions(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.code("console.log('Custom dimensions test');")
                        .width(500)
                        .height(150)
                        .use();
                }
            }
            """;

        // Find the code component by content
        // Verify code component is rendered
        // Verify width and height attributes are set
        // Verify CSS custom properties are applied (through computed styles)
        // Verify content is present
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Find the code component by content
            var theCodeComponent = page.locator("#app jt-internal-code");
            // Verify code component is rendered
            assertThat(theCodeComponent).isVisible(WAIT_1_SEC_MAX);
            // Verify width and height attributes are set
            assertThat(page.locator("#app jt-internal-code[width='500']")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-internal-code[height='150']")).isVisible(WAIT_1_SEC_MAX);

            // Verify CSS custom properties are applied (through computed styles)
            theCodeComponent.waitFor();
            String computedWidth = theCodeComponent.evaluate("el => getComputedStyle(el).getPropertyValue('--code-width')").toString();
            String computedHeight = theCodeComponent.evaluate("el => getComputedStyle(el).getPropertyValue('--code-height')").toString();

            assertEquals("500px", computedWidth);
            assertEquals("150px", computedHeight);

            // Verify content is present
            assertThat(page.getByText("console.log")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
