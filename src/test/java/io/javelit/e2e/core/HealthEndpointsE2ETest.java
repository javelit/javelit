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

import com.microsoft.playwright.APIResponse;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for health and readiness endpoints.
 */
public class HealthEndpointsE2ETest {

    @Test
    void testHealthAndReadyEndpoints(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("Hello World").use();
                }
            }
            """;

        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify the app is running - check for "Hello World" text
            assertThat(page.getByText("Hello World")).isVisible(WAIT_1_SEC_MAX);

            // Test health endpoint
            final String baseUrl = page.url().substring(0, page.url().lastIndexOf('/'));
            final APIResponse healthResponse = page.request().get(baseUrl + "/_/health");
            assertEquals(200, healthResponse.status(), "Health endpoint should return 200");
            assertEquals("OK", healthResponse.text(), "Health endpoint should return 'OK'");

            // Test ready endpoint
            final APIResponse readyResponse = page.request().get(baseUrl + "/_/ready");
            assertEquals(200, readyResponse.status(), "Ready endpoint should return 200");
            assertEquals("OK", readyResponse.text(), "Ready endpoint should return 'OK'");
        });
    }
}
