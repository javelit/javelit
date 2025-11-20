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
package io.javelit.e2e.components.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.javelit.e2e.helpers.PlaywrightUtils.TEST_PROXY_PREFIX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfComponentE2ETest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testPdfVariations(final boolean proxied, final TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.nio.file.Path;

            public class TestApp {
                public static void main(String[] args) {
                    // Test 1: Public URL
                    Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                        .use();

                    // Test 2: Local file
                    Jt.pdf(Path.of("examples/pdf/sample.pdf"))
                        .use();

                    // Test 3: Height default
                    Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                        .key("diff")
                        .use();

                    // Test 4: Height stretch
                    Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                        .height("stretch")
                        .use();

                    // Test 5: Height pixels
                    Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                        .height(300)
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInBrowser(testInfo, app, true, proxied, page -> {
            final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

            // Verify all 5 PDF components are rendered
            assertThat(page.locator("#app jt-pdf").nth(0)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-pdf").nth(1)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-pdf").nth(2)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-pdf").nth(3)).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-pdf").nth(4)).isVisible(WAIT_1_SEC_MAX);

            // Test 1: Public URL
            assertThat(page.locator("#app jt-pdf").nth(0).locator("iframe")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-pdf").nth(0).locator("iframe")).hasAttribute("src", "https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf");

            // Test 2: Local file
            assertThat(page.locator("#app jt-pdf").nth(1).locator("iframe")).isVisible(WAIT_1_SEC_MAX);
            String src2 = page.locator("#app jt-pdf").nth(1).locator("iframe").getAttribute("src");
            assertTrue(src2.startsWith(pathPrefix + "/_/media/"), "PDF src should start with " + pathPrefix + "/_/media/, got: " + src2);

            // Test 3: Height default
            assertThat(page.locator("#app jt-pdf").nth(2)).hasAttribute("height", "500", WAIT_1_SEC_MAX_ATTRIBUTE);

            // Test 4: Height stretch
            assertThat(page.locator("#app jt-pdf").nth(3)).hasAttribute("height", "stretch", WAIT_1_SEC_MAX_ATTRIBUTE);

            // Test 5: Height pixels
            assertThat(page.locator("#app jt-pdf").nth(4)).hasAttribute("height", "300", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testStaticFolder(final boolean proxied, final TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("javelit-pdf-test-");
        copyResourceDirectory("pdf-test", tempDir);
        final Path appFile = tempDir.resolve("PdfStaticTestApp.java");

        // Verify PDF component is rendered
        // Verify iframe element exists
        // Verify src points to static folder
        PlaywrightUtils.runInBrowser(testInfo, appFile, true, proxied, page -> {
            final String pathPrefix = proxied ? TEST_PROXY_PREFIX + "/" : "";

            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify iframe element exists
            assertThat(page.locator("#app jt-pdf iframe")).isVisible(WAIT_1_SEC_MAX);

            // Verify src points to static folder
            assertThat(page.locator("#app jt-pdf iframe")).hasAttribute("src", pathPrefix + "app/static/sample.pdf", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testGeneratedBytes(final boolean proxied, final TestInfo testInfo) {
        JtRunnable runnable = () -> {
            byte[] pdfBytes = null;
            pdfBytes = Files.readAllBytes(Path.of("examples/pdf/sample.pdf"));
            Jt.pdf(pdfBytes)
              .use();
        };
        PlaywrightUtils.runInBrowser(testInfo, runnable, true, proxied, page -> {
            final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify iframe element exists
            assertThat(page.locator("#app jt-pdf iframe")).isVisible(WAIT_1_SEC_MAX);

            // Verify src contains media hash (starts with /_/media/)
            String src = page.locator("#app jt-pdf iframe").getAttribute("src");
            assertTrue(src.startsWith(pathPrefix + "/_/media/"), "PDF src should start with " + pathPrefix + "/_/media/, got: " + src);
        });
    }
}
