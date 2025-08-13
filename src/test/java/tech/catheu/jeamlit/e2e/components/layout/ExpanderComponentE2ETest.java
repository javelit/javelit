package tech.catheu.jeamlit.e2e.components.layout;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_50_MS_MAX;

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

        PlaywrightUtils.runInBrowser(app, page -> {
            // Wait for expander to be visible
            final Locator expanderLocator = page.locator("jt-expander");
            assertThat(expanderLocator).isVisible(WAIT_1_SEC_MAX);
            // Check expander header is visible
            assertThat(page.locator("jt-expander summary", new Page.LocatorOptions().setHasText("Click to expand"))).isVisible(WAIT_1_SEC_MAX);
            // Initially, content should be hidden (collapsed)
            assertThat(page.getByText("Hidden content inside expander")).not().isVisible(WAIT_50_MS_MAX);
            // Click to expand
            expanderLocator.click();
            // Check that content is now visible
            assertThat(page.getByText("Hidden content inside expander")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Hidden Button")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}