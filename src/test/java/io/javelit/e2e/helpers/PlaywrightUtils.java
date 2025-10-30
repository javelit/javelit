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
package io.javelit.e2e.helpers;

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
import io.javelit.core.Server;
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

    // set this to true in dev to remove HEADLESS browser and add traces
    private static final boolean DEBUG = false;
    public static final BrowserType.LaunchOptions HEADLESS = new BrowserType.LaunchOptions().setHeadless(true);

    public static void runInBrowser(final @Nonnull TestInfo testInfo,
                                    final @Nonnull Path appFile,
                                    final @Nonnull Consumer<Page> run) {
        Server server = null;
        BrowserContext context = null;
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            if (DEBUG) {
                // Enable tracing for screenshots and snapshots
                context = browser.newContext();
                context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
            }

            server = JavelitTestHelper.startServer(appFile);
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
            JavelitTestHelper.stopServer(server);
            JavelitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }

    public static void runInBrowser(final @Nonnull TestInfo testInfo,
                                    final @Nonnull String app,
                                    final @Nonnull Consumer<Page> run) {
        final Path appFile = JavelitTestHelper.writeTestApp(app);
        runInBrowser(testInfo, appFile, run);
    }



    // run an embedded javelit server
    // uses Class run - will be removed, use Runnable instead
    @Deprecated
    public static void runInBrowser(final @Nonnull TestInfo testInfo,
                                    final @Nonnull Class<?> appClass,
                                    final @Nonnull Consumer<Page> run) {
        Server server = null;
        BrowserContext context = null;
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            if (DEBUG) {
                // Enable tracing for screenshots and snapshots
                context = browser.newContext();
                context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
            }
            server = JavelitTestHelper.startEmbeddedServer(appClass);
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
            JavelitTestHelper.stopServer(server);
        }
    }

    public static void runInBrowser(final @Nonnull TestInfo testInfo,
                                    final @Nonnull Runnable appClass,
                                    final @Nonnull Consumer<Page> run) {
        Server server = null;
        BrowserContext context = null;
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            if (DEBUG) {
                // Enable tracing for screenshots and snapshots
                context = browser.newContext();
                context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
            }
            server = JavelitTestHelper.startEmbeddedServer(appClass);
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
            JavelitTestHelper.stopServer(server);
        }
    }

    private PlaywrightUtils() {
    }
}
