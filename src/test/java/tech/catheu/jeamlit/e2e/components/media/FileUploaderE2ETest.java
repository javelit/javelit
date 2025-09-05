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
package tech.catheu.jeamlit.e2e.components.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.microsoft.playwright.FileChooser;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for FileUploaderComponent.
 */
public class FileUploaderE2ETest {

    @Test
    void testCSVFileUpload() throws IOException {
        // Create a CSV file with known content
        final String csvContent = "name,age,city\nAlice,30,New York\nBob,25,Los Angeles\nCharlie,35,Chicago";
        final Path tempDir = Files.createTempDirectory("fileuploader-test");
        final Path csvFile = tempDir.resolve("test-data.csv");
        Files.writeString(csvFile, csvContent, StandardCharsets.UTF_8);
        
        // Calculate expected file size in bytes
        final int expectedSize = csvContent.getBytes(StandardCharsets.UTF_8).length;

        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            import java.util.Arrays;
            import java.util.List;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.fileUploader("Upload CSV file")
                        .type(List.of(".csv"))
                        .help("Please upload a CSV file")
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInBrowser(app, page -> {
            try {
                // Wait for the file uploader component to be visible
                assertThat(page.locator("jt-file-uploader")).isVisible(WAIT_1_SEC_MAX);
                
                // Wait for and handle the file chooser dialog
                FileChooser fileChooser = page.waitForFileChooser(() -> {
                    // Click the "Browse files" button
                    page.getByText("Browse files").click();
                });
                
                // Set the CSV file to upload
                fileChooser.setFiles(csvFile);
                
                // Wait a moment for the file to be processed
                page.waitForTimeout(500);
                
                // Verify the file name appears correctly
                assertThat(page.locator(".file-name")).containsText("test-data.csv");
                
                // Verify the file size is displayed correctly
                // The size should be shown in bytes with the exact count
                assertThat(page.locator(".file-size")).containsText(String.valueOf(expectedSize));
                
                // Verify the file item container is visible
                assertThat(page.locator(".file-item")).isVisible();
                
            } finally {
                // Clean up the temporary CSV file
                try {
                    Files.deleteIfExists(csvFile);
                    Files.deleteIfExists(tempDir);
                } catch (IOException e) {
                    // Ignore cleanup errors in test
                }
            }
        });
    }
}
