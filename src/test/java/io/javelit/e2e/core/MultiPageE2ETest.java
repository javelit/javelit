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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.javelit.e2e.helpers.PlaywrightUtils.EXACT_MATCH;
import static io.javelit.e2e.helpers.PlaywrightUtils.TEST_PROXY_PREFIX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_TEXT_C;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_50_MS_MAX;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for multipage navigation support.
 * Tests that the navigation component correctly handles page routing and content switching.
 */
public class MultiPageE2ETest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testDirectUrlNavigation(final boolean proxied, final TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-multipage-test-");
    copyResourceDirectory("multipage-test", tempDir);
    final Path mainFile = tempDir.resolve("MultiPageApp.java");

    PlaywrightUtils.runInBrowser(testInfo, mainFile, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

      // Verify initial home page loads
      assertThat(page.getByText("Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Welcome to the home page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      // Verify persistent footer is always visible
      assertThat(page.getByText("© 2025 Test App - Always Visible", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);

      // Navigate directly to Settings page using custom URL path
      page.navigate(page.url().replace("/home", "/config/settings"));
      // Verify Settings page content is visible
      assertThat(page.getByText("Settings Page", EXACT_MATCH).first()).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Configure your settings here", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      // Verify URL uses custom path
      assertTrue(page.url().contains(pathPrefix + "/config/settings"));
      // Verify Home page content is NOT visible
      assertThat(page.getByText("Welcome to the home page", EXACT_MATCH)).not().isVisible(WAIT_50_MS_MAX);
      // Verify persistent footer is always visible
      assertThat(page.getByText("© 2025 Test App - Always Visible", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);

      // Navigate directly to About page
      page.navigate(page.url().replace("/config/settings", "/about"));
      // Verify About page content is visible
      assertThat(page.getByText("About Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Learn more about this app", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      // Verify Settings page content is NOT visible
      assertThat(page.getByText("Configure your settings here", EXACT_MATCH)).not().isVisible(WAIT_50_MS_MAX);
      // Verify persistent footer is always visible
      assertThat(page.getByText("© 2025 Test App - Always Visible", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);

      // Test page not found - navigate to an invalid URL
      page.navigate(page.url().replace("/about", "/invalid/page"));
      // Verify page not found message appears
      assertThat(page.getByText("Page Not Found.", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      // Verify other page content is NOT visible
      assertThat(page.getByText("Settings Page", EXACT_MATCH)).not().isVisible(WAIT_50_MS_MAX);
      assertThat(page.getByText("Home Page", EXACT_MATCH)).not().isVisible(WAIT_50_MS_MAX);
      assertThat(page.getByText("About Page", EXACT_MATCH)).not().isVisible(WAIT_50_MS_MAX);
      // Verify persistent footer is still visible
      assertThat(page.getByText("© 2025 Test App - Always Visible", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);

      // Test root path "/" redirects to home page
      page.navigate(page.url().replace("/invalid/page", "/"));
      // Verify home page loads when navigating to root
      assertThat(page.getByText("Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Welcome to the home page", EXACT_MATCH)).isVisible();
      // Verify URL redirected to /HomePage
      String rootUrl = page.url();
      assertTrue(rootUrl.contains(pathPrefix + "/home"));
      // Verify persistent footer is still visible
      assertThat(page.getByText("© 2025 Test App - Always Visible", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
    });
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testSidebarNavigationClicks(final boolean proxied, final TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-multipage-navigation-test-");
    copyResourceDirectory("multipage-test", tempDir);
    final Path mainFile = tempDir.resolve("MultiPageApp.java");

    PlaywrightUtils.runInBrowser(testInfo, mainFile, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

      // Verify initial home page loads
      assertThat(page.getByText("Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Welcome to the home page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);

      // Verify initial URL ends with /HomePage
      String initialUrl = page.url();
      assertTrue(initialUrl.contains(pathPrefix + "/home"));

      // Click on Settings in the sidebar
      page
          .getByRole(AriaRole.LINK)
          .filter(new Locator.FilterOptions().setHasText("Settings"))
          .click(WAIT_1_SEC_MAX_CLICK);
      // Verify URL changed to custom path /config/settings
      String settingsUrl = page.url();
      assertTrue(settingsUrl.contains(pathPrefix + "/config/settings"));
      // Verify Settings page content appears
      assertThat(page.getByText("Settings Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Configure your settings here", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      // Verify Home page content is hidden
      assertThat(page.getByText("Welcome to the home page", EXACT_MATCH)).not().isVisible(WAIT_50_MS_MAX);

      // Click on About in the sidebar
      page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText("About")).click(WAIT_1_SEC_MAX_CLICK);
      // Verify URL changed to /AboutPage
      String aboutUrl = page.url();
      assertTrue(aboutUrl.contains(pathPrefix + "/about"));
      // Verify About page content appears
      assertThat(page.getByText("About Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Learn more about this app", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      // Verify Settings page content is hidden
      assertThat(page.getByText("Configure your settings here", EXACT_MATCH)).not().isVisible(WAIT_50_MS_MAX);

      // Click back to Home
      page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText("Home")).click(WAIT_1_SEC_MAX_CLICK);
      // Verify URL changed back to /HomePage
      String homeUrl = page.url();
      assertTrue(homeUrl.contains(pathPrefix + "/home"));
      // Verify Home page content reappears
      assertThat(page.getByText("Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Welcome to the home page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
    });
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testPersistentElementsAfterNavigation(final boolean proxied, final TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-multipage-persistent-test-");
    copyResourceDirectory("multipage-test", tempDir);
    final Path mainFile = tempDir.resolve("MultiPageApp.java");

    PlaywrightUtils.runInBrowser(testInfo, mainFile, true, proxied, page -> {
      // Verify footer is visible on home page
      assertThat(page.getByText("© 2025 Test App - Always Visible")).isVisible(WAIT_1_SEC_MAX);
      // Verify navigation sidebar is visible
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("Home"))).isVisible(
          WAIT_1_SEC_MAX);
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("Settings"))).isVisible(
          WAIT_1_SEC_MAX);
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("About"))).isVisible(
          WAIT_1_SEC_MAX);

      // Navigate to Settings page
      page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("Settings")).click(WAIT_1_SEC_MAX_CLICK);
      // Verify footer persists on Settings page
      assertThat(page.getByText("© 2025 Test App - Always Visible")).isVisible(WAIT_1_SEC_MAX);
      // Verify navigation sidebar persists
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("Home"))).isVisible(
          WAIT_1_SEC_MAX);
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("Settings"))).isVisible(
          WAIT_1_SEC_MAX);
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("About"))).isVisible(
          WAIT_1_SEC_MAX);

      // Navigate to About page
      page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("About")).click(WAIT_1_SEC_MAX_CLICK);
      // Verify footer persists on About page
      assertThat(page.getByText("© 2025 Test App - Always Visible")).isVisible(WAIT_1_SEC_MAX);
      // Verify navigation sidebar persists
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("Home"))).isVisible(
          WAIT_1_SEC_MAX);
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("Settings"))).isVisible(
          WAIT_1_SEC_MAX);
      assertThat(page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("About"))).isVisible(
          WAIT_1_SEC_MAX);

      // Verify the active page highlighting changes
      // The Settings link should have the 'active' class when on Settings page
      page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("Settings")).click(WAIT_1_SEC_MAX_CLICK);
      assertThat(page.locator(".nav-item.active")).containsText("Settings", WAIT_1_SEC_MAX_TEXT_C);
      // The About link should have the 'active' class when on About page
      page.locator(".nav-item").filter(new Locator.FilterOptions().setHasText("About")).click(WAIT_1_SEC_MAX_CLICK);
      assertThat(page.locator(".nav-item.active")).containsText("About", WAIT_1_SEC_MAX_TEXT_C);
    });
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testHiddenNavigation(final boolean proxied, final TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-hidden-nav-test-");
    copyResourceDirectory("multipage-test", tempDir);
    final Path mainFile = tempDir.resolve("HiddenNavApp.java");

    PlaywrightUtils.runInBrowser(testInfo, mainFile, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

      // Verify home page loads
      assertThat(page.getByText("Home Page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Welcome to the home page", EXACT_MATCH)).isVisible(WAIT_1_SEC_MAX);
      // Verify navigation sidebar is NOT visible
      assertThat(page.locator(".nav-item")).not().isVisible(WAIT_50_MS_MAX);
      assertThat(page.locator(".navigation-container")).not().isVisible(WAIT_50_MS_MAX);
      // Verify app content is still visible
      assertThat(page.getByText("App with hidden navigation")).isVisible(WAIT_1_SEC_MAX);

      // Verify initial URL ends with /HomePage
      String initialUrl = page.url();
      assertTrue(initialUrl.contains(pathPrefix + "/home"));

      // Verify direct URL navigation still works
      page.navigate(page.url().replace("/home", "/settings"));
      // Verify Settings page loads via direct URL
      assertThat(page.getByText("Settings Page")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Configure your settings here")).isVisible(WAIT_1_SEC_MAX);
      // Verify navigation is still hidden on Settings page
      assertThat(page.locator(".nav-item")).not().isVisible(WAIT_50_MS_MAX);

      // Navigate to About page directly
      page.navigate(page.url().replace("/settings", "/about"));
      // Verify About page loads
      assertThat(page.getByText("About Page")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.getByText("Learn more about this app")).isVisible(WAIT_1_SEC_MAX);
      // Verify navigation remains hidden on About page
      assertThat(page.locator(".nav-item")).not().isVisible(WAIT_50_MS_MAX);
      // Verify app content is still visible on all pages
      assertThat(page.getByText("App with hidden navigation")).isVisible(WAIT_1_SEC_MAX);
    });
  }
}
