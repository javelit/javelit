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
import com.microsoft.playwright.assertions.LocatorAssertions;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_10_MS_MAX_HIDDEN;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for component state persistence behavior.
 * Tests that components with user-provided keys persist state, while components without keys
 * or with noPersist() do not persist when they're removed and re-rendered.
 */
public class NoRenderStatePersistenceE2ETest {

    @Test
    void testStatePersistenceBasedOnUserKey(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    var view = Jt.radio("View", List.of("view1", "view2")).use();
                    if ("view1".equals(view)) {
                        Jt.textInput("Persisted text").key("text1").use();
                        Jt.textInput("Not persisted text because no user key").use();
                        Jt.textInput("Not persisted text because noPersist").key("text3").noPersist().use();
                        Jt.text("☝️ Enter some text, then click on view2 above").use();
                    } else if ("view2".equals(view)) {
                        Jt.text("☝️ Now go back to view1 and see if your text is still there").use();
                    }
                }
            }
            """;

        // Wait for page to load - view1 should be selected by default
        // Find the three text inputs using class selectors and nth()
        // Verify all three inputs are visible
        // Enter distinct text in each input
        // Click on "view2" radio button (second option)
        // Verify that the text inputs are no longer visible
        // Click back on "view1" radio button (first option)
        // Wait for inputs to reappear
        // Re-locate the inputs (they were re-rendered)
        // Verify state persistence behavior:
        // 1. First input (with .key("text1")) should still have its value
        // 2. Second input (no .key()) should be empty
        // 3. Third input (with .noPersist()) should be empty
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Wait for page to load - view1 should be selected by default
            assertThat(page.getByText("☝️ Enter some text, then click on view2 above")).isVisible(WAIT_1_SEC_MAX);

            // Find the three text inputs using class selectors and nth()
            final Locator persistedInput = page.locator("jt-text-input").nth(0).locator("input");
            final Locator noKeyInput = page.locator("jt-text-input").nth(1).locator("input");
            final Locator noPersistInput = page.locator("jt-text-input").nth(2).locator("input");

            // Verify all three inputs are visible
            assertThat(persistedInput).isVisible(WAIT_1_SEC_MAX);
            assertThat(noKeyInput).isVisible(WAIT_1_SEC_MAX);
            assertThat(noPersistInput).isVisible(WAIT_1_SEC_MAX);

            // Enter distinct text in each input
            persistedInput.fill("value1");
            noKeyInput.fill("value2");
            noPersistInput.fill("value3");

            // Click on "view2" radio button (second option)
            page.locator("jt-radio .radio-option:nth-child(2) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);

            // Verify that the text inputs are no longer visible
            assertThat(page.getByText("☝️ Now go back to view1 and see if your text is still there")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-text-input").nth(0)).isHidden(WAIT_10_MS_MAX_HIDDEN);
            assertThat(page.locator("jt-text-input").nth(1)).isHidden(WAIT_10_MS_MAX_HIDDEN);
            assertThat(page.locator("jt-text-input").nth(2)).isHidden(WAIT_10_MS_MAX_HIDDEN);

            // Click back on "view1" radio button (first option)
            page.locator("jt-radio .radio-option:nth-child(1) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);

            // Wait for inputs to reappear
            assertThat(page.getByText("☝️ Enter some text, then click on view2 above")).isVisible(WAIT_1_SEC_MAX);

            // Re-locate the inputs (they were re-rendered)
            Locator persistedInputAfter = page.locator("jt-text-input").nth(0).locator("input");
            Locator noKeyInputAfter = page.locator("jt-text-input").nth(1).locator("input");
            Locator noPersistInputAfter = page.locator("jt-text-input").nth(2).locator("input");

            // Verify state persistence behavior:
            // 1. First input (with .key("text1")) should still have its value
            assertEquals("value1", persistedInputAfter.inputValue(),
                "Input with user key should persist its value");

            // 2. Second input (no .key()) should be empty
            assertEquals("", noKeyInputAfter.inputValue(),
                "Input without user key should NOT persist its value");

            // 3. Third input (with .noPersist()) should be empty
            assertEquals("", noPersistInputAfter.inputValue(),
                "Input with noPersist() should NOT persist its value");
        });
    }

    @Test
    void testKeyReuseAcrossDifferentWidgetTypes(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    var view = Jt.radio("View", List.of("view1", "view2")).use();
                    Jt.text(String.valueOf(Jt.componentsState().get("lol1"))).key("bro").use();
                    if ("view1".equals(view)) {
                        Jt.textInput("some text").value("default value").key("lol1").use();
                    } else {
                        Jt.numberInput("some number", Double.class).key("lol1").use();
                    }
                    Jt.text(String.valueOf(Jt.componentsState().get("lol1"))).use();
                }
            }
            """;

        // Step 1: Launch app (view1 selected by default)
        // Step 2: Verify first text displays "null", second text displays "default value"
        // Step 3: Click view2 radio button
        // Step 4: Verify first text displays "default value", second text displays "0.0"
        // Step 5: Click view1 radio button
        // Step 6: Verify first text displays "0.0", second text displays "default value"
        // Step 7: Edit text input to "new text"
        // Step 8: Verify both texts display "new text"
        // Step 9: Click view2 radio button
        // Step 10: Verify first text displays "new text", second text displays "0.0"
        // Step 11: Click number input plus button
        // Step 12: Verify both texts display "0.01"
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Step 1: Launch app (view1 selected by default)
            Locator firstText = page.locator("jt-text").nth(0);
            Locator secondText = page.locator("jt-text").nth(1);

            // Step 2: Verify first text displays "null", second text displays "default value"
            LocatorAssertions.ContainsTextOptions WAIT_1_SEC_MAX_TXT = new LocatorAssertions.ContainsTextOptions().setTimeout(1000);
            assertThat(firstText).containsText("null", WAIT_1_SEC_MAX_TXT);
            assertThat(secondText).containsText("default value", WAIT_1_SEC_MAX_TXT);

            // Step 3: Click view2 radio button
            page.locator("jt-radio .radio-option:nth-child(2) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);

            // Step 4: Verify first text displays "default value", second text displays "0.0"
            assertThat(firstText).containsText("default value", WAIT_1_SEC_MAX_TXT);
            assertThat(secondText).containsText("0.0", WAIT_1_SEC_MAX_TXT);

            // Step 5: Click view1 radio button
            page.locator("jt-radio .radio-option:nth-child(1) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);

            // Step 6: Verify first text displays "0.0", second text displays "default value"
            assertThat(firstText).containsText("0.0", WAIT_1_SEC_MAX_TXT);
            assertThat(secondText).containsText("default value", WAIT_1_SEC_MAX_TXT);

            // Step 7: Edit text input to "new text"
            Locator textInput = page.locator("jt-text-input input");
            textInput.fill("new text");
            textInput.press("Enter");

            // Step 8: Verify both texts display "new text"
            assertThat(firstText).containsText("new text", WAIT_1_SEC_MAX_TXT);
            assertThat(secondText).containsText("new text", WAIT_1_SEC_MAX_TXT);

            // Step 9: Click view2 radio button
            page.locator("jt-radio .radio-option:nth-child(2) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);

            // Step 10: Verify first text displays "new text", second text displays "0.0"
            assertThat(firstText).containsText("new text", WAIT_1_SEC_MAX_TXT);
            assertThat(secondText).containsText("0.0", WAIT_1_SEC_MAX_TXT);

            // Step 11: Click number input plus button
            Locator plusButton = page.locator("jt-number-input .step-up");
            plusButton.click(WAIT_1_SEC_MAX_CLICK);

            // Step 12: Verify both texts display "0.01"
            assertThat(firstText).containsText("0.01", WAIT_1_SEC_MAX_TXT);
            assertThat(secondText).containsText("0.01", WAIT_1_SEC_MAX_TXT);
        });
    }
}
