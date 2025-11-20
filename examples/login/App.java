import io.javelit.core.Jt;

public class App {

  public static void main(String[] args) {
    boolean loggedIn = Jt.sessionState().computeIfAbsentBoolean("logged_in", k -> false);

    if (!loggedIn) {
      var currentPage = Jt.navigation(Jt.page("/login", () -> login())).hidden().use();
      currentPage.run();
    } else {
      var currentPage = Jt
          .navigation(Jt.page("/dashboard", () -> dashboard()).home(), Jt.page("/logout", () -> logout()))
          .use();
      currentPage.run();
    }
  }

  public static void login() {
    if (Jt.button("Log in").use()) {
      Jt.sessionState().put("logged_in", Boolean.TRUE);
      Jt.rerun(true);
    }
  }


  public static void logout() {
    if (Jt.button("Log out").use()) {
      Jt.sessionState().put("logged_in", Boolean.FALSE);
      Jt.rerun(true);
    }
  }

  public static void dashboard() {
    Jt.title("The dashboards").use();
    Jt.text("This dashboard page is only available if the user is logged in.").use();
    Jt.markdown("*the dashboard is not implemented, this is for example purpose*").use();
  }

  private App() {
  }
}
