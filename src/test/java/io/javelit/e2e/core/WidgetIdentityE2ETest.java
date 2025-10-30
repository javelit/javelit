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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for widget identity behavior.
 * Tests how widgets with and without user-defined keys behave when their parameters change.
 * even if a widget has a key, its value is reset if its configuration has changed
 */
public class WidgetIdentityE2ETest {

    @Test
    void testWidgetIdentityWithAndWithoutKey(TestInfo testInfo) {
        JtRunnable app = () -> {
            int minimum = Jt.numberInput("mini", Integer.class).minValue(0).maxValue(10).use();
            int slider1 = Jt.slider("no key").min(minimum).use().intValue();
            Jt.text("keyed value before: " + String.valueOf(Jt.componentsState().get("key1"))).use();
            int slider2 = Jt.slider("with key").key("key1").min(minimum).use().intValue();
            Jt.text("keyed value after: " + String.valueOf(Jt.componentsState().get("key1"))).use();
        };

        // Wait for page to load
        // Verify initial state
        // Get both sliders
        // "no key" slider
        // "with key" slider
        // Set both sliders to value around 20
        // Change the number input to 1 (click + button or fill with "1")
        // Verify final state
        // The first text should show "keyed value before: 20" because the slider without key
        // gets recreated when minimum changes, so its old value (20) is shown before it's created
        // Get sliders again after re-render
        // Both sliders should now have value 1 (slider without key was recreated with new min,
        // slider with key was updated but reset to min value)
        // The second text should show "keyed value after: 1"
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Wait for page to load
            assertThat(page.locator("jt-number-input")).isVisible(WAIT_1_SEC_MAX);

            // Verify initial state
            assertThat(page.getByText("keyed value before: null")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("keyed value after: 0.0")).isVisible(WAIT_1_SEC_MAX);

            // Get both sliders
            final Locator sliders = page.locator("jt-slider .slider-input");
            final Locator slider1 = sliders.nth(0); // "no key" slider
            final Locator slider2 = sliders.nth(1); // "with key" slider

            // Set both sliders to value around 20
            slider1.fill("20");
            slider2.fill("20");

            // Change the number input to 1 (click + button or fill with "1")
            Locator numberInputPlusButton = page.locator("jt-number-input .step-up");
            numberInputPlusButton.click(WAIT_1_SEC_MAX_CLICK);

            // Verify final state
            // The first text should show "keyed value before: 20" because the slider without key
            // gets recreated when minimum changes, so its old value (20) is shown before it's created
            assertThat(page.getByText("keyed value before: 20")).isVisible(WAIT_1_SEC_MAX);

            // Get sliders again after re-render
            final Locator slidersAfter = page.locator("jt-slider .slider-input");
            final Locator slider1After = slidersAfter.nth(0);
            final Locator slider2After = slidersAfter.nth(1);

            // Both sliders should now have value 1 (slider without key was recreated with new min,
            // slider with key was updated but reset to min value)
            assertThat(slider1After).hasValue("1");
            assertThat(slider2After).hasValue("1");

            // The second text should show "keyed value after: 1"
            assertThat(page.getByText("keyed value after: 1.0")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
