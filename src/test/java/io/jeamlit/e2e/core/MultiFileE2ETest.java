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

import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for multi-file compilation support.
 * Tests that the compiler correctly handles dependencies in subdirectories.
 */
public class MultiFileE2ETest {

    @Test
    void testMultiFileCompilationWithSubdirectoryDependencies(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("jeamlit-multifile-test-");
        copyResourceDirectory("multifile-test", tempDir);
        final Path mainFile = tempDir.resolve("Test.java");
        PlaywrightUtils.runInSharedBrowser(testInfo, mainFile, page -> {
            // Verify that Car.BLUE is rendered (from model/Car.java)
            assertThat(page.getByText("BLUE")).isVisible(WAIT_1_SEC_MAX);
            // Verify that Owner.BOSS is rendered (from model/Owner.java)
            assertThat(page.getByText("BOSS")).isVisible(WAIT_1_SEC_MAX);
        });
    }
    
    @Test
    void testMultiFileHotReloadWithMainFileChange(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("jeamlit-multifile-reload-test-");
        copyResourceDirectory("multifile-test", tempDir);
        final Path mainFile = tempDir.resolve("Test.java");
        
        PlaywrightUtils.runInSharedBrowser(testInfo, mainFile, page -> {
            try {
                // Verify initial state - Car.BLUE is displayed
                assertThat(page.getByText("BLUE")).isVisible(WAIT_1_SEC_MAX);
                final String originalContent = Files.readString(mainFile);
                final String modifiedContent = originalContent.replace("Car.BLUE", "Car.RED");
                Files.writeString(mainFile, modifiedContent);
                // Verify the change is reflected - RED should now be displayed
                assertThat(page.getByText("RED")).isVisible(WAIT_1_SEC_MAX);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Test
    void testMultiFileHotReloadWithDependencyFileChange(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("jeamlit-multifile-dependency-reload-test-");
        copyResourceDirectory("multifile-test", tempDir);
        final Path mainFile = tempDir.resolve("Test.java");
        final Path carFile = tempDir.resolve("model/Car.java");
        
        PlaywrightUtils.runInSharedBrowser(testInfo, mainFile, page -> {
            try {
                // Verify initial state - Car.BLUE is displayed
                assertThat(page.getByText("BLUE")).isVisible(WAIT_1_SEC_MAX);
                
                // Modify the dependency file to change BLUE to GREEN
                final String originalCarContent = Files.readString(carFile);
                final String modifiedCarContent = originalCarContent.replace("BLUE", "GREEN");
                Files.writeString(carFile, modifiedCarContent);
                
                // Also update the main file to use GREEN instead of BLUE
                final String originalMainContent = Files.readString(mainFile);
                final String modifiedMainContent = originalMainContent.replace("Car.BLUE", "Car.GREEN");
                Files.writeString(mainFile, modifiedMainContent);
                
                // Verify the change is reflected - GREEN should now be displayed
                assertThat(page.getByText("GREEN")).isVisible(WAIT_1_SEC_MAX);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
