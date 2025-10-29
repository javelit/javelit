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

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfComponentE2ETest {

    @Test
    void testPublicUrl(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify iframe element exists
            assertThat(page.locator("#app jt-pdf iframe")).isVisible(WAIT_1_SEC_MAX);

            // Verify src attribute points to PDF URL
            assertThat(page.locator("#app jt-pdf iframe")).hasAttribute("src", "https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf");
        });
    }

    @Test
    void testStaticFolder(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("javelit-pdf-test-");
        copyResourceDirectory("pdf-test", tempDir);
        final Path appFile = tempDir.resolve("PdfStaticTestApp.java");

        PlaywrightUtils.runInSharedBrowser(testInfo, appFile, page -> {
            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify iframe element exists
            assertThat(page.locator("#app jt-pdf iframe")).isVisible(WAIT_1_SEC_MAX);

            // Verify src points to static folder
            assertThat(page.locator("#app jt-pdf iframe")).hasAttribute("src", "app/static/sample.pdf", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @Test
    void testLocalFile(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.nio.file.Path;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.pdf(Path.of("examples/pdf/sample.pdf"))
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify iframe element exists
            assertThat(page.locator("#app jt-pdf iframe")).isVisible(WAIT_1_SEC_MAX);

            // Verify src contains media hash (starts with /_/media/)
            String src = page.locator("#app jt-pdf iframe").getAttribute("src");
            assertTrue(src.startsWith("/_/media/"), "PDF src should start with /_/media/, got: " + src);
        });
    }

    @Test
    void testGeneratedBytes(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.nio.file.Files;
            import java.nio.file.Path;

            public class TestApp {
                public static void main(String[] args) throws Exception {
                    byte[] pdfBytes = Files.readAllBytes(Path.of("examples/pdf/sample.pdf"));
                    Jt.pdf(pdfBytes)
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify iframe element exists
            assertThat(page.locator("#app jt-pdf iframe")).isVisible(WAIT_1_SEC_MAX);

            // Verify src contains media hash (starts with /_/media/)
            String src = page.locator("#app jt-pdf iframe").getAttribute("src");
            assertTrue(src.startsWith("/_/media/"), "PDF src should start with /_/media/, got: " + src);
        });
    }

    @Test
    void testHeightDefault(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify default height attribute is 500
            assertThat(page.locator("#app jt-pdf")).hasAttribute("height", "500", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @Test
    void testHeightStretch(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                        .height("stretch")
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify height attribute is stretch
            assertThat(page.locator("#app jt-pdf")).hasAttribute("height", "stretch", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @Test
    void testHeightPixels(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                        .height(300)
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify PDF component is rendered
            assertThat(page.locator("#app jt-pdf")).isVisible(WAIT_1_SEC_MAX);

            // Verify height attribute is 300
            assertThat(page.locator("#app jt-pdf")).hasAttribute("height", "300", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }
}
