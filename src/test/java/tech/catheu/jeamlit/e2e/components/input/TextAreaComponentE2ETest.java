package tech.catheu.jeamlit.e2e.components.input;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.core.Server;
import tech.catheu.jeamlit.e2e.helpers.JeamlitTestHelper;
import tech.catheu.jeamlit.e2e.helpers.OsUtils;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.NOT_HEADLESS;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for TextAreaComponent.
 */
public class TextAreaComponentE2ETest {
    
    @Test
    void testTextAreaInput() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            import tech.catheu.jeamlit.components.input.TextAreaComponent;
            
            public class TestApp {
                public static void main(String[] args) {
                    String text = new TextAreaComponent.Builder("Enter your message")
                        .placeholder("Type here...")
                        .height(150)
                        .build()
                        .use();
                    Jt.text("Message: " + text).use();
                }
            }
            """;
        
        final Path appFile = JeamlitTestHelper.writeTestApp(app);
        Server server = null;

        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(NOT_HEADLESS);
             final Page page = browser.newPage()) {
            server = JeamlitTestHelper.startServer(appFile);
            page.navigate("http://localhost:" + server.port);
            
            // text area input is visible
            assertThat(page.locator("jt-text-area")).isVisible(WAIT_1_SEC_MAX);
            // current message is empty
            final Locator byText = page.getByText("Message: ");
            assertThat(byText).isVisible(WAIT_1_SEC_MAX);
            // Type multi-line text in the textarea
            final Locator textarea = page.locator("jt-text-area textarea");
            textarea.fill("Line 1\nLine 2\nLine 3");
            // Press Cmd/Ctrl+Enter to submit (TextArea's default submit behavior)
            final OsUtils.OS os = OsUtils.getOS();
            page.keyboard().down(os.modifier);
            page.keyboard().press("Enter");
            page.keyboard().up(os.modifier);
            assertThat(page.getByText("Message: Line 1\nLine 2\nLine 3")).isVisible(WAIT_1_SEC_MAX);
            
        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }
}