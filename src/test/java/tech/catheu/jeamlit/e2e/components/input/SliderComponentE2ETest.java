package tech.catheu.jeamlit.e2e.components.input;

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
 * End-to-end tests for SliderComponent.
 */
public class SliderComponentE2ETest {

    @Test
    void testSliderDrag() {
        final @Language("java") String app = """
                import tech.catheu.jeamlit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        double value = Jt.slider("Temperature")
                            .min(0)
                            .max(100)
                            .value(50)
                            .use();
                        Jt.text("Temperature: " + value).use();
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

            // slider exists
            assertThat(page.locator("jt-slider")).isVisible(WAIT_1_SEC_MAX);
            // intial value is 50
            assertThat(page.getByText("Temperature: " + 50)).isVisible(WAIT_1_SEC_MAX);
            // change the slider value to 25
            final Locator sliderInput = page.locator("jt-slider .slider-input");
            sliderInput.fill("25");
            // text is updated
            assertThat(page.getByText("Temperature: " + 25)).isVisible(WAIT_1_SEC_MAX);

        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }
}