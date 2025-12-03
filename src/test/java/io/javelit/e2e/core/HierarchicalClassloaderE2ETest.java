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
import static io.javelit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_10_MS_MAX_HIDDEN;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for hierarchical classloader caching behavior.
 * Tests that the classloader correctly caches unchanged classes and handles
 * ClassCastException when cached state references old classloader instances.
 */
public class HierarchicalClassloaderE2ETest {

  @Test
  void testHierarchicalClassloaderCachingAndMismatch(TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-classloader-test-");
    copyResourceDirectory("classloader-test", tempDir);
    final Path appFile = tempDir.resolve("App.java");
    final Path messageFile = tempDir.resolve("Message.java");

    // Step 1: Verify initial state - both Message and Warning should be visible
    // Step 2: Edit App.java comment inline (classloader cache hit for Message.class and App.java)
    // Both should still be visible
    // Step 3: Edit App.java by adding a comment
    // This triggers App reload (including inner Warning class) Message.java should hit cache
    // Should see ClassCastException (Warning from old classloader)
    // Step 4: Click "Clear cache" button to recover
    // After clearing cache, both should be visible again
    // Step 5: Edit Message.java (add a comment on a new line)
    // This reloads Message.class - by hierarchy App and Warning should reload too
    // Should see ClassCastException
    // Message and Warning should NOT be visible (error occurred early)
    // Step 6: Click "Clear cache" button again to recover
    // After clearing cache, both should be visible again
    PlaywrightUtils.runInBrowser(testInfo, appFile, page -> {
      try {
        // Step 1: Verify initial state - both Message and Warning should be visible
        assertThat(page.getByText("Message: hello")).isVisible(WAIT_1_SEC_MAX);
        assertThat(page.getByText("Warning: caution")).isVisible(WAIT_1_SEC_MAX);

        // Step 2: Edit App.java comment inline (classloader cache hit for Message.class and App.java)
        final String originalContent = Files.readString(appFile);
        final String modifiedContent1 = originalContent.replace("// some comment",
                                                                "// some comment something got appended");
        Files.writeString(appFile, modifiedContent1);

        // Both should still be visible
        assertThat(page.getByText("Message: hello")).isVisible(WAIT_1_SEC_MAX);
        assertThat(page.getByText("Warning: caution")).isVisible(WAIT_1_SEC_MAX);

        // Step 3: Edit App.java by adding a comment
        // This triggers App reload (including inner Warning class) Message.java should hit cache
        final String currentContent = Files.readString(appFile);
        final String modifiedContent2 = currentContent.replace("public class App {",
                                                               "public class App {\n// new comment\n");
        Files.writeString(appFile, modifiedContent2);

        // Should see ClassCastException (Warning from old classloader)
        assertThat(page.getByText("Message: hello")).isVisible(WAIT_1_SEC_MAX);
        assertThat(page.getByText("ClassCastException").first()).isVisible(WAIT_1_SEC_MAX);

        // Step 4: Click "Clear cache" button to recover
        var clearCacheButton = page.locator("jt-button").getByText("Clear cache");
        assertThat(clearCacheButton).isVisible(WAIT_1_SEC_MAX);
        clearCacheButton.click(WAIT_1_SEC_MAX_CLICK);

        // After clearing cache, both should be visible again
        assertThat(page.getByText("Message: hello")).isVisible(WAIT_1_SEC_MAX);
        assertThat(page.getByText("Warning: caution")).isVisible(WAIT_1_SEC_MAX);

        // Step 5: Edit Message.java (add a comment on a new line)
        // This reloads Message.class - by hierarchy App and Warning should reload too
        final String messageContent = Files.readString(messageFile);
        final String modifiedMessageContent = messageContent.replace("{\n}", "{\nstatic String NEW = null;\n}");
        Files.writeString(messageFile, modifiedMessageContent);

        // Should see ClassCastException
        assertThat(page.getByText("ClassCastException").first()).isVisible(WAIT_1_SEC_MAX);

        // Message and Warning should NOT be visible (error occurred early)
        assertThat(page.getByText("Message: hello")).isHidden(WAIT_10_MS_MAX_HIDDEN);
        assertThat(page.getByText("Warning: caution")).isHidden(WAIT_10_MS_MAX_HIDDEN);

        // Step 6: Click "Clear cache" button again to recover
        clearCacheButton = page.locator("jt-button").getByText("Clear cache");
        assertThat(clearCacheButton).isVisible(WAIT_1_SEC_MAX);
        clearCacheButton.click(WAIT_1_SEC_MAX_CLICK);

        // After clearing cache, both should be visible again
        assertThat(page.getByText("Message: hello")).isVisible(WAIT_1_SEC_MAX);
        assertThat(page.getByText("Warning: caution")).isVisible(WAIT_1_SEC_MAX);

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
