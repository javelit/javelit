package tech.catheu.jeamlit.e2e.components.layout;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.core.Server;
import tech.catheu.jeamlit.e2e.helpers.JeamlitTestHelper;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.HEADLESS;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for ExpanderComponent.
 */
public class ExpanderComponentE2ETest {
    
    @Test
    void testExpanderToggle() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            import tech.catheu.jeamlit.components.layout.ExpanderComponent;import tech.catheu.jeamlit.core.JtContainer;
            
            public class TestApp {
                public static void main(String[] args) {
                    JtContainer expanderContainer = Jt.expander("test-expander", "Click to expand").use();
                    Jt.text("Hidden content inside expander").use(expanderContainer);
                    Jt.button("Hidden Button").use(expanderContainer);
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
            
            // Wait for expander to be visible
            final Locator expanderLocator = page.locator("jt-expander");
            assertThat(expanderLocator).isVisible(WAIT_1_SEC_MAX);
            // Check expander header is visible
            assertThat(page.locator("jt-expander summary", new Page.LocatorOptions().setHasText("Click to expand"))).isVisible(WAIT_1_SEC_MAX);
            // Initially, content should be hidden (collapsed)
            assertThat(page.getByText("Hidden content inside expander")).not().isVisible(WAIT_1_SEC_MAX);
            // Click to expand
            expanderLocator.click();
            // Check that content is now visible
            assertThat(page.getByText("Hidden content inside expander")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Hidden Button")).isVisible(WAIT_1_SEC_MAX);
        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }
}