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
package tech.catheu.jeamlit.e2e.helpers;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import tech.catheu.jeamlit.core.Server;

public final class PlaywrightUtils {

    public static final LocatorAssertions.IsVisibleOptions WAIT_1_SEC_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(1000);
    public static final LocatorAssertions.IsVisibleOptions WAIT_100_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(100);
    public static final LocatorAssertions.IsVisibleOptions WAIT_50_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(50);
    public static final LocatorAssertions.IsVisibleOptions WAIT_10_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(10);
    public static final BrowserType.LaunchOptions HEADLESS = new BrowserType.LaunchOptions().setHeadless(true);
    @SuppressWarnings("unused") // used when editing tests
    public static final BrowserType.LaunchOptions NOT_HEADLESS = new BrowserType.LaunchOptions().setHeadless(false);


    public static void runInBrowser(final Path appFile, Consumer<Page> run) {
        Server server = null;
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            server = JeamlitTestHelper.startServer(appFile);
            page.navigate("http://localhost:" + server.port);
            run.accept(page);
        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }


    public static void runInBrowser(final String app, Consumer<Page> run) {
        final Path appFile = JeamlitTestHelper.writeTestApp(app);
        runInBrowser(appFile, run);
    }

    private PlaywrightUtils() {
    }
}
