


/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.87.0

import io.javelit.core.Jt;
import io.javelit.core.JtContainer;
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
        Jt.page("/query-test", TestQueryPage::app)
          .icon("🔍")
          .title("Query Parameters"),
        Jt.page("/another", () -> anotherPage()).title("Inner method page")
    ).use();

    currentPage.run();
  }

  public static void anotherPage() {
    Jt.text("here is another page coming from an inner class").use();
  }
}
