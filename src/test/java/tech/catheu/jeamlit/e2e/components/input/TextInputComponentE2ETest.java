package tech.catheu.jeamlit.e2e.components.input;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for TextInputComponent.
 */
public class TextInputComponentE2ETest {
    
    @Test
    void testTextEntry() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    String name = Jt.textInput("Enter your name").use();
                    Jt.text("Hello, " + name + "!").use();
                }
            }
            """;

        PlaywrightUtils.runInBrowser(app, page -> {
            // text input is visible
            page.waitForSelector("jt-text-input", new Page.WaitForSelectorOptions().setTimeout(5000));
            // greating has an empty name
            assertThat(page.getByText("Hello, !")).isVisible(WAIT_1_SEC_MAX);
            // Type text in the input
            Locator input = page.locator("jt-text-input input");
            input.fill("Cyril");
            // Press Enter to submit
            input.press("Enter");
            // Wait for update
            assertThat(page.getByText("Hello, Cyril!")).isVisible(WAIT_1_SEC_MAX);
            // Test clearing and entering new text
            input.fill("");
            input.fill("Boss");
            input.press("Enter");
            assertThat(page.getByText("Hello, Boss!")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}