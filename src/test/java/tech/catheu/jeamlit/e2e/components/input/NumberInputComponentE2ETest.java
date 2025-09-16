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
package tech.catheu.jeamlit.e2e.components.input;

import com.microsoft.playwright.Locator;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for NumberInputComponent.
 */
public class NumberInputComponentE2ETest {
    
    @Test
    void testDirectInput(TestInfo testInfo) {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    final Number value = Jt.numberInput("Test Input").use();
                    Jt.text("Value: " + value).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {

            // number input exists
            assertThat(page.locator("jt-number-input")).isVisible(WAIT_1_SEC_MAX);
            // number input text is correct
            assertThat(page.getByText("Test Input")).isVisible(WAIT_1_SEC_MAX);
            // Enter a value in the number input
            final Locator input = page.locator("jt-number-input input");
            input.fill("42", new Locator.FillOptions().setTimeout(100));
            // Press Enter to submit
            input.press("Enter", new Locator.PressOptions().setTimeout(100));
            // Verify the value is displayed
            assertThat(page.getByText("Value: 42")).isVisible(WAIT_1_SEC_MAX);
        });
    }
    
    @Test
    void testStepButtons(TestInfo testInfo) {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    Integer value = Jt.numberInput("Counter", Integer.class)
                        .value(5)
                        .step(1)
                        .use();
                    Jt.text("Count: " + value).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {

            // button exists
            assertThat(page.locator("jt-number-input")).isVisible(WAIT_1_SEC_MAX);
            // Initial value should be 5
            assertThat(page.getByText("Count: 5")).isVisible(WAIT_1_SEC_MAX);
            // Click the + button --> value should increment
            final Locator plusButton = page.locator("jt-number-input .step-up");
            plusButton.click();
            assertThat(page.getByText("Count: 6")).isVisible(WAIT_1_SEC_MAX);
            // Click the - button twice - value should decrement
            final Locator minusButton = page.locator("jt-number-input .step-down");
            minusButton.click(new Locator.ClickOptions().setClickCount(2));
            assertThat(page.getByText("Count: 4")).isVisible(WAIT_1_SEC_MAX);

        });
    }
}
