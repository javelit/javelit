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
package io.javelit.e2e.components.layout;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.javelit.core.Jt;
import io.javelit.core.JtContainer;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

/**
 * End-to-end tests for FormComponent and FormSubmitButtonComponent.
 */
public class FormComponentE2ETest {

    @Test
    void testFormSubmission(TestInfo testInfo) {
        JtRunnable app = () -> {
            JtContainer formContainer = Jt.form().use();
            Jt.text("used to get out of form").use();

            // Add form inputs
            String name = Jt.textInput("Your Name").value("NOT_SET").use(formContainer);
            String email = Jt.textInput("Your Email").value("NOT_SET").use(formContainer);

            // Add submit button
            boolean submitted = Jt.formSubmitButton("Submit Form").use(formContainer);

            Jt.text("Name: " + name + ", Email: " + email).use();
        };

        // used to get out of inputs easily
        // Fill name form input
        // ensure the change did not apply yet because the form was not submitted
        // Fill email form input
        // Click submit button
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // used to get out of inputs easily
            final Locator textUtilLocator = page.locator("jt-text", new Page.LocatorOptions().setHasText("used to get out of form"));
            assertThat(textUtilLocator).isVisible(WAIT_1_SEC_MAX);

            assertThat(page.locator("jt-form")).isVisible(WAIT_1_SEC_MAX);
            // Fill name form input
            Locator nameInput = page.locator("jt-text-input[label='Your Name'] input");
            nameInput.fill("John");
            textUtilLocator.click(WAIT_1_SEC_MAX_CLICK);
            // ensure the change did not apply yet because the form was not submitted
            assertThat(page.getByText("Name: " + "NOT_SET" + ", Email: " + "NOT_SET")).isVisible(WAIT_1_SEC_MAX);
            // Fill email form input
            Locator emailInput = page.locator("jt-text-input[label='Your Email'] input");
            emailInput.fill("john@example.com");
            textUtilLocator.click(WAIT_1_SEC_MAX_CLICK);
            assertThat(page.getByText("Name: " + "NOT_SET" + ", Email: " + "NOT_SET")).isVisible(WAIT_1_SEC_MAX);

            // Click submit button
            page.locator("jt-form-submit-button button").click(WAIT_1_SEC_MAX_CLICK);
            assertThat(page.getByText("Name: " + "John" + ", Email: " + "john@example.com")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
