import io.jeamlit.core.Jt;

public class App {

    public static void main(String[] args) {
        boolean loggedIn = Jt.sessionState().computeIfAbsentBoolean("logged_in", k -> false);

        if (!loggedIn) {
            var currentPage = Jt.navigation(Jt.page(LoginPage.class)).hidden().use();
            currentPage.run();
        } else {
            var currentPage = Jt.navigation(Jt.page(DashboardPage.class).home(), Jt.page(LogoutPage.class)).use();
            currentPage.run();
        }
    }

    public class LoginPage {
        public static void main(String[] args) {
            if (Jt.button("Log in").use()) {
                Jt.sessionState().put("logged_in", Boolean.TRUE);
                Jt.rerun(true);
            }
        }
    }

    public class LogoutPage {
        public static void main(String[] args) {
            if (Jt.button("Log out").use()) {
                Jt.sessionState().put("logged_in", Boolean.FALSE);
                Jt.rerun(true);
            }
        }
    }

    public class DashboardPage {
        public static void main(String[] args) {
            Jt.title("The dashboards").use();
            Jt.text("This dashboard page is only available if the user is logged in.").use();
            Jt.markdown("*the dashboard is not implemented, this is for example purpose*").use();
        }
    }
}
