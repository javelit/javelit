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
package io.javelit.e2e.components.input;

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_50_MS_MAX;

/**
 * End-to-end tests for ToggleComponent.
 */
public class CheckboxComponentE2ETest {

  @Test
  void testSimpleToggleClick(TestInfo testInfo) {
    JtRunnable app = () -> {
      boolean toggle = Jt.checkbox("Enable notifications").use();
      if (toggle) {
        Jt.text("Notifications are **enabled**").use();
      } else {
        Jt.text("Notifications are **disabled**").use();
      }
    };

    // Toggle component exists
    // Initial state shows "disabled" message
    // "Enabled" message is not visible initially
    // Wait for toggle input to be visible and click it
    // "Enabled" message is now visible
    // "Disabled" message is no longer visible
    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // Toggle component exists
      assertThat(page.locator("jt-checkbox")).isVisible(WAIT_1_SEC_MAX);
      // Initial state shows "disabled" message
      assertThat(page.getByText("Notifications are **disabled**")).isVisible(WAIT_1_SEC_MAX);
      // "Enabled" message is not visible initially
      assertThat(page.getByText("Notifications are **enabled**")).not().isVisible(WAIT_50_MS_MAX);
      // Wait for toggle input to be visible and click it
      page.locator("jt-checkbox .checkbox-visual").click(WAIT_1_SEC_MAX_CLICK);
      // "Enabled" message is now visible
      assertThat(page.getByText("Notifications are **enabled**")).isVisible(WAIT_1_SEC_MAX);
      // "Disabled" message is no longer visible
      assertThat(page.getByText("Notifications are **disabled**")).not().isVisible(WAIT_50_MS_MAX);
    });
  }

  @Test
  void testToggleWithDefaultValueTrue(TestInfo testInfo) {
    JtRunnable app = () -> {
      boolean toggle = Jt.checkbox("Auto-save").value(true).use();
      if (toggle) {
        Jt.text("Notifications are **enabled**").use();
      } else {
        Jt.text("Notifications are **disabled**").use();
      }
    };

    // Toggle component exists
    // Initial state shows "enabled" message
    // "disabled" message is not visible initially
    // Wait for toggle input to be visible and click it
    // "Disabled" message is now visible
    // "Enabled" message is no longer visible
    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // Toggle component exists
      assertThat(page.locator("jt-checkbox")).isVisible(WAIT_1_SEC_MAX);
      // Initial state shows "enabled" message
      assertThat(page.getByText("Notifications are **enabled**")).isVisible(WAIT_1_SEC_MAX);
      // "disabled" message is not visible initially
      assertThat(page.getByText("Notifications are **disabled**")).not().isVisible(WAIT_50_MS_MAX);
      // Wait for toggle input to be visible and click it
      page.locator("jt-checkbox .checkbox-visual").click(WAIT_1_SEC_MAX_CLICK);
      // "Disabled" message is now visible
      assertThat(page.getByText("Notifications are **disabled**")).isVisible(WAIT_1_SEC_MAX);
      // "Enabled" message is no longer visible
      assertThat(page.getByText("Notifications are **enabled**")).not().isVisible(WAIT_50_MS_MAX);
    });
  }
}
