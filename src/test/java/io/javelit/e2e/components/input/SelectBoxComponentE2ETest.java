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
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for SelectBoxComponent.
 */
public class SelectBoxComponentE2ETest {

    @Test
    void testBasicSelectBoxWithStrings(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String color = Jt.selectbox("Select color", List.of("Red", "Green", "Blue")).use();
                    if (color != null) {
                        Jt.text("Selected: " + color).use();
                    } else {
                        Jt.text("No color selected").use();
                    }
                }
            }
            """;

        // SelectBox component exists
        // First option is selected by default (index 0)
        // Click to open dropdown
        // Select Green option
        // Verify selection changed to Green
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // SelectBox component exists
            assertThat(page.locator("jt-selectbox")).isVisible(WAIT_1_SEC_MAX);
            // First option is selected by default (index 0)
            assertThat(page.getByText("Selected: Red")).isVisible(WAIT_1_SEC_MAX);

            // Click to open dropdown
            page.locator("jt-selectbox .selectbox-control").click(WAIT_1_SEC_MAX_CLICK);
            // Select Green option
            page.locator("jt-selectbox .selectbox-option", new Page.LocatorOptions().setHasText("Green")).click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Green
            assertThat(page.getByText("Selected: Green")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testSelectBoxWithSecondValueSelected(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String size = Jt.selectbox("Select size", List.of("Small", "Medium", "Large"))
                            .index(1)  // Select "Medium" (index 1)
                            .use();
                    if (size != null) {
                        Jt.text("Size: " + size).use();
                    }
                }
            }
            """;

        // SelectBox component exists
        // Second option (Medium) is selected by default
        // Click to open dropdown
        // Select Large option
        // Verify selection changed to Large
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // SelectBox component exists
            assertThat(page.locator("jt-selectbox")).isVisible(WAIT_1_SEC_MAX);
            // Second option (Medium) is selected by default
            assertThat(page.getByText("Size: Medium")).isVisible(WAIT_1_SEC_MAX);

            // Click to open dropdown
            page.locator("jt-selectbox .selectbox-control").click(WAIT_1_SEC_MAX_CLICK);
            // Select Large option
            page.locator("jt-selectbox .selectbox-option", new Page.LocatorOptions().setHasText("Large")).click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Large
            assertThat(page.getByText("Size: Large")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testSelectBoxWithNullIndex(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String option = Jt.selectbox("Select option", List.of("Option A", "Option B", "Option C"))
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

        // SelectBox component exists
        // No option selected initially
        // Click to open dropdown
        // Select Option B
        // Verify selection changed to Option B
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // SelectBox component exists
            assertThat(page.locator("jt-selectbox")).isVisible(WAIT_1_SEC_MAX);
            // No option selected initially
            assertThat(page.getByText("No option selected")).isVisible(WAIT_1_SEC_MAX);

            // Click to open dropdown
            page.locator("jt-selectbox .selectbox-control").click(WAIT_1_SEC_MAX_CLICK);
            // Select Option B
            page.locator("jt-selectbox .selectbox-option", new Page.LocatorOptions().setHasText("Option B")).click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Option B
            assertThat(page.getByText("Selected: Option B")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testSelectBoxWithObjectsAndFormatFunction(TestInfo testInfo) {
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

                    Person selected = Jt.selectbox("Select person", people)
                            .formatFunction(p -> p.name() + " (" + p.age() + " years)")
                            .use();

                    if (selected != null) {
                        Jt.text("Selected: " + selected.name()).use();
                    }
                }
            }
            """;

        // SelectBox component exists
        // First option (Alice) is selected by default
        // Click to open dropdown and verify formatted options
        // Select Bob option
        // Verify selection changed to Bob
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // SelectBox component exists
            assertThat(page.locator("jt-selectbox")).isVisible(WAIT_1_SEC_MAX);
            // First option (Alice) is selected by default
            assertThat(page.getByText("Selected: Alice")).isVisible(WAIT_1_SEC_MAX);

            // Click to open dropdown and verify formatted options
            page.locator("jt-selectbox .selectbox-control").click(WAIT_1_SEC_MAX_CLICK);
            assertThat(page.locator("jt-selectbox .selectbox-option", new Page.LocatorOptions().setHasText("Alice (25 years)"))).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-selectbox .selectbox-option", new Page.LocatorOptions().setHasText("Bob (30 years)"))).isVisible(WAIT_1_SEC_MAX);

            // Select Bob option
            page.locator("jt-selectbox .selectbox-option", new Page.LocatorOptions().setHasText("Bob (30 years)")).click(WAIT_1_SEC_MAX_CLICK);
            // Verify selection changed to Bob
            assertThat(page.getByText("Selected: Bob")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testSelectBoxWithAcceptNewOptionsAndExistingList(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String skill = Jt.selectbox("Select or enter skill", List.of("Java", "Python", "JavaScript"))
                            .acceptNewOptions(true)
                            .use();
                    if (skill != null) {
                        Jt.text("Skill: " + skill).use();
                    }
                }
            }
            """;

        // SelectBox component exists
        // First option (Java) is selected by default
        // Test selecting existing option
        // Test typing custom value
        // Verify custom value is displayed
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // SelectBox component exists
            assertThat(page.locator("jt-selectbox")).isVisible(WAIT_1_SEC_MAX);
            // First option (Java) is selected by default
            assertThat(page.getByText("Skill: Java")).isVisible(WAIT_1_SEC_MAX);

            // Test selecting existing option
            page.locator("jt-selectbox .selectbox-control").click(WAIT_1_SEC_MAX_CLICK);
            page.locator("jt-selectbox .selectbox-option", new Page.LocatorOptions().setHasText("Python")).click(WAIT_1_SEC_MAX_CLICK);
            assertThat(page.getByText("Skill: Python")).isVisible(WAIT_1_SEC_MAX);

            // Test typing custom value
            page.locator("jt-selectbox .selectbox-control").click(WAIT_1_SEC_MAX_CLICK);
            page.locator("jt-selectbox .selectbox-search").clear(new Locator.ClearOptions().setTimeout(1000));
            page.locator("jt-selectbox .selectbox-search").fill("TypeScript", new Locator.FillOptions().setTimeout(1000));
            page.locator("jt-selectbox .selectbox-search").press("Enter", new Locator.PressOptions().setTimeout(1000));

            // Verify custom value is displayed
            assertThat(page.getByText("Skill: TypeScript")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testSelectBoxWithEmptyListAndAcceptNewOptions(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.List;

            public class TestApp {
                public static void main(String[] args) {
                    String tag = Jt.selectbox("Enter new tag", List.<String>of())
                            .acceptNewOptions(true)
                            .use();
                    if (tag != null) {
                        Jt.text("Tag: " + tag).use();
                    } else {
                        Jt.text("No tag entered").use();
                    }
                }
            }
            """;

        // SelectBox component exists
        // No tag selected initially
        // Type custom value
        // Verify custom value is displayed
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // SelectBox component exists
            assertThat(page.locator("jt-selectbox")).isVisible(WAIT_1_SEC_MAX);
            // No tag selected initially
            assertThat(page.getByText("No tag entered")).isVisible(WAIT_1_SEC_MAX);

            // Type custom value
            page.locator("jt-selectbox .selectbox-control").click(WAIT_1_SEC_MAX_CLICK);
            page.locator("jt-selectbox .selectbox-search").fill("important", new Locator.FillOptions().setTimeout(1000));
            page.locator("jt-selectbox .selectbox-search").press("Enter", new Locator.PressOptions().setTimeout(1000));

            // Verify custom value is displayed
            assertThat(page.getByText("Tag: important")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
