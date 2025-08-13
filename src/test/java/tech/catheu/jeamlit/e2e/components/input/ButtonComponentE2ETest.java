package tech.catheu.jeamlit.e2e.components.input;

import com.microsoft.playwright.Locator;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_50_MS_MAX;

/**
 * End-to-end tests for ButtonComponent.
 */
public class ButtonComponentE2ETest {
    
    @Test
    void testButtonClick() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    if (Jt.button("Click Me").use()) {
                        Jt.text("Button was clicked!").use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInBrowser(app, page -> {
            // button exists
            assertThat(page.locator("jt-button")).isVisible(WAIT_1_SEC_MAX);
            // button text is correct
            assertThat(page.getByText("Click Me")).isVisible(WAIT_1_SEC_MAX);
            // "Button was clicked" text is not visible
            assertThat(page.getByText("Button was clicked!")).not().isVisible(WAIT_50_MS_MAX);
            // Click the button
            page.locator("jt-button button").click(new Locator.ClickOptions().setTimeout(100));
            // "Button was clicked" new text is now visible
            assertThat(page.getByText("Button was clicked!")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}