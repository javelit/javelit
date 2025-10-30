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
package io.javelit.e2e.components.data;

import com.microsoft.playwright.assertions.LocatorAssertions;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for TableComponent.
 */
public class TableComponentE2ETest {

    @Test
    void testTable_WithObjectList(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.Arrays;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.table(Arrays.asList(
                        new Person("Alice", "Engineer"),
                        new Person("Bob", "Designer")
                    )).use();
                }

                public record Person(String name, String role){}
            }
            """;

        // Wait for table to be visible
        // Check headers
        // Check data
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Wait for table to be visible
            assertThat(page.locator("jt-table")).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(1000));

            // Check headers
            assertThat(page.locator("jt-table th")).hasCount(2);
            assertThat(page.getByText("name")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("role")).isVisible(WAIT_1_SEC_MAX);

            // Check data
            assertThat(page.getByText("Alice")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Engineer")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Bob")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Designer")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testTable_WithObjectArray(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.table(new Person[] {
                        new Person("Alice", "Engineer"),
                        new Person("Bob", null)
                    }).use();
                }

                public record Person(String name, String role){}
            }
            """;

        // Wait for table to be visible
        // Check headers
        // Check data including null handling
        // Check null value displays as "—"
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Wait for table to be visible
            assertThat(page.locator("jt-table")).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(1000));

            // Check headers
            assertThat(page.locator("jt-table th")).hasCount(2);
            assertThat(page.getByText("name")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("role")).isVisible(WAIT_1_SEC_MAX);

            // Check data including null handling
            assertThat(page.getByText("Alice")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Engineer")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Bob")).isVisible(WAIT_1_SEC_MAX);

            // Check null value displays as "—"
            assertThat(page.locator("jt-table .empty-cell").first()).hasText("null");
        });
    }

    @Test
    void testTableFromArrayColumns(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.HashMap;
            import java.util.Map;

            public class TestApp {
                public static void main(String[] args) {
                    Map<String, Object[]> columns = new HashMap<>();
                    columns.put("Product", new String[]{"Laptop", "Mouse"});
                    columns.put("Category", new String[]{"Electronics", "Accessories"});
                    Jt.tableFromArrayColumns(columns).use();
                }
            }
            """;

        // Wait for table to be visible
        // Check headers
        // Check data
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Wait for table to be visible
            assertThat(page.locator("jt-table")).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(1000));

            // Check headers
            assertThat(page.locator("jt-table th")).hasCount(2);
            assertThat(page.getByText("Product")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Category")).isVisible(WAIT_1_SEC_MAX);

            // Check data
            assertThat(page.getByText("Laptop")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Electronics")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Mouse")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Accessories")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testTableFromListColumns(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.util.Arrays;
            import java.util.HashMap;
            import java.util.Map;

            public class TestApp {
                public static void main(String[] args) {
                    Map<String, java.util.List<Object>> columns = new HashMap<>();
                    columns.put("Name", Arrays.asList("Alice", "Bob"));
                    columns.put("Status", Arrays.asList("Active", "Inactive"));
                    Jt.tableFromListColumns(columns).use();
                }
            }
            """;

        // Wait for table to be visible
        // Check headers
        // Check data (verify table has correct number of rows and basic content)
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Wait for table to be visible
            assertThat(page.locator("jt-table")).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(1000));

            // Check headers
            assertThat(page.locator("jt-table th")).hasCount(2);
            assertThat(page.getByText("Name")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("Status")).isVisible(WAIT_1_SEC_MAX);

            // Check data (verify table has correct number of rows and basic content)
            assertThat(page.locator("jt-table tbody tr")).hasCount(2);
            assertThat(page.locator("jt-table")).containsText("Alice");
            assertThat(page.locator("jt-table")).containsText("Active");
            assertThat(page.locator("jt-table")).containsText("Bob");
            assertThat(page.locator("jt-table")).containsText("Inactive");
        });
    }
}
