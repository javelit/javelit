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
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for SliderComponent.
 */
public class SliderComponentE2ETest {

    @Test
    void testSliderDrag() {
        final @Language("java") String app = """
                import tech.catheu.jeamlit.core.Jt;
                
                public class TestApp {
                    public static void main(String[] args) {
                        double value = Jt.slider("Temperature")
                            .min(0)
                            .max(100)
                            .value(50)
                            .use();
                        Jt.text("Temperature: " + value).use();
                    }
                }
                """;

        PlaywrightUtils.runInBrowser(app, page -> {
            // slider exists
            assertThat(page.locator("jt-slider")).isVisible(WAIT_1_SEC_MAX);
            // intial value is 50
            assertThat(page.getByText("Temperature: " + 50)).isVisible(WAIT_1_SEC_MAX);
            // change the slider value to 25
            final Locator sliderInput = page.locator("jt-slider .slider-input");
            sliderInput.fill("25");
            // text is updated
            assertThat(page.getByText("Temperature: " + 25)).isVisible(WAIT_1_SEC_MAX);

        });
    }
}