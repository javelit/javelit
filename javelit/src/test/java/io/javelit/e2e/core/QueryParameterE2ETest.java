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

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for query parameter support.
 */
public class QueryParameterE2ETest {

    @Test
    void testQueryParameters(TestInfo testInfo) {
        JtRunnable app = () -> {
            var params = Jt.urlQueryParameters();
            Jt.title("Query Parameter Test").use();
            if (params.containsKey("name")) {
                Jt.text("Name: " + params.get("name").getFirst()).use();
            }
            if (params.containsKey("id")) {
                Jt.text("ID: " + params.get("id").getFirst()).use();
            }
            if (params.isEmpty()) {
                Jt.text("No query parameters").use();
            }
        };

        // First verify no query params
        // Navigate with query parameters
        // Verify query parameters are displayed
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // First verify no query params
            assertThat(page.getByText("No query parameters")).isVisible(WAIT_1_SEC_MAX);

            // Navigate with query parameters
            page.navigate(page.url() + "?name=Alice&id=456");

            // Verify query parameters are displayed
            assertThat(page.getByText("Name: Alice")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.getByText("ID: 456")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
