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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.playwright.assertions.LocatorAssertions;
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * End-to-end tests for TableComponent.
 */
public class TableComponentE2ETest {

  record Person(String name, String role) {
  }

  @Test
  void testTableVariations(TestInfo testInfo) {
    JtRunnable app = () -> {
      // Test 1: Table with object list
      Jt.table(Arrays.asList(
          new Person("Alice", "Engineer"),
          new Person("Bob", "Designer")
      )).use();

      // Test 2: Table with object array (including null handling)
      Jt.table(new Person[]{
          new Person("Charlie", "Manager"),
          new Person("Diana", null)
      }).use();

      // Test 3: Table from array columns
      Map<String, Object[]> arrayColumns = new HashMap<>();
      arrayColumns.put("Product", new String[]{"Laptop", "Mouse"});
      arrayColumns.put("Category", new String[]{"Electronics", "Accessories"});
      Jt.tableFromArrayColumns(arrayColumns).use();

      // Test 4: Table from list columns
      Map<String, List<Object>> listColumns = new HashMap<>();
      listColumns.put("Name", Arrays.asList("Eve", "Frank"));
      listColumns.put("Status", Arrays.asList("Active", "Inactive"));
      Jt.tableFromListColumns(listColumns).use();
    };

    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // Verify all 4 tables are rendered
      assertThat(page.locator("jt-table")).hasCount(4, new LocatorAssertions.HasCountOptions().setTimeout(1000));

      // Test 1: Table with object list
      assertThat(page.locator("jt-table").nth(0).locator("th")).hasCount(2);
      assertThat(page.locator("jt-table").nth(0)).containsText("name");
      assertThat(page.locator("jt-table").nth(0)).containsText("role");
      assertThat(page.locator("jt-table").nth(0)).containsText("Alice");
      assertThat(page.locator("jt-table").nth(0)).containsText("Engineer");
      assertThat(page.locator("jt-table").nth(0)).containsText("Bob");
      assertThat(page.locator("jt-table").nth(0)).containsText("Designer");

      // Test 2: Table with object array (null handling)
      assertThat(page.locator("jt-table").nth(1).locator("th")).hasCount(2);
      assertThat(page.locator("jt-table").nth(1)).containsText("Charlie");
      assertThat(page.locator("jt-table").nth(1)).containsText("Manager");
      assertThat(page.locator("jt-table").nth(1)).containsText("Diana");
      assertThat(page.locator("jt-table").nth(1).locator(".empty-cell").first()).hasText("null");

      // Test 3: Table from array columns
      assertThat(page.locator("jt-table").nth(2).locator("th")).hasCount(2);
      assertThat(page.locator("jt-table").nth(2)).containsText("Product");
      assertThat(page.locator("jt-table").nth(2)).containsText("Category");
      assertThat(page.locator("jt-table").nth(2)).containsText("Laptop");
      assertThat(page.locator("jt-table").nth(2)).containsText("Electronics");
      assertThat(page.locator("jt-table").nth(2)).containsText("Mouse");
      assertThat(page.locator("jt-table").nth(2)).containsText("Accessories");

      // Test 4: Table from list columns
      assertThat(page.locator("jt-table").nth(3).locator("th")).hasCount(2);
      assertThat(page.locator("jt-table").nth(3)).containsText("Name");
      assertThat(page.locator("jt-table").nth(3)).containsText("Status");
      assertThat(page.locator("jt-table").nth(3).locator("tbody tr")).hasCount(2);
      assertThat(page.locator("jt-table").nth(3)).containsText("Eve");
      assertThat(page.locator("jt-table").nth(3)).containsText("Active");
      assertThat(page.locator("jt-table").nth(3)).containsText("Frank");
      assertThat(page.locator("jt-table").nth(3)).containsText("Inactive");
    });
  }
}
