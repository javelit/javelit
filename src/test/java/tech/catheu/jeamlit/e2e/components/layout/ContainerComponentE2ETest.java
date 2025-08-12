package tech.catheu.jeamlit.e2e.components.layout;

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
 * End-to-end tests for ContainerComponent.
 */
public class ContainerComponentE2ETest {
    
    @Test
    void testContainerContent() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            import tech.catheu.jeamlit.components.layout.ContainerComponent;import tech.catheu.jeamlit.core.JtContainer;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Before container").use();
                    
                    JtContainer container = Jt.container("test-container").use();
                    Jt.text("Inside container").use(container);
                    Jt.button("Container Button").use(container);
                    
                    Jt.text("After container").use();
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
            
            // Wait for container to be visible
            assertThat(page.locator("jt-container")).isVisible(WAIT_1_SEC_MAX);
            // Check content before container
            assertThat(page.locator("jt-text", new Page.LocatorOptions().setHasText("Before container"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-container", new Page.LocatorOptions().setHasText("Before container"))).not().isVisible(WAIT_1_SEC_MAX);
            // Check content inside container
            assertThat(page.locator("jt-container", new Page.LocatorOptions().setHasText("Inside container"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-container", new Page.LocatorOptions().setHasText("Container Button"))).isVisible(WAIT_1_SEC_MAX);
            // Check content after container
            assertThat(page.locator("jt-text", new Page.LocatorOptions().setHasText("After container"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-container", new Page.LocatorOptions().setHasText("After container"))).not().isVisible(WAIT_1_SEC_MAX);
            
        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }
}