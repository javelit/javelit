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
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.javelit.e2e.helpers.PlaywrightUtils.TEST_PROXY_PREFIX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageComponentE2ETest {

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void testImageVariations(final boolean proxied, final TestInfo testInfo) {
    final JtRunnable app = () -> {
      // Test 1: Public URL
      Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
          .caption("Mountain landscape from Unsplash")
          .use();

      // Test 2: Local file
      Jt.image(Path.of("examples/image/mountains.jpg"))
          .caption("Mountains from local file")
          .use();

      // Test 3: SVG
      String svg = """
          <svg width="200" height="200" xmlns="http://www.w3.org/2000/svg">
              <circle cx="100" cy="100" r="80" fill="#4CAF50" />
              <path d="M 60 100 L 90 130 L 140 80" stroke="white" stroke-width="8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          """;
      Jt.imageFromSvg(svg)
          .caption("Simple SVG checkmark icon")
          .use();

      // Test 4: Caption with markdown
      Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
          .caption("**Beautiful mountains** in *stunning* detail. [Learn more](https://unsplash.com)")
          .use();

      // Test 5: Width content
      Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
          .width("content")
          .caption("Content width (natural size)")
          .use();

      // Test 6: Width stretch
      Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
          .width("stretch")
          .caption("Stretched to full width")
          .use();

      // Test 7: Width pixels
      Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
          .width(400)
          .caption("Fixed width: 400px")
          .use();

      // Test 8: Base64 image
      String base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVR42mP8z8DwHwYYGBgYBhBHAQBVhQOBMN0vkwAAAABJRU5ErkJggg==";
      Jt.imageFromBase64(base64Png)
          .caption("Image from base64")
          .use();
    };

    PlaywrightUtils.runInBrowser(testInfo, app, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

      // Verify all 8 image components are rendered
      assertThat(page.locator("#app jt-image").nth(0)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-image").nth(1)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-image").nth(2)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-image").nth(3)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-image").nth(4)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-image").nth(5)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-image").nth(6)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-image").nth(7)).isVisible(WAIT_1_SEC_MAX);

      // Test 1: Public URL
      assertThat(page.locator("#app jt-image").nth(0).locator("img")).isVisible(WAIT_1_SEC_MAX);
      String src1 = page.locator("#app jt-image").nth(0).locator("img").getAttribute("src");
      assertTrue(src1.contains("mountains2.jpg"), "Image src should contain mountains2.jpg, got: " + src1);
      assertThat(page.locator("#app jt-image").nth(0).locator(".caption")).hasText("Mountain landscape from Unsplash",
          WAIT_1_SEC_MAX_TEXT);

      // Test 2: Local file
      assertThat(page.locator("#app jt-image").nth(1).locator("img")).isVisible(WAIT_1_SEC_MAX);
      String src2 = page.locator("#app jt-image").nth(1).locator("img").getAttribute("src");
      assertTrue(src2.startsWith(pathPrefix + "/_/media/"),
          "Image src should start with " + pathPrefix + "/_/media/, got: " + src2);
      assertThat(page.locator("#app jt-image").nth(1).locator(".caption")).hasText("Mountains from local file",
          WAIT_1_SEC_MAX_TEXT);

      // Test 3: SVG
      assertThat(page.locator("#app jt-image").nth(2).locator("img")).isVisible(WAIT_1_SEC_MAX);
      String src3 = page.locator("#app jt-image").nth(2).locator("img").getAttribute("src");
      assertTrue(src3.startsWith("data:image/svg+xml,"), "Image src should be SVG data URI, got: " + src3);
      assertThat(page.locator("#app jt-image").nth(2).locator(".caption")).hasText("Simple SVG checkmark icon",
          WAIT_1_SEC_MAX_TEXT);

      // Test 4: Caption with markdown
      assertThat(page.locator("#app jt-image").nth(3).locator(".caption strong")).hasText("Beautiful mountains",
          WAIT_1_SEC_MAX_TEXT);
      assertThat(page.locator("#app jt-image").nth(3).locator(".caption em")).hasText("stunning", WAIT_1_SEC_MAX_TEXT);
      assertThat(page.locator("#app jt-image").nth(3).locator(".caption a")).hasText("Learn more", WAIT_1_SEC_MAX_TEXT);
      assertThat(page.locator("#app jt-image").nth(3).locator(".caption a")).hasAttribute("href",
          "https://unsplash.com",
          WAIT_1_SEC_MAX_ATTRIBUTE);

      // Test 5: Width content
      assertThat(page.locator("#app jt-image").nth(4)).hasAttribute("width", "content", WAIT_1_SEC_MAX_ATTRIBUTE);

      // Test 6: Width stretch
      assertThat(page.locator("#app jt-image").nth(5)).hasAttribute("width", "stretch", WAIT_1_SEC_MAX_ATTRIBUTE);

      // Test 7: Width pixels
      assertThat(page.locator("#app jt-image").nth(6)).hasAttribute("width", "400", WAIT_1_SEC_MAX_ATTRIBUTE);

      // Test 8: Base64 image
      assertThat(page.locator("#app jt-image").nth(7).locator("img")).isVisible(WAIT_1_SEC_MAX);
      String src8 = page.locator("#app jt-image").nth(7).locator("img").getAttribute("src");
      assertTrue(src8.startsWith("data:image/png;base64,"), "Image src should be base64 data URI, got: " + src8);
      assertThat(page.locator("#app jt-image").nth(7).locator(".caption")).hasText("Image from base64",
          WAIT_1_SEC_MAX_TEXT);
    });
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void testStaticFolder(final boolean proxied, final TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-image-test-");
    copyResourceDirectory("image-test", tempDir);
    final Path appFile = tempDir.resolve("ImageStaticTestApp.java");

    // Verify image component is rendered
    // Verify img element exists
    // Verify src points to static folder
    PlaywrightUtils.runInBrowser(testInfo, appFile, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX + "/" : "";

      // Verify image component is rendered
      assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

      // Verify img element exists
      assertThat(page.locator("#app jt-image img")).isVisible(WAIT_1_SEC_MAX);

      // Verify src points to static folder
      assertThat(page.locator("#app jt-image img")).hasAttribute("src",
          pathPrefix + "app/static/mountains.jpg",
          WAIT_1_SEC_MAX_ATTRIBUTE);
    });
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void testGeneratedBytes(final boolean proxied, final TestInfo testInfo) {
    final JtRunnable app = () -> {
      byte[] imageBytes = Files.readAllBytes(Path.of("examples/image/mountains.jpg"));
      Jt.image(imageBytes)
          .caption("Programmatically generated hexagon (800x400)")
          .use();
    };

    // Verify image component is rendered
    // Verify img element exists
    // Verify src contains media hash (starts with /_/media/)
    // Verify caption
    PlaywrightUtils.runInBrowser(testInfo, app, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

      // Verify image component is rendered
      assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);

      // Verify img element exists
      assertThat(page.locator("#app jt-image img")).isVisible(WAIT_1_SEC_MAX);

      // Verify src contains media hash (starts with /_/media/)
      String src = page.locator("#app jt-image img").getAttribute("src");
      assertTrue(src.startsWith(pathPrefix + "/_/media/"),
          "Image src should start with " + pathPrefix + "/_/media/, got: " + src);

      // Verify caption
      assertThat(page.locator("#app jt-image .caption")).hasText("Programmatically generated hexagon (800x400)",
          WAIT_1_SEC_MAX_TEXT);
    });
  }
}
