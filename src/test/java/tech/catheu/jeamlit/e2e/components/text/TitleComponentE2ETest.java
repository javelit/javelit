package tech.catheu.jeamlit.e2e.components.text;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
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

        PlaywrightUtils.runInBrowser(app, page -> {
            // Wait for title to be visible
            assertThat(page.locator("jt-title")).isVisible(WAIT_1_SEC_MAX);
            // Check title is rendered
            assertThat(page.getByText("Main Title")).isVisible(WAIT_1_SEC_MAX);
            // Check content after title
            assertThat(page.getByText("Some content under the title")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}