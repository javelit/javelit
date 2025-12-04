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

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_10_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for classloader mismatch detection and recovery.
 * Tests that the framework correctly detects when cached objects are from old classloaders
 * and provides appropriate error messages and recovery options.
 */
public class ClassloaderMismatchE2ETest {

  @Test
  void testClassloaderMismatchDetectionAndRecovery(TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-classloader-test-");
    final Path mainFile = tempDir.resolve("TestApp.java");

    // Create initial app with Message class
    final String initialApp = """
        import io.javelit.core.Jt;
        
        public class TestApp {
            record Message(String name) {}
        
            public static void main(String[] args) {
                Message message = (Message) Jt
                        .cache()
                        .computeIfAbsent("mess", k -> new Message("Bob"));
                Jt.text("Message: " + message.name).use();
            }
        }
        """;

    Files.writeString(mainFile, initialApp);

    // Verify initial state - should display "Message: Bob"
    // Modify the file to trigger hot reload (add a comment)
    // After reload, should see ClassCastException (search for unique text from developer feedback)
    // visible for any user
    // Should see "Clear cache" button (use getByRole to target the specific button in the error message)
    // only visible in dev mode (which is the case for tests as it's running on localhost)
    // After clearing cache, the app should work again
    PlaywrightUtils.runInBrowser(testInfo, mainFile, page -> {
      try {
        // Verify initial state - should display "Message: Bob"
        assertThat(page.getByText("Message: Bob")).isVisible(WAIT_1_SEC_MAX);

        // Modify the file to trigger hot reload (add a comment)
        final String originalContent = Files.readString(mainFile);
        final String modifiedContent = "//some comment\n" + originalContent;
        Files.writeString(mainFile, modifiedContent);

        // After reload, should see ClassCastException (search for unique text from developer feedback)
        // visible for any user
        assertThat(page.getByText("ClassCastException").first()).isVisible(WAIT_10_SEC_MAX);

        // Should see "Clear cache" button (use getByRole to target the specific button in the error message)
        // only visible in dev mode (which is the case for tests as it's running on localhost)
        var clearCacheButton = page.locator("jt-button").getByText("Clear cache");
        assertThat(clearCacheButton).isVisible(WAIT_1_SEC_MAX);
        clearCacheButton.click(WAIT_1_SEC_MAX_CLICK);

        // After clearing cache, the app should work again
        assertThat(page.getByText("Message: Bob")).isVisible(WAIT_1_SEC_MAX);

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
