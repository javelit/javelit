package tech.catheu.jeamlit.e2e.helpers;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import tech.catheu.jeamlit.core.Server;

import java.nio.file.Path;
import java.util.function.Consumer;

public class PlaywrightUtils {

    public static final LocatorAssertions.IsVisibleOptions WAIT_1_SEC_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(1000);
    public static final LocatorAssertions.IsVisibleOptions WAIT_100_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(100);
    public static final LocatorAssertions.IsVisibleOptions WAIT_50_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(50);
    public static final LocatorAssertions.IsVisibleOptions WAIT_10_MS_MAX = new LocatorAssertions.IsVisibleOptions().setTimeout(10);
    public static final BrowserType.LaunchOptions HEADLESS = new BrowserType.LaunchOptions().setHeadless(true);
    @SuppressWarnings("unused") // used when editing tests
    public static final BrowserType.LaunchOptions NOT_HEADLESS = new BrowserType.LaunchOptions().setHeadless(false);


    public static void runInBrowser(final String app, Consumer<Page> run) {
        final Path appFile = JeamlitTestHelper.writeTestApp(app);
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
}
