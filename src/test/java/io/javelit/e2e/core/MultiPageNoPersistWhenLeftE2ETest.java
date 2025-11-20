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
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_10_MS_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for multipage apps with noPersistWhenLeft functionality.
 */
public class MultiPageNoPersistWhenLeftE2ETest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testNoPersistWhenLeftBehavior(final boolean proxied, final TestInfo testInfo) {
    JtRunnable app = () -> {
      var currentPage = Jt.navigation(
          Jt.page("/page-1", MultiPageNoPersistWhenLeftE2ETest::page1).home().noPersistWhenLeft(),
          Jt.page("/page-2", MultiPageNoPersistWhenLeftE2ETest::page2)
      ).use();

      currentPage.run();

      Jt.textInput("shared text input").use();
    };

    // Wait for page to load - should be on home page (Page1)
    // Fill the shared text input with "forever here"
    // Fill the page 1 text input with "should not be here for long"
    // Click on page2
    // Ensure page 2 text input is here
    // Ensure page 1 text input is not here
    // Ensure "forever here" text value in shared input is still here
    // Fill the "page 2 text input" with value: "should stay here"
    // Click on page 1
    // Ensure "page 1 text input" is here and its value is empty string
    // Click on page 2
    // Ensure "page 2 text input" is here and its value is "should stay here"
    PlaywrightUtils.runInBrowser(testInfo, app, true, proxied, page -> {
      // Wait for page to load - should be on home page (Page1)
      Locator sharedInput = page
          .locator("jt-text-input")
          .filter(new Locator.FilterOptions().setHasText("shared text input"))
          .locator("input");
      assertThat(sharedInput).isVisible(WAIT_1_SEC_MAX);

      Locator page1Input = page
          .locator("jt-text-input")
          .filter(new Locator.FilterOptions().setHasText("page 1 text input"))
          .locator("input");
      assertThat(page1Input).isVisible(WAIT_1_SEC_MAX);

      // Fill the shared text input with "forever here"
      sharedInput.fill("forever here");
      sharedInput.press("Enter");

      // Fill the page 1 text input with "should not be here for long"
      page1Input.fill("should not be here for long");
      page1Input.press("Enter");

      // Click on page2
      page
          .locator("jt-navigation a")
          .filter(new Locator.FilterOptions().setHasText("Page 2"))
          .click(WAIT_1_SEC_MAX_CLICK);

      // Ensure page 2 text input is here
      Locator page2Input = page
          .locator("jt-text-input")
          .filter(new Locator.FilterOptions().setHasText("page 2 text input"))
          .locator("input");
      assertThat(page2Input).isVisible(WAIT_1_SEC_MAX);

      // Ensure page 1 text input is not here
      assertThat(page.locator("jt-text-input").filter(new Locator.FilterOptions().setHasText("page 1 text input")))
          .not()
          .isVisible(WAIT_10_MS_MAX);

      // Ensure "forever here" text value in shared input is still here
      Locator sharedInputOnPage2 = page
          .locator("jt-text-input")
          .filter(new Locator.FilterOptions().setHasText("shared text input"))
          .locator("input");
      assertEquals("forever here", sharedInputOnPage2.inputValue());

      // Fill the "page 2 text input" with value: "should stay here"
      page2Input.fill("should stay here");
      page2Input.press("Enter");

      // Click on page 1
      page
          .locator("jt-navigation a")
          .filter(new Locator.FilterOptions().setHasText("Page 1"))
          .click(WAIT_1_SEC_MAX_CLICK);

      // Ensure "page 1 text input" is here and its value is empty string
      Locator page1InputAgain = page
          .locator("jt-text-input")
          .filter(new Locator.FilterOptions().setHasText("page 1 text input"))
          .locator("input");
      assertThat(page1InputAgain).isVisible(WAIT_1_SEC_MAX);
      assertEquals("", page1InputAgain.inputValue(),
                   "Page1 with noPersistWhenLeft should have cleared its state");

      // Click on page 2
      page
          .locator("jt-navigation a")
          .filter(new Locator.FilterOptions().setHasText("Page 2"))
          .click(WAIT_1_SEC_MAX_CLICK);

      // Ensure "page 2 text input" is here and its value is "should stay here"
      Locator page2InputAgain = page
          .locator("jt-text-input")
          .filter(new Locator.FilterOptions().setHasText("page 2 text input"))
          .locator("input");
      assertThat(page2InputAgain).isVisible(WAIT_1_SEC_MAX);
      assertEquals("should stay here", page2InputAgain.inputValue(),
                   "Page2 without noPersistWhenLeft should persist its state");
    });
  }

  private static void page1() {
    Jt.textInput("page 1 text input").key("key1").use();
  }

  private static void page2() {
    Jt.textInput("page 2 text input").key("key1").use();
  }
}
