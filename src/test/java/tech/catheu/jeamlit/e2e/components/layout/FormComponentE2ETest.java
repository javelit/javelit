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
 * End-to-end tests for FormComponent and FormSubmitButtonComponent.
 */
public class FormComponentE2ETest {
    
    @Test
    void testFormSubmission() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            import tech.catheu.jeamlit.components.layout.FormComponent;
            import tech.catheu.jeamlit.core.JtContainer;
            
            public class TestApp {
                public static void main(String[] args) {
                    JtContainer formContainer = Jt.form("test-form").use();
                    Jt.text("used to get out of form").use();
                    
                    // Add form inputs
                    String name = Jt.textInput("Your Name").value("NOT_SET").use(formContainer);
                    String email = Jt.textInput("Your Email").value("NOT_SET").use(formContainer);
                    
                    // Add submit button
                    boolean submitted = Jt.formSubmitButton("Submit Form").use(formContainer);
                    
                    Jt.text("Name: " + name + ", Email: " + email).use();
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

            // used to get out of inputs easily
            final Locator textUtilLocator = page.locator("jt-text",  new Page.LocatorOptions().setHasText("used to get out of form"));
            assertThat(textUtilLocator).isVisible(WAIT_1_SEC_MAX);

            assertThat(page.locator("jt-form")).isVisible(WAIT_1_SEC_MAX);
            // Fill name form input
            Locator nameInput = page.locator("jt-text-input[label='Your Name'] input");
            nameInput.fill("John");
            textUtilLocator.click();
            // ensure the change did not apply yet because the form was not submitted
            assertThat(page.getByText("Name: " + "NOT_SET" + ", Email: " + "NOT_SET")).isVisible(WAIT_1_SEC_MAX);
            // Fill email form input
            Locator emailInput = page.locator("jt-text-input[label='Your Email'] input");
            emailInput.fill("john@example.com");
            textUtilLocator.click();
            assertThat(page.getByText("Name: " + "NOT_SET" + ", Email: " + "NOT_SET")).isVisible(WAIT_1_SEC_MAX);
            
            // Click submit button
            page.locator("jt-form-submit-button button").click();
            assertThat(page.getByText("Name: " + "John" + ", Email: " + "john@example.com")).isVisible(WAIT_1_SEC_MAX);
        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
    }
}