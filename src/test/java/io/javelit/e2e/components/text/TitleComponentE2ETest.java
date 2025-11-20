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
package io.javelit.e2e.components.text;

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for TitleComponent.
 */
public class TitleComponentE2ETest {

  @Test
  void testTitleDisplay(TestInfo testInfo) {
    JtRunnable app = () -> {
      Jt.title("Main Title").use();
      Jt.text("Some content under the title").use();
    };

    // Wait for title to be visible
    // Verify it renders as h1
    // Check title is rendered
    // Check content after title
    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // Wait for title to be visible
      assertThat(page.locator("jt-title")).isVisible(WAIT_1_SEC_MAX);
      // Verify it renders as h1
      assertThat(page.locator("jt-title h1")).isVisible(WAIT_1_SEC_MAX);
      // Check title is rendered
      assertThat(page.getByText("Main Title")).isVisible(WAIT_1_SEC_MAX);
      // Check content after title
      assertThat(page.getByText("Some content under the title")).isVisible(WAIT_1_SEC_MAX);
    });
  }
}
