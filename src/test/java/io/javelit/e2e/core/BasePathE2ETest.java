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
package io.javelit.e2e.core;

import java.nio.file.Files;
import java.nio.file.Path;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.core.Server;
import io.javelit.e2e.helpers.JavelitTestHelper;
import io.javelit.e2e.helpers.PlaywrightUtils;
import io.javelit.e2e.helpers.PortAllocator;
import io.undertow.Undertow;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static io.javelit.e2e.helpers.PlaywrightUtils.HEADLESS;
import static io.javelit.e2e.helpers.PlaywrightUtils.TEST_PROXY_PREFIX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static org.junit.jupiter.api.Assertions.assertTrue;

// test the basePath feature of the server
public class BasePathE2ETest {

  @Test
  public void basePathTest() {
    final JtRunnable app = () -> {
      byte[] imageBytes = Files.readAllBytes(Path.of("examples/image/mountains.jpg"));
      Jt.image(imageBytes).caption("Programmatically generated hexagon (800x400)").use();
    };

    Server javelit = null;
    Undertow proxyServer = null;
    try {
      final Server server = Server
          .builder(app, PortAllocator.getNextAvailablePort())
          .basePath(TEST_PROXY_PREFIX)
          .build();
      javelit = JavelitTestHelper.startServer(server);
      // test in the proxy
      try (final Playwright playwright = Playwright.create();
           final Browser browser = playwright.chromium().launch(HEADLESS);
           final Page page = browser.newPage()) {
        final int proxyPort = PortAllocator.getNextAvailablePort();
        proxyServer = PlaywrightUtils.startProxy(proxyPort, server.port, false);
        final String url = "http://localhost:" + proxyPort + TEST_PROXY_PREFIX;
        page.navigate(url);
        test(page, true);
      }
      // test without the proxy, using the ignoreBasePath special query parameters
      try (final Playwright playwright = Playwright.create();
           final Browser browser = playwright.chromium().launch(HEADLESS);
           final Page page = browser.newPage()) {
        final String url = "http://localhost:" + server.port + "?ignoreBasePath=true";
        page.navigate(url);
        test(page, false);
      }
    } finally {
      if (javelit != null) {
        JavelitTestHelper.stopServer(javelit);
      }
      if (proxyServer != null) {
        proxyServer.stop();
      }
    }
  }

  private static @NotNull void test(final @Nonnull Page page, final boolean proxied) {
    final String prefix = proxied ? TEST_PROXY_PREFIX : "";

    PlaywrightAssertions.assertThat(page.locator("#app jt-image")).isVisible(WAIT_1_SEC_MAX);
    // Verify img element exists
    PlaywrightAssertions.assertThat(page.locator("#app jt-image img")).isVisible(WAIT_1_SEC_MAX);
    // Verify src contains media hash (starts with /_/media/)
    String src = page.locator("#app jt-image img").getAttribute("src");
    assertTrue(src.startsWith(prefix + "/_/media/"),
               "Image src should start with " + prefix + "/_/media/, got: " + src);
  }
}
