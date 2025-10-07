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

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import io.jeamlit.cli.Cli;
import io.jeamlit.e2e.helpers.JeamlitTestHelper;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.HEADLESS;
import static io.jeamlit.e2e.helpers.PortAllocator.getNextAvailablePort;

/**
 * End-to-end tests for running remote files via URL with the CLI.
 * NOTE: the classpath used by the command here is the test jvm classpath - this is not the behaviour when calling from the jar but this is ok to ignore for this test
 */
public class RemoteFileE2ETest {

    @Test
    void testRunRemoteFileViaUrl() {
        final int port = getNextAvailablePort();
        final String remoteUrl = "https://raw.githubusercontent.com/jeamlit/jeamlit/refs/heads/main/src/test/resources/RemoteUrlApp.java";

        // Execute jeamlit CLI in a separate thread
        final Thread cliThread = new Thread(() -> {
            // Use main method approach since subcommands are package-private
            String[] args = {"run", remoteUrl, "--port", String.valueOf(port), "--no-browser"};
            Cli.main(args);
        });
        cliThread.setDaemon(true);
        cliThread.start();

        JeamlitTestHelper.waitForServerReady(port);

        // Verify with Playwright that the app is running
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            page.navigate("http://localhost:" + port, new Page.NavigateOptions().setTimeout(10000));

            // Verify the title from the remote app is visible
            final LocatorAssertions.IsVisibleOptions timeout = new LocatorAssertions.IsVisibleOptions().setTimeout(5000);
            assertThat(page.locator("h1:has-text('Hello World')")).isVisible(timeout);
        }
    }
}
