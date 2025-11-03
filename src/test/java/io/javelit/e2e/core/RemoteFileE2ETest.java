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

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import io.javelit.cli.Cli;
import io.javelit.e2e.helpers.JavelitTestHelper;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.HEADLESS;
import static io.javelit.e2e.helpers.PortAllocator.getNextAvailablePort;

/**
 * End-to-end tests for running remote files via URL with the CLI.
 * NOTE: the classpath used by the command here is the test jvm classpath - this is not the behaviour when calling from the jar but this is ok to ignore for this test
 */
public class RemoteFileE2ETest {

    @Test
    void testRunRemoteFileViaPlainTextUrl() {
        final int port = getNextAvailablePort();
        final String remoteUrl = "https://raw.githubusercontent.com/javelit/javelit/refs/heads/main/src/test/resources/RemoteUrlApp.java";

        // Execute javelit CLI in a separate thread
        final Thread cliThread = new Thread(() -> {
            // Use main method approach since subcommands are package-private
            String[] args = {"run", remoteUrl, "--port", String.valueOf(port), "--no-browser"};
            Cli.main(args);
        });
        cliThread.setDaemon(true);
        cliThread.start();

        JavelitTestHelper.waitForServerReady(port);

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

    @Test
    void testRunRemoteFileViaGithubBlobUrl() {
        final int port = getNextAvailablePort();
        // also test without protocol
        final String remoteUrl = "https://github.com/javelit/javelit/blob/refs/heads/main/src/test/resources/RemoteUrlApp.java";

        // Execute javelit CLI in a separate thread
        final Thread cliThread = new Thread(() -> {
            // Use main method approach since subcommands are package-private
            String[] args = {"run", remoteUrl, "--port", String.valueOf(port), "--no-browser"};
            Cli.main(args);
        });
        cliThread.setDaemon(true);
        cliThread.start();

        JavelitTestHelper.waitForServerReady(port);

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

    @Test
    void testRunRemoteGitHubFolder() {
        final int port = getNextAvailablePort();
        final String remoteFolderUrl = "https://github.com/javelit/javelit/tree/main/src/test/resources/multipage-remote";

        // Execute javelit CLI in a separate thread
        final Thread cliThread = new Thread(() -> {
            // Use main method approach since subcommands are package-private
            String[] args = {"run", remoteFolderUrl, "--port", String.valueOf(port), "--no-browser"};
            Cli.main(args);
        });
        cliThread.setDaemon(true);
        cliThread.start();

        JavelitTestHelper.waitForServerReady(port);

        // Verify with Playwright that the app is running
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            page.navigate("http://localhost:" + port, new Page.NavigateOptions().setTimeout(10000));

            // Verify the multipage navigation is visible (checking for "Dashboard" link)
            final LocatorAssertions.IsVisibleOptions timeout = new LocatorAssertions.IsVisibleOptions().setTimeout(5000);
            assertThat(page.getByText("Users")).isVisible(timeout);
        }
    }

    @Test
    void testRunRemotePlainGitHubRepoUrl() {
        final int port = getNextAvailablePort();
        final String repoUrl = "https://github.com/javelit/jeamlit-example-standalone-vanilla";

        // Execute javelit CLI in a separate thread
        final Thread cliThread = new Thread(() -> {
            // Use main method approach since subcommands are package-private
            String[] args = {"run", repoUrl, "--port", String.valueOf(port), "--no-browser"};
            Cli.main(args);
        });
        cliThread.setDaemon(true);
        cliThread.start();

        JavelitTestHelper.waitForServerReady(port);

        // Verify with Playwright that the app is running
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            page.navigate("http://localhost:" + port, new Page.NavigateOptions().setTimeout(10000));

            // Verify the app loaded (check for any visible content - adjust based on actual app)
            final LocatorAssertions.IsVisibleOptions timeout = new LocatorAssertions.IsVisibleOptions().setTimeout(5000);
            assertThat(page.locator("#app")).isVisible(timeout);
        }
    }

    @Test
    void testRunRemoteGitHubRepoRootWithTree() {
        final int port = getNextAvailablePort();
        final String repoUrl = "https://github.com/javelit/jeamlit-example-standalone-vanilla/tree/main";

        // Execute javelit CLI in a separate thread
        final Thread cliThread = new Thread(() -> {
            // Use main method approach since subcommands are package-private
            String[] args = {"run", repoUrl, "--port", String.valueOf(port), "--no-browser"};
            Cli.main(args);
        });
        cliThread.setDaemon(true);
        cliThread.start();

        JavelitTestHelper.waitForServerReady(port);

        // Verify with Playwright that the app is running
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            page.navigate("http://localhost:" + port, new Page.NavigateOptions().setTimeout(10000));

            // Verify the app loaded (check for any visible content - adjust based on actual app)
            final LocatorAssertions.IsVisibleOptions timeout = new LocatorAssertions.IsVisibleOptions().setTimeout(5000);
            assertThat(page.locator("#app")).isVisible(timeout);
        }
    }
}
