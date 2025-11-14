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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Response;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for oEmbed functionality.
 * Tests oEmbed discovery link and endpoint response.
 */
public class OEmbedE2ETest {

    @Test
    void testOEmbedDiscoveryAndEndpoint(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.text("oEmbed Test").use();
                }
            }
            """;

        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            final String baseUrl = page.url().substring(0, page.url().lastIndexOf('/'));

            // Navigate to the page
            Response response = page.navigate(baseUrl);
            assertNotNull(response, "Response should not be null");

            // Verify app loads correctly
            assertThat(page.getByText("oEmbed Test")).isVisible(WAIT_1_SEC_MAX);

            // ==========================================
            // Part 1: Verify oEmbed Discovery Link
            // ==========================================

            // Check that the discovery link exists in the HTML
            final String pageContent = page.content();
            assertTrue(pageContent.contains("<link rel=\"alternate\" type=\"application/json+oembed\""),
                "Page should contain oEmbed discovery link");

            // Verify the discovery link has the correct href format
            assertTrue(pageContent.contains("href=\"/_/oembed?url="),
                "Discovery link should point to /_/oembed endpoint with url parameter");
            // Note: & is HTML-encoded to &amp; in the HTML
            assertTrue(pageContent.contains("&amp;format=json\""),
                "Discovery link should include format=json parameter");

            // ==========================================
            // Part 2: Test oEmbed Endpoint
            // ==========================================

            // Construct the oEmbed URL
            final String testUrl = baseUrl + "/";
            final String encodedUrl = URLEncoder.encode(testUrl, StandardCharsets.UTF_8);
            final String oembedUrl = baseUrl + "/_/oembed?url=" + encodedUrl + "&format=json";

            // Make API request to oEmbed endpoint
            final APIResponse oembedResponse = page.request().get(oembedUrl);

            // Verify response status
            assertEquals(200, oembedResponse.status(), "oEmbed endpoint should return 200 OK");

            // Verify content type
            final String contentType = oembedResponse.headers().get("content-type");
            assertNotNull(contentType, "Content-Type header should be present");
            assertTrue(contentType.contains("application/json"),
                "Content-Type should be application/json");

            // Parse JSON response
            final String responseBody = oembedResponse.text();
            final JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            // Verify required oEmbed fields
            assertEquals("1.0", json.get("version").getAsString(),
                "oEmbed version should be 1.0");
            assertEquals("rich", json.get("type").getAsString(),
                "oEmbed type should be rich");
            assertEquals("Javelit", json.get("provider_name").getAsString(),
                "Provider name should be Javelit");
            assertEquals("https://javelit.io", json.get("provider_url").getAsString(),
                "Provider URL should be https://javelit.io");

            // Verify HTML field contains iframe
            assertTrue(json.has("html"), "oEmbed response should have html field");
            final String htmlContent = json.get("html").getAsString();
            assertTrue(htmlContent.contains("<iframe"), "HTML should contain iframe tag");
            assertTrue(htmlContent.contains("src='"), "iframe should have src attribute");

            // Verify iframe includes ?embed=true parameter
            assertTrue(htmlContent.contains("?embed=true") || htmlContent.contains("&embed=true"),
                "iframe src should include embed=true parameter");

            // ==========================================
            // Part 3: Test Error Cases
            // ==========================================

            // Test missing URL parameter
            final APIResponse missingUrlResponse = page.request().get(baseUrl + "/_/oembed?format=json");
            assertEquals(400, missingUrlResponse.status(),
                "oEmbed endpoint should return 400 when url parameter is missing");
            assertTrue(missingUrlResponse.text().contains("Missing required parameter: url"),
                "Error message should indicate missing url parameter");

            // Test unsupported format
            final String xmlFormatUrl = baseUrl + "/_/oembed?url=" + encodedUrl + "&format=xml";
            final APIResponse xmlFormatResponse = page.request().get(xmlFormatUrl);
            assertEquals(501, xmlFormatResponse.status(),
                "oEmbed endpoint should return 501 for unsupported formats");
            assertTrue(xmlFormatResponse.text().contains("Only JSON format is supported"),
                "Error message should indicate only JSON is supported");
        });
    }
}
