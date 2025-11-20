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


/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.42.0

import io.javelit.core.Jt;
import pages.DashboardPage;
import pages.SettingsPage;
import pages.TestQueryPage;
import pages.UsersPage;

public class MultiPageApp {
  public static void main(String[] args) throws InterruptedException {
    // Define navigation with multiple pages;
    final var currentPage = Jt.navigation(
        Jt.page("/dashboard", DashboardPage::app)
          .title("Dashboard")
          .icon("📊")
          .section("lol")
          .home(),
        Jt.page("/users", UsersPage::app)
          .icon("👥")
          .title("Users"),
        Jt.page("/settings", SettingsPage::app)
          .icon("⚙️")
          .title("Settings"),
        Jt.page("query-parameters", TestQueryPage::app)
          .icon("🔍")
          .title("Query Parameters"),
        Jt.page("another-page", () -> anotherPage()).title("Inner class page")
    ).use();

    currentPage.run();
  }

  public static void anotherPage() {
    Jt.text("here is another page coming from an inner class").use();
  }

  private MultiPageApp() {
  }
}
