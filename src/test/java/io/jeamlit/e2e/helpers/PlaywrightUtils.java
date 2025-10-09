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
package io.jeamlit.e2e.helpers;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.assertions.LocatorAssertions;
import io.jeamlit.core.Server;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.TestInfo;

public final class PlaywrightUtils {

    public static final LocatorAssertions.IsVisibleOptions WAIT_1_SEC_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(
            1000);
    public static final LocatorAssertions.IsVisibleOptions WAIT_100_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(
            100);
    public static final LocatorAssertions.IsVisibleOptions WAIT_50_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(
            50);
    public static final LocatorAssertions.IsVisibleOptions WAIT_10_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(
            10);

    public static final LocatorAssertions.HasTextOptions WAIT_1_SEC_MAX_TEXT = new LocatorAssertions.HasTextOptions().setTimeout(
            1000);
    public static final LocatorAssertions.ContainsTextOptions WAIT_1_SEC_MAX_TEXT_C = new LocatorAssertions.ContainsTextOptions().setTimeout(
            1000);
    public static final LocatorAssertions.HasAttributeOptions WAIT_1_SEC_MAX_ATTRIBUTE = new LocatorAssertions.HasAttributeOptions().setTimeout(
            1000);
    public static final LocatorAssertions.IsHiddenOptions WAIT_1_SEC_MAX_HIDDEN = new LocatorAssertions.IsHiddenOptions().setTimeout(
            1000);
    public static final LocatorAssertions.IsHiddenOptions WAIT_10_MS_MAX_HIDDEN = new LocatorAssertions.IsHiddenOptions().setTimeout(
            10);
    public static final LocatorAssertions.HasClassOptions WAIT_1_SEC_MAX_CLASS = new LocatorAssertions.HasClassOptions().setTimeout(
            1000);

    public static final Locator.ClickOptions WAIT_1_SEC_MAX_CLICK = new Locator.ClickOptions().setTimeout(1000);
    public static final Locator.ClickOptions WAIT_100_MS_MAX_CLICK = new Locator.ClickOptions().setTimeout(100);

    public static final Page.GetByTextOptions EXACT_MATCH = new Page.GetByTextOptions().setExact(true);


    public static final BrowserType.LaunchOptions HEADLESS = new BrowserType.LaunchOptions().setHeadless(true);
    @SuppressWarnings("unused") // used when editing tests
    public static final BrowserType.LaunchOptions NOT_HEADLESS = new BrowserType.LaunchOptions().setHeadless(false);

    private static Browser sharedBrowser;


    public static void runInSharedBrowser(final @Nonnull TestInfo testInfo,
                                          final @Nonnull Path appFile,
                                          final @Nonnull Consumer<Page> run) {
        final Browser browser = getSharedBrowser();
        Server server = null;
        BrowserContext context = null;
        try {
            context = browser.newContext();
            // Enable tracing for screenshots and snapshots
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));

            final Page page = context.newPage();
            server = JeamlitTestHelper.startServer(appFile);
            page.navigate("http://localhost:" + server.port, new Page.NavigateOptions().setTimeout(10000));
            run.accept(page);
        } finally {
            if (context != null) {
                try {
                    final String testMethodName = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
                    final String sanitizedTestName = testMethodName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    context
                            .tracing()
                            .stop(new Tracing.StopOptions().setPath(Paths.get("target/playwright-traces/trace-" + sanitizedTestName + ".zip")));
                } catch (Exception e) {
                    // Ignore trace save errors
                }
                context.close();
            }
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }

    public static void runInDedicatedBrowser(final @Nonnull TestInfo testInfo,
                                             final @Nonnull Path appFile,
                                             final @Nonnull Consumer<Page> run) {
        Server server = null;
        BrowserContext context = null;
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            context = browser.newContext();
            // Enable tracing for screenshots and snapshots
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));

            server = JeamlitTestHelper.startServer(appFile);
            page.navigate("http://localhost:" + server.port, new Page.NavigateOptions().setTimeout(10000));
            run.accept(page);
        } finally {
            if (context != null) {
                try {
                    final String testMethodName = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
                    final String sanitizedTestName = testMethodName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    context
                            .tracing()
                            .stop(new Tracing.StopOptions().setPath(Paths.get("target/playwright-traces/trace-" + sanitizedTestName + ".zip")));
                } catch (Exception e) {
                    // Ignore trace save errors
                }
                context.close();
            }
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }

    private static synchronized Browser getSharedBrowser() {
        if (sharedBrowser == null) {
            final Playwright playwright = Playwright.create();
            sharedBrowser = playwright.chromium().launch(HEADLESS);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (sharedBrowser != null) {
                    sharedBrowser.close();
                }
                playwright.close();
            }));
        }
        return sharedBrowser;
    }


    public static void runInSharedBrowser(final @Nonnull TestInfo testInfo,
                                          final @Nonnull String app,
                                          final @Nonnull Consumer<Page> run) {
        final Path appFile = JeamlitTestHelper.writeTestApp(app);
        runInSharedBrowser(testInfo, appFile, run);
    }

    public static void runInDedicatedBrowser(final @Nonnull TestInfo testInfo,
                                             final @Nonnull String app,
                                             final @Nonnull Consumer<Page> run) {
        final Path appFile = JeamlitTestHelper.writeTestApp(app);
        runInDedicatedBrowser(testInfo, appFile, run);
    }


    // run an embedded jeamlit server
    public static void runInDedicatedBrowser(final @Nonnull TestInfo testInfo,
                                             final @Nonnull Class<?> appClass,
                                             final @Nonnull Consumer<Page> run) {
        Server server = null;
        BrowserContext context = null;
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            context = browser.newContext();
            // Enable tracing for screenshots and snapshots
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
            server = JeamlitTestHelper.startEmbeddedServer(appClass);
            page.navigate("http://localhost:" + server.port);
            run.accept(page);
        } finally {
            if (context != null) {
                try {
                    final String testMethodName = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
                    final String sanitizedTestName = testMethodName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    context
                            .tracing()
                            .stop(new Tracing.StopOptions().setPath(Paths.get("target/playwright-traces/trace-" + sanitizedTestName + ".zip")));
                } catch (Exception e) {
                    // Ignore trace save errors
                }
                context.close();
            }
            JeamlitTestHelper.stopServer(server);
        }
    }

    private PlaywrightUtils() {
    }
}
