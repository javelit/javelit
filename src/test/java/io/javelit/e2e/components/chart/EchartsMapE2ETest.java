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
package io.javelit.e2e.components.chart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for EchartsComponent map functionality.
 */
public class EchartsMapE2ETest {

  @Test
  void testEchartsMap_BasicMapRegistration(TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-map-test-");
    copyResourceDirectory("map-test", tempDir);
    final Path mainFile = tempDir.resolve("MapTestApp.java");

    PlaywrightUtils.runInBrowser(testInfo, mainFile, page -> {
      // Wait for ECharts component to be visible - test the first chart (basic map)
      assertThat(page.locator("jt-echarts").first()).isVisible(WAIT_1_SEC_MAX);

      // Verify the first component has maps attribute
      assertThat(page.locator("jt-echarts").first()).hasAttribute("maps", Pattern.compile(".{10,}"));

      // Verify the chart container exists
      assertThat(page.locator("jt-echarts").first().locator("#container")).isVisible(WAIT_1_SEC_MAX);

      // Wait a bit for map to potentially load and check for console errors
      page.waitForTimeout(2000);

      // Verify no critical JavaScript errors (this would throw if there were errors)
      page.evaluate(
          "() => { if (window.jsErrors && window.jsErrors.length > 0) throw new Error('JS errors: ' + window.jsErrors.join(', ')); }");
    });
  }

  @Test
  void testEchartsMap_WithSpecialAreas(TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-map-test-");
    copyResourceDirectory("map-test", tempDir);
    final Path mainFile = tempDir.resolve("MapTestApp.java");

    PlaywrightUtils.runInBrowser(testInfo, mainFile, page -> {
      // Wait for ECharts component to be visible - there are two charts, check for both
      assertThat(page.locator("jt-echarts").first()).isVisible(WAIT_1_SEC_MAX);

      // Verify the second chart (with special areas) exists
      assertThat(page.locator("jt-echarts").nth(1)).isVisible(WAIT_1_SEC_MAX);

      // Verify the second component has maps attribute with special areas
      assertThat(page.locator("jt-echarts").nth(1)).hasAttribute("maps", Pattern.compile("specialAreas"));

      // Verify the chart container exists for the second chart
      assertThat(page.locator("jt-echarts").nth(1).locator("#container")).isVisible(WAIT_1_SEC_MAX);
    });
  }
}
