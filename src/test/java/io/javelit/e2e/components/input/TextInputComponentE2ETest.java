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

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_FILL;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_PRESS;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_VALUE;

/**
 * End-to-end tests for TextInputComponent.
 */
public class TextInputComponentE2ETest {

  @Test
  void testTextEntry(TestInfo testInfo) {
    JtRunnable app = () -> {
      String name = Jt.textInput("Enter your name").use();
      Jt.text("Hello, " + name + "!").use();
    };

    // text input is visible
    // greating has an empty name
    // Type text in the input
    // Press Enter to submit
    // Wait for update
    // Test clearing and entering new text
    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // text input is visible
      page.waitForSelector("jt-text-input", new Page.WaitForSelectorOptions().setTimeout(5000));
      // greating has an empty name
      assertThat(page.getByText("Hello, !")).isVisible(WAIT_1_SEC_MAX);
      // Type text in the input
      Locator input = page.locator("jt-text-input input");
      input.fill("Cyril", WAIT_1_SEC_MAX_FILL);
      // Press Enter to submit
      input.press("Enter", WAIT_1_SEC_MAX_PRESS);
      // Wait for update
      assertThat(page.getByText("Hello, Cyril!")).isVisible(WAIT_1_SEC_MAX);
      // Test clearing and entering new text
      input.fill("", WAIT_1_SEC_MAX_FILL);
      input.fill("Boss", WAIT_1_SEC_MAX_FILL);
      input.press("Enter", WAIT_1_SEC_MAX_PRESS);
      assertThat(page.getByText("Hello, Boss!")).isVisible(WAIT_1_SEC_MAX);
    });
  }

  @Test
  public void testTextInputWithClearOnEnter(final TestInfo testInfo) {
    JtRunnable app = () -> {
      String text1 = Jt.textInput("text1").clearOnEnter().use();
      if (text1.isBlank()) {
        Jt.text("text1 is blank").use();
      } else {
        Jt.text("text1 is " + text1).use();
      }

      String text2 = Jt.textInput("text2").clearOnEnter().use();
      if (text2.isBlank()) {
        Jt.text("text2 is blank").use();
      } else {
        Jt.text("text2 is " + text2).use();
      }

      String text3 = Jt.textInput("text without autoclear").use();
      if (text3.isBlank()) {
        Jt.text("text without autoclear is blank").use();
      } else {
        Jt.text("text without autoclear is " + text3).use();
      }
    };

    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // Wait for all text inputs to be visible
      page.waitForSelector("jt-text-input", new Page.WaitForSelectorOptions().setTimeout(5000));

      // Get locators for the three text inputs
      final Locator input1 = page.locator("jt-text-input").nth(0).locator("input");
      final Locator input2 = page.locator("jt-text-input").nth(1).locator("input");
      final Locator input3 = page.locator("jt-text-input").nth(2).locator("input");

      // Initial state - all should show "blank"
      assertThat(page.getByText("text1 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text2 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text without autoclear is blank")).isVisible(WAIT_1_SEC_MAX);

      // - enter "hi" in the first textInput, press enter
      input1.fill("hi", WAIT_1_SEC_MAX_FILL);
      input1.press("Enter", WAIT_1_SEC_MAX_PRESS);
      //  - ensure "text1 is hi" text is found
      //  - ensure "text2 is blank" test is found
      //  - ensure "text without autoclear is blank" is found
      assertThat(page.getByText("text1 is hi")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text2 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text without autoclear is blank")).isVisible(WAIT_1_SEC_MAX);
      //  - ensure the 3 textInput box are empty
      assertThat(input1).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input2).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input3).hasValue("", WAIT_1_SEC_MAX_VALUE);
      //  - enter "hey" in the second textInput, press enter
      input2.fill("hey", WAIT_1_SEC_MAX_FILL);
      input2.press("Enter", WAIT_1_SEC_MAX_PRESS);
      //  - ensure "text1 is blank" text is found
      //  - ensure "text2 is hey" text is found
      //  - ensure "text without autoclear is blank" is found
      assertThat(page.getByText("text1 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text2 is hey")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text without autoclear is blank")).isVisible(WAIT_1_SEC_MAX);
      //  - ensure the 3 textInput box are empty
      assertThat(input1).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input2).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input3).hasValue("", WAIT_1_SEC_MAX_VALUE);
      //  - enter hello in the third textInput, press enter
      // Step 3: Enter "hello" in the third textInput and press enter
      input3.fill("hello", WAIT_1_SEC_MAX_FILL);
      input3.press("Enter", WAIT_1_SEC_MAX_PRESS);
      //  - ensure "text1 is blank" is found
      //  - ensure "text2 is blank" is found
      //  - ensure "text without autoclear is hello" is found
      assertThat(page.getByText("text1 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text2 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text without autoclear is hello")).isVisible(WAIT_1_SEC_MAX);
      //  - ensure the 2 first input box are empty
      //  - ensure the third input box contains "hello"
      assertThat(input1).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input2).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input3).hasValue("hello");
      //  - enter "hi" in the first input box, press enter
      input1.fill("hi", WAIT_1_SEC_MAX_FILL);
      input1.press("Enter", WAIT_1_SEC_MAX_PRESS);
      //  - ensure "text1 is hi" is found
      //  - ensure "text2 is blank" is found
      //  - ensure "text without autoclear is hello" is found
      assertThat(page.getByText("text1 is hi")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text2 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text without autoclear is hello")).isVisible(WAIT_1_SEC_MAX);
      //  - ensure the 2 first input box are empty
      //  - ensure the third input box contains "hello"
      assertThat(input1).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input2).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input3).hasValue("hello");
      //  - enter "hi" in the first input box, enter "hey" in the second input box (ENTER is not pressed)
      input1.fill("hi", WAIT_1_SEC_MAX_FILL);
      input2.fill("hey", WAIT_1_SEC_MAX_FILL);
      //  - click outside the second input box (blur)
      page.locator("body").click();
      //  - ensure "text1 is hi" is found
      //  - ensure "text2 is blank" is found
      //  - ensure "text without autoclear is hello" is found
      assertThat(page.getByText("text1 is hi")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text2 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text without autoclear is hello")).isVisible(WAIT_1_SEC_MAX);
      //  - click in second textInput
      //  - press enter
      input2.click();
      input2.press("Enter", WAIT_1_SEC_MAX_PRESS);
      //  - ensure "text1 is blank" is found
      //  - ensure first text input box has value "hi"
      //  - ensure "text2 is hey" is found
      //  - ensure second text input box is blank
      //  - ensure "text without autoclear is hello" is found
      //  - ensure the third input box contains "hello"
      assertThat(page.getByText("text1 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(input1).hasValue("hi");
      assertThat(page.getByText("text2 is hey")).isVisible(WAIT_1_SEC_MAX);
      assertThat(input2).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(page.getByText("text without autoclear is hello")).isVisible(WAIT_1_SEC_MAX);
      assertThat(input3).hasValue("hello");
      //  - enter "hey" in second text input
      //  - click on first text input
      //  - press enter
      input2.fill("hey", WAIT_1_SEC_MAX_FILL);
      input1.click();
      input1.press("Enter", WAIT_1_SEC_MAX_PRESS);
      //  - ensure "text1 is hi" is found
      //  - ensure "text2 is blank" is found
      // Verify text displays
      assertThat(page.getByText("text1 is hi")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("text2 is blank")).isVisible(WAIT_1_SEC_MAX);
      assertThat(input1).hasValue("", WAIT_1_SEC_MAX_VALUE);
      assertThat(input2).hasValue("", WAIT_1_SEC_MAX_VALUE);
    });
  }
}
