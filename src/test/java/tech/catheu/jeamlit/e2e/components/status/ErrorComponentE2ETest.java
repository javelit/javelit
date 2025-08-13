package tech.catheu.jeamlit.e2e.components.status;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

public class ErrorComponentE2ETest {

    @Test
    void testErrorDisplayOnException() {
        final @Language("java") String app = """
                import tech.catheu.jeamlit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        throw new RuntimeException("Something went wrong");
                    }
                }
                """;

        PlaywrightUtils.runInBrowser(app, page -> {
            assertThat(page.locator("jt-error")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testErrorDisplay() {
        final @Language("java") String app = """
                import tech.catheu.jeamlit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        Jt.error("User generated error").use();
                    }
                }
                """;

        PlaywrightUtils.runInBrowser(app, page -> {
            assertThat(page.locator("jt-error")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
