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

import com.microsoft.playwright.Locator;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_10_MS_MAX_HIDDEN;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for the clear cache functionality in the developer options menu.
 */
public class ClearCacheE2ETest {

  @Test
  void testClearCacheButton(TestInfo testInfo) {
    final @Language("java") String app = """
        import io.javelit.core.Jt;
        
        public class TestApp {
            public static void main(String[] args) {
                int res = Jt.cache().computeIfAbsentInt("res", k -> {
                    Jt.text("performing long computation").use();
                    return 5;
                });
                Jt.text("the value was computed").use();
            }
        }
        """;

    // Test 1: On first load, both texts should be visible (cache miss)
    // Test 2: Reload the page - cache should hit
    // Test 3: Click menu and clear cache
    // Verify developer section is visible (we're on localhost)
    // Click "Clear cache" button
    // Test 4: After clearing cache, both texts should be visible again (cache miss)
    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // Test 1: On first load, both texts should be visible (cache miss)
      assertThat(page.getByText("performing long computation")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("the value was computed")).isVisible(WAIT_1_SEC_MAX);

      // Test 2: Reload the page - cache should hit
      page.reload();
      assertThat(page.getByText("the value was computed")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("performing long computation")).isHidden(WAIT_10_MS_MAX_HIDDEN);

      // Test 3: Click menu and clear cache
      Locator menuButton = page.locator(".menu-button");
      assertThat(menuButton).isVisible(WAIT_1_SEC_MAX);
      menuButton.click(WAIT_1_SEC_MAX_CLICK);

      // Verify developer section is visible (we're on localhost)
      assertThat(page.locator(".developer-section")).isVisible(WAIT_1_SEC_MAX);

      // Click "Clear cache" button
      page.locator(".developer-section").getByText("Clear cache").click(WAIT_1_SEC_MAX_CLICK);

      // Test 4: After clearing cache, both texts should be visible again (cache miss)
      assertThat(page.getByText("performing long computation")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("the value was computed")).isVisible(WAIT_1_SEC_MAX);
    });
  }
}
