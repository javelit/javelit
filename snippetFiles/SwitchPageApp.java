import io.javelit.core.Jt;

public class SwitchPageApp {
  public static void welcome() {
    Jt.title("Welcome Page").use();
    Jt.text("Please complete the requirements below to proceed:").use();

    boolean agreedToTerms = Jt.checkbox("I agree with Bob").use();
    boolean confirmedAge = Jt.checkbox("I agree with Alice").use();

    if (agreedToTerms && confirmedAge) {
      Jt.text("All requirements met! Redirecting to dashboard...").use();
      Jt.switchPage("/dashboard");
    } else {
      Jt.text("Please check both boxes to continue.").use();
    }
  }

  public static void dashboard() {
    Jt.title("Dashboard").use();
    Jt.text("Welcome to your dashboard!").use();
    Jt.text("You have successfully completed the requirements.").use();
  }

  public static void main(String[] args) {
    Jt.navigation(Jt.page("/welcome", () -> welcome()).title("Welcome").icon("👋").home(),
                  Jt.page("/dashboard", () -> dashboard()).title("Dashboard").icon("📊"))
      .hidden()
      .use();
  }
}
