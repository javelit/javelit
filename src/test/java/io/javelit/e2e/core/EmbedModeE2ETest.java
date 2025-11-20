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

import java.util.List;

import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.SameSiteAttribute;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for embed mode functionality.
 * Tests cookie SameSite attributes and X-Frame-Options header based on ?embed=true parameter.
 */
public class EmbedModeE2ETest {

  @Test
  void testEmbedModeConfiguresCookiesAndHeaders(TestInfo testInfo) {
    final @Language("java") String app = """
        import io.javelit.core.Jt;
        
        public class TestApp {
            public static void main(String[] args) {
                Jt.text("Embed Mode Test").use();
            }
        }
        """;

    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      final String baseUrl = page.url().substring(0, page.url().lastIndexOf('/'));

      // ==========================================
      // Part 1: Test WITHOUT ?embed=true
      // ==========================================

      // Navigate without embed parameter
      Response response = page.navigate(baseUrl);
      assertNotNull(response, "Response should not be null");

      // Verify app loads correctly
      assertThat(page.getByText("Embed Mode Test")).isVisible(WAIT_1_SEC_MAX);

      // Verify X-Frame-Options header is set to DENY
      String xFrameOptions = response.headerValue("x-frame-options");
      assertEquals("DENY", xFrameOptions,
                   "X-Frame-Options should be DENY when embed mode is not enabled");

      // Wait for cookies to be set (after WebSocket connection)
      page.waitForTimeout(500);

      // Get cookies from browser context
      List<Cookie> cookies = page.context().cookies();

      // Find session and XSRF cookies
      Cookie sessionCookie = cookies.stream()
                                    .filter(c -> "javelit-session-id".equals(c.name))
                                    .findFirst()
                                    .orElse(null);
      Cookie xsrfCookie = cookies.stream()
                                 .filter(c -> "javelit-xsrf".equals(c.name))
                                 .findFirst()
                                 .orElse(null);

      assertNotNull(sessionCookie, "Session cookie should be set");
      assertNotNull(xsrfCookie, "XSRF cookie should be set");

      // Verify cookies are NOT SameSite=None (should be Lax or Strict)
      assertNotEquals(SameSiteAttribute.NONE, sessionCookie.sameSite,
                      "Session cookie should NOT be SameSite=None without embed mode");
      assertNotEquals(SameSiteAttribute.NONE, xsrfCookie.sameSite,
                      "XSRF cookie should NOT be SameSite=None without embed mode");

      // ==========================================
      // Part 2: Test WITH ?embed=true
      // ==========================================

      // Clear cookies to get fresh ones
      page.context().clearCookies();

      // Navigate WITH embed parameter
      response = page.navigate(baseUrl + "?embed=true");
      assertNotNull(response, "Response should not be null");

      // Verify app loads correctly
      assertThat(page.getByText("Embed Mode Test")).isVisible(WAIT_1_SEC_MAX);

      // Verify X-Frame-Options header is NOT present (allows iframe embedding)
      xFrameOptions = response.headerValue("x-frame-options");
      assertNull(xFrameOptions,
                 "X-Frame-Options should not be set when embed mode is enabled");

      // Wait for cookies to be set (after WebSocket connection)
      page.waitForTimeout(500);

      // Get cookies from browser context
      cookies = page.context().cookies();

      // Find session and XSRF cookies
      sessionCookie = cookies.stream()
                             .filter(c -> "javelit-session-id".equals(c.name))
                             .findFirst()
                             .orElse(null);
      xsrfCookie = cookies.stream()
                          .filter(c -> "javelit-xsrf".equals(c.name))
                          .findFirst()
                          .orElse(null);

      assertNotNull(sessionCookie, "Session cookie should be set in embed mode");
      assertNotNull(xsrfCookie, "XSRF cookie should be set in embed mode");

      // Verify cookies HAVE SameSite=None for cross-origin iframe support
      assertEquals(SameSiteAttribute.NONE, sessionCookie.sameSite,
                   "Session cookie should be SameSite=None in embed mode");
      assertTrue(sessionCookie.secure,
                 "Session cookie should be Secure in embed mode");

      assertEquals(SameSiteAttribute.NONE, xsrfCookie.sameSite,
                   "XSRF cookie should be SameSite=None in embed mode");
      assertTrue(xsrfCookie.secure,
                 "XSRF cookie should be Secure in embed mode");
    });
  }
}
