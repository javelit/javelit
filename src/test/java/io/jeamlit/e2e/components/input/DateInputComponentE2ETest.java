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
package io.jeamlit.e2e.components.input;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLASS;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT_C;

/**
 * End-to-end tests for DateInputComponent.
 */
public class DateInputComponentE2ETest {

    @Test
    void testBasicDateInput(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import java.time.LocalDate;

            public class TestApp {
                public static void main(String[] args) {
                    LocalDate date = Jt.dateInput("Select date").use();
                    if (date != null) {
                        Jt.text("Selected: " + date).use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Default value should be today's date
            final LocalDate today = LocalDate.now(ZoneId.systemDefault());
            assertThat(page.getByText("Selected: " + today)).isVisible(WAIT_1_SEC_MAX);

            // Click to open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Calendar popup should be visible
            assertThat(page.locator("jt-date-input .calendar-popup")).isVisible(WAIT_1_SEC_MAX);

            // Select a different day (day 15 of current month)
            page.locator("jt-date-input .calendar-day:not(.other-month)", new Page.LocatorOptions().setHasText("15")).first().click(WAIT_1_SEC_MAX_CLICK);

            // Verify date changed
            LocalDate expectedDate = LocalDate.of(today.getYear(), today.getMonth(), 15);
            assertThat(page.getByText("Selected: " + expectedDate)).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDateInputWithDefaultValue(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import java.time.LocalDate;

            public class TestApp {
                public static void main(String[] args) {
                    LocalDate date = Jt.dateInput("Birth date")
                            .value(LocalDate.of(1990, 6, 15))
                            .use();
                    if (date != null) {
                        Jt.text("Birth date: " + date).use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Should show the default value
            assertThat(page.getByText("Birth date: 1990-06-15")).isVisible(WAIT_1_SEC_MAX);

            // Click to open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Change to next month
            page.locator("jt-date-input .calendar-nav-button").last().click(WAIT_1_SEC_MAX_CLICK);

            // Select day 20
            page.locator("jt-date-input .calendar-day:not(.other-month)", new Page.LocatorOptions().setHasText("20")).first().click(WAIT_1_SEC_MAX_CLICK);

            // Verify date changed to July 20, 1990
            assertThat(page.getByText("Birth date: 1990-07-20")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDateInputWithNullValue(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import java.time.LocalDate;

            public class TestApp {
                public static void main(String[] args) {
                    LocalDate date = Jt.dateInput("Optional date")
                            .value(null)
                            .use();
                    if (date != null) {
                        Jt.text("Date selected: " + date).use();
                    } else {
                        Jt.text("No date selected").use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Should show no date selected initially
            assertThat(page.getByText("No date selected")).isVisible(WAIT_1_SEC_MAX);

            // Click to open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Select today's date (click on the day marked as today)
            page.locator("jt-date-input .calendar-day.today").click(WAIT_1_SEC_MAX_CLICK);

            // Verify date is now selected
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            assertThat(page.getByText("Date selected: " + today)).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDateInputWithMinMaxConstraints(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import java.time.LocalDate;

            public class TestApp {
                public static void main(String[] args) {
                    LocalDate today = LocalDate.now(ZoneId.systemDefault());
                    LocalDate date = Jt.dateInput("Appointment date")
                            .value(today)
                            .minValue(today)
                            .maxValue(today.plusMonths(1))
                            .use();
                    if (date != null) {
                        Jt.text("Appointment: " + date).use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            assertThat(page.getByText("Appointment: " + today)).isVisible(WAIT_1_SEC_MAX);

            // Click to open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Verify that dates before today are disabled
            // Try to click on yesterday (should be disabled)
            int yesterday = today.minusDays(1).getDayOfMonth();
            Locator yesterdayButton = page.locator("jt-date-input .calendar-day:not(.other-month)",
                new Page.LocatorOptions().setHasText(String.valueOf(yesterday))).first();

            // Check if it exists and is disabled
            if (yesterdayButton.count() > 0) {
                assertThat(yesterdayButton).isDisabled();
            }

            // Select a date within the valid range (today + 7 days)
            LocalDate futureDate = today.plusDays(7);
            if (futureDate.getMonth() == today.getMonth()) {
                page.locator("jt-date-input .calendar-day:not(.other-month)",
                    new Page.LocatorOptions().setHasText(String.valueOf(futureDate.getDayOfMonth()))).first().click(WAIT_1_SEC_MAX_CLICK);
                assertThat(page.getByText("Appointment: " + futureDate)).isVisible(WAIT_1_SEC_MAX);
            }
        });
    }

    @Test
    void testDateInputWithDifferentFormats(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import java.time.LocalDate;

            public class TestApp {
                public static void main(String[] args) {
                    LocalDate date1 = Jt.dateInput("US format")
                            .format("MM/DD/YYYY")
                            .value(LocalDate.of(2024, 12, 25))
                            .use();

                    LocalDate date2 = Jt.dateInput("European format")
                            .format("DD/MM/YYYY")
                            .value(LocalDate.of(2024, 12, 25))
                            .use();

                    LocalDate date3 = Jt.dateInput("ISO format")
                            .format("YYYY-MM-DD")
                            .value(LocalDate.of(2024, 12, 25))
                            .use();

                    Jt.text("US: " + date1).use();
                    Jt.text("EU: " + date2).use();
                    Jt.text("ISO: " + date3).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // All DateInput components should exist
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Check that each format displays correctly in the input field
            // US format: MM/DD/YYYY
            final Locator usInput = page.locator("jt-date-input").first().locator(".date-value");
            assertThat(usInput).containsText("12/25/2024", WAIT_1_SEC_MAX_TEXT_C);

            // European format: DD/MM/YYYY
            final Locator euInput = page.locator("jt-date-input").nth(1).locator(".date-value");
            assertThat(euInput).containsText("25/12/2024", WAIT_1_SEC_MAX_TEXT_C);

            // ISO format: YYYY-MM-DD
            final Locator isoInput = page.locator("jt-date-input").nth(2).locator(".date-value");
            assertThat(isoInput).containsText("2024-12-25", WAIT_1_SEC_MAX_TEXT_C);

            // Verify backend still receives standard format
            assertThat(page.getByText("US: 2024-12-25")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("EU: 2024-12-25")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("ISO: 2024-12-25")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDisabledDateInput(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import java.time.LocalDate;

            public class TestApp {
                public static void main(String[] args) {
                    LocalDate date = Jt.dateInput("Locked date")
                            .value(LocalDate.of(2024, 1, 1))
                            .disabled(true)
                            .use();
                    Jt.text("Locked date: " + date).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Should show the date value
            assertThat(page.getByText("Locked date: 2024-01-01")).isVisible(WAIT_1_SEC_MAX);

            // Input field should have disabled class
            final Locator inputField = page.locator("jt-date-input .date-input-field");
            assertThat(inputField).hasClass(Pattern.compile(".*\\bdisabled\\b.*"), WAIT_1_SEC_MAX_CLASS);

            // Click should not open calendar
            inputField.click(WAIT_1_SEC_MAX_CLICK);

            // Calendar popup should not be visible
            assertThat(page.locator("jt-date-input .calendar-popup")).not().isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDateInputCalendarNavigation(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import java.time.LocalDate;

            public class TestApp {
                public static void main(String[] args) {
                    LocalDate date = Jt.dateInput("Navigate calendar")
                            .value(LocalDate.of(2024, 6, 15))
                            .use();
                    if (date != null) {
                        Jt.text("Selected: " + date).use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Navigate to previous month
            page.locator("jt-date-input .calendar-nav-button").first().click(WAIT_1_SEC_MAX_CLICK);

            // Select day 10 from May 2024
            page.locator("jt-date-input .calendar-day:not(.other-month)",
                new Page.LocatorOptions().setHasText("10")).first().click(WAIT_1_SEC_MAX_CLICK);

            // Verify date changed to May 10, 2024
            assertThat(page.getByText("Selected: 2024-05-10")).isVisible(WAIT_1_SEC_MAX);

            // Open calendar again
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Navigate to next month twice (to July)
            page.locator("jt-date-input .calendar-nav-button").last().click(WAIT_1_SEC_MAX_CLICK);
            page.locator("jt-date-input .calendar-nav-button").last().click(WAIT_1_SEC_MAX_CLICK);

            // Select day 20 from July
            page.locator("jt-date-input .calendar-day:not(.other-month)",
                new Page.LocatorOptions().setHasText("20")).first().click(WAIT_1_SEC_MAX_CLICK);

            // Verify date changed to July 20, 2024
            assertThat(page.getByText("Selected: 2024-07-20")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDateInputWithYearMonthSelectors(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.jeamlit.core.Jt;
            import java.time.LocalDate;

            public class TestApp {
                public static void main(String[] args) {
                    LocalDate date = Jt.dateInput("Select any date")
                            .value(LocalDate.of(2024, 1, 1))
                            .minValue(LocalDate.of(2020, 1, 1))
                            .maxValue(LocalDate.of(2030, 12, 31))
                            .use();
                    if (date != null) {
                        Jt.text("Selected: " + date).use();
                    }
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Change year using year selector
            Locator yearSelect = page.locator("jt-date-input .calendar-year-select");
            yearSelect.selectOption("2025");

            // Change month using month selector
            Locator monthSelect = page.locator("jt-date-input .calendar-month-select");
            monthSelect.selectOption("11"); // December (0-indexed)

            // Select day 31
            page.locator("jt-date-input .calendar-day:not(.other-month)",
                new Page.LocatorOptions().setHasText("31")).first().click(WAIT_1_SEC_MAX_CLICK);

            // Verify date changed to December 31, 2025
            assertThat(page.getByText("Selected: 2025-12-31")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
