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
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageComponentE2ETest {

    @Test
    void testPublicUrl(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                        .caption("Mountain landscape from Unsplash")
                        .use();
                }
            }
            """;

        // Verify image component is rendered
        // Verify img element exists
        // Verify src attribute
        // Verify caption is present
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify img element exists
            assertThat(page.locator("#app jt-image img")).isVisible(WAIT_1_SEC_MAX);

            // Verify src attribute
            String src = page.locator("#app jt-image img").getAttribute("src");
            assertTrue(src.contains("mountains2.jpg"), "Image src should contain mountains2.jpg, got: " + src);

            // Verify caption is present
            assertThat(page.locator("#app jt-image .caption")).hasText("Mountain landscape from Unsplash", WAIT_1_SEC_MAX_TEXT);
        });
    }

    @Test
    void testStaticFolder(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("javelit-image-test-");
        copyResourceDirectory("image-test", tempDir);
        final Path appFile = tempDir.resolve("ImageStaticTestApp.java");

        // Verify image component is rendered
        // Verify img element exists
        // Verify src points to static folder
        PlaywrightUtils.runInBrowser(testInfo, appFile, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify img element exists
            assertThat(page.locator("#app jt-image img")).isVisible(WAIT_1_SEC_MAX);

            // Verify src points to static folder
            assertThat(page.locator("#app jt-image img")).hasAttribute("src", "app/static/mountains.jpg", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @Test
    void testLocalFile(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.nio.file.Path;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.image(Path.of("examples/image/mountains.jpg"))
                        .caption("Mountains from local file")
                        .use();
                }
            }
            """;

        // Verify image component is rendered
        // Verify img element exists
        // Verify src contains media hash (starts with /_/media/)
        // Verify caption
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify img element exists
            assertThat(page.locator("#app jt-image img")).isVisible(WAIT_1_SEC_MAX);

            // Verify src contains media hash (starts with /_/media/)
            String src = page.locator("#app jt-image img").getAttribute("src");
            assertTrue(src.startsWith("/_/media/"), "Image src should start with /_/media/, got: " + src);

            // Verify caption
            assertThat(page.locator("#app jt-image .caption")).hasText("Mountains from local file", WAIT_1_SEC_MAX_TEXT);
        });
    }

    @Test
    void testGeneratedBytes(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import javax.imageio.ImageIO;

            public class TestApp {
                public static void main(String[] args) throws Exception {
                    byte[] imageBytes = Files.readAllBytes(Path.of("examples/image/mountains.jpg"));
                    Jt.image(imageBytes)
                        .caption("Programmatically generated hexagon (800x400)")
                        .use();
                }
            }
            """;

        // Verify image component is rendered
        // Verify img element exists
        // Verify src contains media hash (starts with /_/media/)
        // Verify caption
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify img element exists
            assertThat(page.locator("#app jt-image img")).isVisible(WAIT_1_SEC_MAX);

            // Verify src contains media hash (starts with /_/media/)
            String src = page.locator("#app jt-image img").getAttribute("src");
            assertTrue(src.startsWith("/_/media/"), "Image src should start with /_/media/, got: " + src);

            // Verify caption
            assertThat(page.locator("#app jt-image .caption")).hasText("Programmatically generated hexagon (800x400)", WAIT_1_SEC_MAX_TEXT);
        });
    }

    @Test
    void testSvg(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    String svg = \"\"\"
                        <svg width="200" height="200" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="100" cy="100" r="80" fill="#4CAF50" />
                            <path d="M 60 100 L 90 130 L 140 80" stroke="white" stroke-width="8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                        \"\"\";
                    Jt.imageFromSvg(svg)
                        .caption("Simple SVG checkmark icon")
                        .use();
                }
            }
            """;

        // Verify image component is rendered
        // Verify img element exists
        // Verify src is a data URI with SVG
        // Verify caption
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify img element exists
            assertThat(page.locator("#app jt-image img")).isVisible(WAIT_1_SEC_MAX);

            // Verify src is a data URI with SVG
            String src = page.locator("#app jt-image img").getAttribute("src");
            assertTrue(src.startsWith("data:image/svg+xml,"), "Image src should be SVG data URI, got: " + src);

            // Verify caption
            assertThat(page.locator("#app jt-image .caption")).hasText("Simple SVG checkmark icon", WAIT_1_SEC_MAX_TEXT);
        });
    }

    @Test
    void testCaptionWithMarkdown(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                        .caption("**Beautiful mountains** in *stunning* detail. [Learn more](https://unsplash.com)")
                        .use();
                }
            }
            """;

        // Verify image component is rendered
        // Verify caption contains markdown formatted elements
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify caption contains markdown formatted elements
            assertThat(page.locator("#app jt-image .caption strong")).hasText("Beautiful mountains", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("#app jt-image .caption em")).hasText("stunning", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("#app jt-image .caption a")).hasText("Learn more", WAIT_1_SEC_MAX_TEXT);
            assertThat(page.locator("#app jt-image .caption a")).hasAttribute("href", "https://unsplash.com", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @Test
    void testWidthContent(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                        .width("content")
                        .caption("Content width (natural size)")
                        .use();
                }
            }
            """;

        // Verify image component is rendered
        // Verify width attribute
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify width attribute
            assertThat(page.locator("#app jt-image")).hasAttribute("width", "content", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @Test
    void testWidthStretch(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                        .width("stretch")
                        .caption("Stretched to full width")
                        .use();
                }
            }
            """;

        // Verify image component is rendered
        // Verify width attribute
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify width attribute
            assertThat(page.locator("#app jt-image")).hasAttribute("width", "stretch", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }

    @Test
    void testWidthPixels(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                        .width(400)
                        .caption("Fixed width: 400px")
                        .use();
                }
            }
            """;

        // Verify image component is rendered
        // Verify width attribute
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify image component is rendered
            assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

            // Verify width attribute
            assertThat(page.locator("#app jt-image")).hasAttribute("width", "400", WAIT_1_SEC_MAX_ATTRIBUTE);
        });
    }
}
