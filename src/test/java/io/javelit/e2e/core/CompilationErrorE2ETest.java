/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javelit.e2e.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.javelit.e2e.helpers.JavelitTestHelper;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_HIDDEN;

/**
 * End-to-end tests for compilation error handling.
 */
public class CompilationErrorE2ETest {

    @Test
    void testCompilationErrorModalAppears(TestInfo testInfo) {
        // App with compilation error (missing semicolon)
        final @Language("java") String invalidApp = """
                import io.javelit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        Jt.title("Test App").use()  // Missing semicolon - COMPILATION ERROR
                        Jt.text("This app has an error.").use();
                    }
                }
                """;

        // Verify app loads correctly first
        PlaywrightUtils.runInBrowser(testInfo, invalidApp, page ->
                // Verify app loads correctly first
                assertThat(page.getByText("';' expected")).isVisible(WAIT_1_SEC_MAX));
    }

    @Test
    void testFileDeletionErrorMessage(TestInfo testInfo) {
        // Create a valid app
        final @Language("java") String validApp = """
                import io.javelit.core.Jt;

                public class TestApp {
                    public static void main(String[] args) {
                        Jt.title("Test App").use();
                        Jt.text("This app is running.").use();
                    }
                }
                """;

        final Path appFile = JavelitTestHelper.writeTestApp(validApp);

        // Verify app loads correctly first
        // Delete the app file
        // Verify error message appears
        PlaywrightUtils.runInBrowser(testInfo, appFile, page -> {
            // Verify app loads correctly first
            assertThat(page.getByText("Test App")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("This app is running.")).isVisible();

            // Delete the app file
            try {
                Files.delete(appFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Verify error message appears
            assertThat(page.getByText("App file was deleted.")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testCompilationErrorWithReloadAndRecovery(TestInfo testInfo) {
        // Create a valid app
        final @Language("java") String validApp = """
                import io.javelit.core.Jt;

                public class TestApp {
                    public static void main(String[] args) {
                        Jt.title("Test App").use();
                    }
                }
                """;

        final @Language("java") String appWithError = """
                    import io.javelit.core.Jt;

                    public class TestApp {
                        public static void main(String[] args) {
                            Jt.title("Test App").use()
                        }
                    }
                    """;

        final Path appFile = JavelitTestHelper.writeTestApp(validApp);

        // Step 1: Verify app loads correctly
        // Step 2: Introduce compilation error by removing semicolon
        // Step 3: Verify error message appears
        // Step 4: Reload the page
        // Step 5: Verify error message persists after reload
        // Step 6: Fix the error by adding semicolon back
        // Step 7: Verify error disappears and app recovers
        PlaywrightUtils.runInBrowser(testInfo, appFile, page -> {
            // Step 1: Verify app loads correctly
            assertThat(page.getByText("Test App")).isVisible(WAIT_1_SEC_MAX);

            // Step 2: Introduce compilation error by removing semicolon
            try {
                Files.writeString(appFile, appWithError);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Step 3: Verify error message appears
            assertThat(page.getByText("';' expected")).isVisible(WAIT_1_SEC_MAX);

            // Step 4: Reload the page
            page.reload();

            // Step 5: Verify error message persists after reload
            assertThat(page.getByText("';' expected")).isVisible(WAIT_1_SEC_MAX);

            // Step 6: Fix the error by adding semicolon back
            try {
                Files.writeString(appFile, validApp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Step 7: Verify error disappears and app recovers
            assertThat(page.getByText("';' expected")).isHidden(WAIT_1_SEC_MAX_HIDDEN);
            assertThat(page.getByText("Test App")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
