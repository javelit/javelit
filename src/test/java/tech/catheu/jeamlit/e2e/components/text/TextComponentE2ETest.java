package tech.catheu.jeamlit.e2e.components.text;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.core.Server;
import tech.catheu.jeamlit.e2e.helpers.JeamlitTestHelper;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.HEADLESS;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for TextComponent.
 */
public class TextComponentE2ETest {
    
    @Test
    void testTextDisplay() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("A first text").use();
                    Jt.text("A second text").use();
                }
            }
            """;
        
        final Path appFile = JeamlitTestHelper.writeTestApp(app);
        Server server = null;
        
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
            server = JeamlitTestHelper.startServer(appFile);
            page.navigate("http://localhost:" + server.port);
            
            // Wait for text components to be visible
            assertThat(page.locator("jt-text")).hasCount(2, new LocatorAssertions.HasCountOptions().setTimeout(1000));
            assertThat(page.getByText("A first text")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("A second text")).isVisible(WAIT_1_SEC_MAX);
            
        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }
}