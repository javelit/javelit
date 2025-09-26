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
package io.jeamlit.e2e.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.jeamlit.e2e.helpers.JeamlitTestHelper;
import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for compilation error handling.
 */
public class CompilationErrorE2ETest {

    @Test
    void testCompilationErrorModalAppears(TestInfo testInfo) {
        // App with compilation error (missing semicolon)
        final @Language("java") String invalidApp = """
                import io.jeamlit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        Jt.title("Test App").use()  // Missing semicolon - COMPILATION ERROR
                        Jt.text("This app has an error.").use();
                    }
                }
                """;

        PlaywrightUtils.runInSharedBrowser(testInfo, invalidApp, page ->
                // Verify app loads correctly first
                assertThat(page.getByText("';' expected")).isVisible(WAIT_1_SEC_MAX));
    }

    @Test
    void testFileDeletionErrorMessage(TestInfo testInfo) {
        // Create a valid app
        final @Language("java") String validApp = """
                import io.jeamlit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        Jt.title("Test App").use();
                        Jt.text("This app is running.").use();
                    }
                }
                """;

        final Path appFile = JeamlitTestHelper.writeTestApp(validApp);

        PlaywrightUtils.runInSharedBrowser(testInfo, appFile, page -> {
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
}
