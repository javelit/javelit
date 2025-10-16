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
package io.jeamlit.e2e.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Response;
import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for static file serving functionality.
 * Tests that the server correctly serves static files from the ./static folder.
 */
public class StaticServingE2ETest {

    @Test
    void testBasicStaticFileServing(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("jeamlit-static-test-");
        final Path staticDir = tempDir.resolve("static");
        Files.createDirectories(staticDir);

        // Create a test image file
        final Path imageFile = staticDir.resolve("test-image.png");
        Files.write(imageFile, "PNG_FAKE_CONTENT".getBytes(StandardCharsets.UTF_8));

        // Create a test app that references the static file
        final String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Static file serving test").use();
                    Jt.markdown("![test image](/app/static/test-image.png)").use();
                }
            }
            """;

        final Path appFile = tempDir.resolve("TestApp.java");
        Files.writeString(appFile, app);

        PlaywrightUtils.runInSharedBrowser(testInfo, appFile, page -> {
            // Verify the app loads correctly
            assertThat(page.getByText("Static file serving test")).isVisible(WAIT_1_SEC_MAX);

            // Test direct access to static file
            final String baseUrl = page.url().substring(0, page.url().lastIndexOf('/'));
            final Response response = page.navigate(baseUrl + "/app/static/test-image.png");

            assertNotNull(response);
            assertEquals(200, response.status());
            assertEquals("nosniff", response.headers().get("x-content-type-options"));
            assertNotNull(response.headers().get("cache-control"));
            assertTrue(response.body().length > 0);
        });
    }

    @Test
    void testNoStaticFileFolder(TestInfo testInfo) throws IOException {
        final String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Static file 404 test").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify the app loads correctly
            assertThat(page.getByText("Static file 404 test")).isVisible(WAIT_1_SEC_MAX);

            // Test access to non-existent static file when no static directory exists
            // When no static directory exists, /app/static/* requests fall through to IndexHandler
            // which serves the main app, so we expect a 200 response with the app content
            final String baseUrl = page.url().substring(0, page.url().lastIndexOf('/'));
            final Response response = page.navigate(baseUrl + "/app/static/nonexistent.png");

            assertNotNull(response);
            assertEquals(200, response.status());
        });
    }

    @Test
    void testMultipleStaticFileTypes(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("jeamlit-static-multitype-test-");
        final Path staticDir = tempDir.resolve("static");
        Files.createDirectories(staticDir);

        // Create different types of static files
        Files.write(staticDir.resolve("test.txt"), "Hello World!".getBytes(StandardCharsets.UTF_8));
        Files.write(staticDir.resolve("test.css"), "body { color: red; }".getBytes(StandardCharsets.UTF_8));
        Files.write(staticDir.resolve("test.js"), "console.log('test');".getBytes(StandardCharsets.UTF_8));
        Files.write(staticDir.resolve("test.json"), "{\"test\": true}".getBytes(StandardCharsets.UTF_8));

        final String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Multi-type static files test").use();
                }
            }
            """;

        final Path appFile = tempDir.resolve("TestApp.java");
        Files.writeString(appFile, app);

        PlaywrightUtils.runInSharedBrowser(testInfo, appFile, page -> {
            final String baseUrl = page.url().substring(0, page.url().lastIndexOf('/'));

            // Test text file
            Response response = page.navigate(baseUrl + "/app/static/test.txt");
            assertEquals(200, response.status());
            assertEquals("nosniff", response.headers().get("x-content-type-options"));
            assertEquals("text/plain", response.headers().get("content-type"));

            // Test CSS file
            response = page.navigate(baseUrl + "/app/static/test.css");
            assertEquals(200, response.status());
            assertEquals("nosniff", response.headers().get("x-content-type-options"));
            assertEquals("text/css", response.headers().get("content-type"));

            // Test JS file
            response = page.navigate(baseUrl + "/app/static/test.js");
            assertEquals(200, response.status());
            assertEquals("nosniff", response.headers().get("x-content-type-options"));
            assertEquals("application/javascript", response.headers().get("content-type"));

            // Test JSON file
            response = page.navigate(baseUrl + "/app/static/test.json");
            assertEquals(200, response.status());
            assertEquals("nosniff", response.headers().get("x-content-type-options"));
            assertEquals("application/json", response.headers().get("content-type"));
        });
    }

    @Test
    void testStaticDirectoryListingDisabled(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("jeamlit-static-listing-test-");
        final Path staticDir = tempDir.resolve("static");
        Files.createDirectories(staticDir);

        Files.write(staticDir.resolve("file1.txt"), "content1".getBytes(StandardCharsets.UTF_8));
        Files.write(staticDir.resolve("file2.txt"), "content2".getBytes(StandardCharsets.UTF_8));

        final String app = """
            import io.jeamlit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Directory listing test").use();
                }
            }
            """;

        final Path appFile = tempDir.resolve("TestApp.java");
        Files.writeString(appFile, app);

        PlaywrightUtils.runInSharedBrowser(testInfo, appFile, page -> {
            // Try to access the static directory directly
            final String baseUrl = page.url().substring(0, page.url().lastIndexOf('/'));
            final APIResponse response = page.request().get(baseUrl + "/app/static/");

            // Should get 403 Forbidden or 404 Not Found (directory listing disabled)
            assertTrue(response.status() == 403 || response.status() == 404);
        });
    }
}
