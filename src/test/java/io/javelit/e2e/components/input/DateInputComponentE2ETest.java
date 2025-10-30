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

import java.time.LocalDate;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLASS;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT_C;

/**
 * End-to-end tests for DateInputComponent.
 */
public class DateInputComponentE2ETest {

    @Test
    void testBasicDateInput(TestInfo testInfo) {
        JtRunnable app = () -> {
            // Start with a specific date for deterministic testing
            LocalDate date = Jt.dateInput("Select date")
                    .value(LocalDate.of(2024, 7, 10))// July 10, 2024
                    .use();
            if (date != null) {
                Jt.text("Selected: " + date).use();
            }
        };

        // DateInput component exists
        // Initial value should be the specified date
        // July 10, 2024
        // Click to open calendar
        // Calendar popup should be visible
        // Select a different day (day 15 of current month - July)
        // Verify date changed to July 15, 2024
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Initial value should be the specified date
            final LocalDate initialDate = LocalDate.of(2024, 7, 10);  // July 10, 2024
            assertThat(page.getByText("Selected: " + initialDate)).isVisible(WAIT_1_SEC_MAX);

            // Click to open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Calendar popup should be visible
            assertThat(page.locator("jt-date-input .calendar-popup")).isVisible(WAIT_1_SEC_MAX);

            // Select a different day (day 15 of current month - July)
            page.locator("jt-date-input .calendar-day:not(.other-month)", new Page.LocatorOptions().setHasText("15")).first().click(WAIT_1_SEC_MAX_CLICK);

            // Verify date changed to July 15, 2024
            LocalDate expectedDate = LocalDate.of(2024, 7, 15);
            assertThat(page.getByText("Selected: " + expectedDate)).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDateInputWithDefaultValue(TestInfo testInfo) {
        JtRunnable app = () -> {
            LocalDate date = Jt.dateInput("Birth date")
                    .value(LocalDate.of(1990, 6, 15))
                    .use();
            if (date != null) {
                Jt.text("Birth date: " + date).use();
            }
        };

        // DateInput component exists
        // Should show the default value
        // Click to open calendar
        // Change to next month
        // Select day 20
        // Verify date changed to July 20, 1990
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
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
        JtRunnable app = () -> {
            LocalDate date = Jt.dateInput("Optional date")
                    .value(null)
                    .use();
            if (date != null) {
                Jt.text("Date selected: " + date).use();
            } else {
                Jt.text("No date selected").use();
            }
        };

        // DateInput component exists
        // Should show no date selected initially
        // Click to open calendar
        // Select a specific date (August 20, 2024) by clicking on day 20
        // Verify date is now selected - we selected day 20 of the current visible month
        // Since we can't be sure which month is shown, just verify that a date was selected
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Should show no date selected initially
            assertThat(page.getByText("No date selected")).isVisible(WAIT_1_SEC_MAX);

            // Click to open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Select a specific date (August 20, 2024) by clicking on day 20
            page.locator("jt-date-input .calendar-day:not(.other-month)", new Page.LocatorOptions().setHasText("20")).first().click(WAIT_1_SEC_MAX_CLICK);

            // Verify date is now selected - we selected day 20 of the current visible month
            // Since we can't be sure which month is shown, just verify that a date was selected
            assertThat(page.getByText("Date selected: ")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDateInputWithMinMaxConstraints(TestInfo testInfo) {
        JtRunnable app = () -> {
            // Use a fixed date to make tests deterministic
            LocalDate baseDate = LocalDate.of(2024, 6, 15);  // June 15, 2024 - middle of month to avoid edge cases
            LocalDate date = Jt.dateInput("Appointment date")
                    .value(baseDate)
                    .minValue(baseDate)
                    .maxValue(baseDate.plusMonths(1))
                    .use();
            if (date != null) {
                Jt.text("Appointment: " + date).use();
            }
        };

        // DateInput component exists
        // Use the same fixed date as in the app
        // June 15, 2024
        // Click to open calendar
        // Verify that dates before the base date are disabled
        // Try to click on day 14 (one day before base date) - should be disabled
        // June 14, 2024
        // Check if it exists and is disabled
        // Select a date within the valid range (base date + 7 days = June 22, 2024)
        // June 22, 2024
        // Since we're in the middle of June, futureDate will still be in June
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // DateInput component exists
            assertThat(page.locator("jt-date-input .date-input-field").first()).isVisible(WAIT_1_SEC_MAX);

            // Use the same fixed date as in the app
            final LocalDate baseDate = LocalDate.of(2024, 6, 15);  // June 15, 2024
            assertThat(page.getByText("Appointment: " + baseDate)).isVisible(WAIT_1_SEC_MAX);

            // Click to open calendar
            page.locator("jt-date-input .date-input-field").click(WAIT_1_SEC_MAX_CLICK);

            // Verify that dates before the base date are disabled
            // Try to click on day 14 (one day before base date) - should be disabled
            final int previousDay = 14;  // June 14, 2024
            final Locator previousDayButton = page.locator("jt-date-input .calendar-day:not(.other-month)",
                new Page.LocatorOptions().setHasText(String.valueOf(previousDay))).first();

            // Check if it exists and is disabled
            if (previousDayButton.count() > 0) {
                assertThat(previousDayButton).isDisabled();
            }

            // Select a date within the valid range (base date + 7 days = June 22, 2024)
            final LocalDate futureDate = baseDate.plusDays(7);  // June 22, 2024
            // Since we're in the middle of June, futureDate will still be in June
            page.locator("jt-date-input .calendar-day:not(.other-month)",
                new Page.LocatorOptions().setHasText(String.valueOf(futureDate.getDayOfMonth()))).first().click(WAIT_1_SEC_MAX_CLICK);
            assertThat(page.getByText("Appointment: " + futureDate)).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testDateInputWithDifferentFormats(TestInfo testInfo) {
        JtRunnable app = () -> {
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
        };

        // All DateInput components should exist
        // Check that each format displays correctly in the input field
        // US format: MM/DD/YYYY
        // European format: DD/MM/YYYY
        // ISO format: YYYY-MM-DD
        // Verify backend still receives standard format
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
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
        JtRunnable app = () -> {
            LocalDate date = Jt.dateInput("Locked date")
                    .value(LocalDate.of(2024, 1, 1))
                    .disabled(true)
                    .use();
            Jt.text("Locked date: " + date).use();
        };

        // DateInput component exists
        // Should show the date value
        // Input field should have disabled class
        // Click should not open calendar
        // Calendar popup should not be visible
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
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
        JtRunnable app = () -> {
            LocalDate date = Jt.dateInput("Navigate calendar")
                    .value(LocalDate.of(2024, 6, 15))
                    .use();
            if (date != null) {
                Jt.text("Selected: " + date).use();
            }
        };

        // DateInput component exists
        // Open calendar
        // Navigate to previous month
        // Select day 10 from May 2024
        // Verify date changed to May 10, 2024
        // Open calendar again
        // Navigate to next month twice (to July)
        // Select day 20 from July
        // Verify date changed to July 20, 2024
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
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
        JtRunnable app = () -> {
            LocalDate date = Jt.dateInput("Select any date")
                    .value(LocalDate.of(2024, 1, 1))
                    .minValue(LocalDate.of(2020, 1, 1))
                    .maxValue(LocalDate.of(2030, 12, 31))
                    .use();
            if (date != null) {
                Jt.text("Selected: " + date).use();
            }
        };

        // DateInput component exists
        // Open calendar
        // Change year using year selector
        // Change month using month selector
        // December (0-indexed)
        // Select day 31
        // Verify date changed to December 31, 2025
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
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
