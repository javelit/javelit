package tech.catheu.jeamlit.e2e.components.text;

import com.microsoft.playwright.*;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.core.Server;
import tech.catheu.jeamlit.e2e.helpers.JeamlitTestHelper;
import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.HEADLESS;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for TitleComponent.
 */
public class TitleComponentE2ETest {
    
    @Test
    void testTitleDisplay() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.title("Main Title").use();
                    Jt.text("Some content under the title").use();
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
            
            // Wait for title to be visible
            assertThat(page.locator("jt-title")).isVisible(WAIT_1_SEC_MAX);
            // Check title is rendered
            assertThat(page.getByText("Main Title")).isVisible(WAIT_1_SEC_MAX);
            // Check content after title
            assertThat(page.getByText("Some content under the title")).isVisible(WAIT_1_SEC_MAX);
            
        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }
}