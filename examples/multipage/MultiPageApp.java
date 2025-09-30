



import io.jeamlit.core.Jt;
import io.jeamlit.core.JtContainer;import pages.DashboardPage;
import pages.SettingsPage;
import pages.TestQueryPage;
import pages.UsersPage;

public class MultiPageApp {
    public static void main(String[] args) throws InterruptedException {
        // Define navigation with multiple pages;
        final var currentPage = Jt.navigation(
            Jt.page(DashboardPage.class)
                .title("Dashboard")
                    .icon("📊")
                    .section("lol")
                .home(),
            Jt.page(UsersPage.class)
                    .icon("👥")
                .title("Users"),
            Jt.page(SettingsPage.class)
                    .icon("⚙️")
                .title("Settings"),
            Jt.page(TestQueryPage.class)
                    .icon("🔍")
                    .title("Query Parameters"),
            Jt.page(AnotherPage.class).title("Inner class page")
        ).use();

        currentPage.run();
    }

    public static class AnotherPage {
        public static void main(String[] args) {
            Jt.text("here is another page coming from an inner class").use();
        }
    }
}
