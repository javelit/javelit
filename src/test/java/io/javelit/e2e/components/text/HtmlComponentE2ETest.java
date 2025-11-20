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
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_10_MS_MAX_HIDDEN;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT;


public class HtmlComponentE2ETest {

  @Test
  void testHtmlVariations(TestInfo testInfo) {
    JtRunnable app = () -> {
      // Test 1: Simple HTML string
      Jt.html("<h2>Hello HTML</h2><p>This is a <strong>test</strong>!</p>").use();

      // Test 2: With width
      Jt.html("<div>Content with width</div>").width(400).use();

      // Test 3: Script sanitization
      Jt.html("<p>Safe content</p><script>alert('dangerous');</script>").use();
    };

    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // Verify all 3 HTML components are rendered
      assertThat(page.locator("jt-html").nth(0)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("jt-html").nth(1)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("jt-html").nth(2)).isVisible(WAIT_1_SEC_MAX);

      // Test 1: Simple HTML string
      assertThat(page.locator("jt-html").nth(0).locator("h2")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("jt-html").nth(0).locator("h2")).hasText("Hello HTML", WAIT_1_SEC_MAX_TEXT);
      assertThat(page.locator("jt-html").nth(0).locator("p")).hasText("This is a test!", WAIT_1_SEC_MAX_TEXT);
      assertThat(page.locator("jt-html").nth(0).locator("strong")).hasText("test", WAIT_1_SEC_MAX_TEXT);

      // Test 2: With width
      assertThat(page.locator("jt-html").nth(1)).hasAttribute("width", "400", WAIT_1_SEC_MAX_ATTRIBUTE);

      // Test 3: Script sanitization
      assertThat(page.locator("jt-html").nth(2).locator("p")).hasText("Safe content", WAIT_1_SEC_MAX_TEXT);
      assertThat(page.locator("jt-html").nth(2).locator("script")).isHidden(WAIT_10_MS_MAX_HIDDEN);
    });
  }
}
