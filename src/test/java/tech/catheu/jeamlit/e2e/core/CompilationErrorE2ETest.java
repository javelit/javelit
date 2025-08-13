package tech.catheu.jeamlit.e2e.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.core.Server;
import tech.catheu.jeamlit.e2e.helpers.JeamlitTestHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.HEADLESS;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for compilation error handling.
 */
public class CompilationErrorE2ETest {

    @Test
    void testCompilationErrorModalAppears() throws IOException, InterruptedException {
        // App with compilation error (missing semicolon)
        final @Language("java") String invalidApp = """
            import tech.catheu.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.title("Test App").use()  // Missing semicolon - COMPILATION ERROR
                    Jt.text("This app has an error.").use();
                }
            }
            """;

        final Path appFile = JeamlitTestHelper.writeTestApp(invalidApp);
        Server server = null;

        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            server = JeamlitTestHelper.startServer(appFile);
            page.navigate("http://localhost:" + server.port);

            // Verify app loads correctly first
            assertThat(page.getByText("';' expected")).isVisible(WAIT_1_SEC_MAX);

        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }
}