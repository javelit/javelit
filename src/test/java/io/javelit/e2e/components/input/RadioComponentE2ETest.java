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

import com.microsoft.playwright.Page;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT_C;

/**
 * End-to-end tests for RadioComponent.
 */
public class RadioComponentE2ETest {

    @Test
    void testBasicRadioWithStrings(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String color = Jt.radio("Select color", List.of("Red", "Green", "Blue")).use();
                    if (color != null) {
                        Jt.text("Selected: " + color).use();
                    }
                }
            }
            """;

        // Radio component exists
        // First option is selected by default (index 0)
        // Click on Green radio button
        // Verify selection changed to Green
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Radio component exists
            assertThat(page.locator("jt-radio")).isVisible(WAIT_1_SEC_MAX);
            // First option is selected by default (index 0)
            assertThat(page.getByText("Selected: Red")).isVisible(WAIT_1_SEC_MAX);
            // Click on Green radio button
            page.locator("jt-radio .radio-option:nth-child(2) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Green
            assertThat(page.getByText("Selected: Green")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testRadioWithSecondValueSelected(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String size = Jt.radio("Select size", List.of("Small", "Medium", "Large"))
                            .index(1)  // Select "Medium" (index 1)
                            .use();
                    if (size != null) {
                        Jt.text("Size: " + size).use();
                    }
                }
            }
            """;

        // Radio component exists
        // Second option (Medium) is selected by default
        // Click on Large radio button
        // Verify selection changed to Large
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Radio component exists
            assertThat(page.locator("jt-radio")).isVisible(WAIT_1_SEC_MAX);
            // Second option (Medium) is selected by default
            assertThat(page.getByText("Size: Medium")).isVisible(WAIT_1_SEC_MAX);
            // Click on Large radio button
            page.locator("jt-radio .radio-option:nth-child(3) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Large
            assertThat(page.getByText("Size: Large")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testRadioWithNullIndex(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String option = Jt.radio("Select option", List.of("Option A", "Option B", "Option C"))
                            .index(null)  // No default selection
                            .use();
                    if (option != null) {
                        Jt.text("Selected: " + option).use();
                    } else {
                        Jt.text("No option selected").use();
                    }
                }
            }
            """;

        // Radio component exists
        // No option selected initially
        // Click on Option B radio button
        // Verify selection changed to Option B
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Radio component exists
            assertThat(page.locator("jt-radio")).isVisible(WAIT_1_SEC_MAX);
            // No option selected initially
            assertThat(page.getByText("No option selected")).isVisible(WAIT_1_SEC_MAX);
            // Click on Option B radio button
            page.locator("jt-radio .radio-option:nth-child(2) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Option B
            assertThat(page.getByText("Selected: Option B")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testRadioWithObjectsAndFormatFunction(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                record Person(String name, int age) {}

                public static void main(String[] args) {
                    List<Person> people = List.of(
                        new Person("Alice", 25),
                        new Person("Bob", 30),
                        new Person("Charlie", 35)
                    );

                    Person selected = Jt.radio("Select person", people)
                            .formatFunction(p -> p.name() + " (" + p.age() + " years)")
                            .use();

                    if (selected != null) {
                        Jt.text("Selected: " + selected.name()).use();
                    }
                }
            }
            """;

        // Radio component exists
        // Verify formatted options are displayed
        // First option is selected by default
        // Click on Bob radio button
        // Verify selection changed to Bob
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Radio component exists
            assertThat(page.locator("jt-radio")).isVisible(WAIT_1_SEC_MAX);
            // Verify formatted options are displayed
            assertThat(page.locator("jt-radio .radio-option-label", new Page.LocatorOptions().setHasText("Alice (25 years)"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-radio .radio-option-label", new Page.LocatorOptions().setHasText("Bob (30 years)"))).isVisible(WAIT_1_SEC_MAX);
            // First option is selected by default
            assertThat(page.getByText("Selected: Alice")).isVisible(WAIT_1_SEC_MAX);
            // Click on Bob radio button
            page.locator("jt-radio .radio-option:nth-child(2) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Bob
            assertThat(page.getByText("Selected: Bob")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testRadioWithCaptions(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String plan = Jt.radio("Select plan", List.of("Basic", "Pro", "Enterprise"))
                            .captions(List.of("$0/month", "$10/month", "$50/month"))
                            .use();
                    if (plan != null) {
                        Jt.text("Plan: " + plan).use();
                    }
                }
            }
            """;

        // Radio component exists
        // Verify captions are displayed
        // First option (Basic) is selected by default
        // Click on Pro radio button
        // Verify selection changed to Pro
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Radio component exists
            assertThat(page.locator("jt-radio")).isVisible(WAIT_1_SEC_MAX);
            // Verify captions are displayed
            assertThat(page.locator("jt-radio .radio-option:first-child .caption")).containsText("$0/month", WAIT_1_SEC_MAX_TEXT_C);
            assertThat(page.locator("jt-radio .radio-option:nth-child(2) .caption")).containsText("$10/month", WAIT_1_SEC_MAX_TEXT_C);
            assertThat(page.locator("jt-radio .radio-option:nth-child(3) .caption")).containsText("$50/month", WAIT_1_SEC_MAX_TEXT_C);
            // First option (Basic) is selected by default
            assertThat(page.getByText("Plan: Basic")).isVisible(WAIT_1_SEC_MAX);
            // Click on Pro radio button
            page.locator("jt-radio .radio-option:nth-child(2) .radio-visual").click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Pro
            assertThat(page.getByText("Plan: Pro")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
