import io.javelit.core.Jt;

public class FormattedWarningApp {
  public static void main(String[] args) {
    Jt.warning("**Connection Failed**: Unable to connect to the database. Please check your settings.").use();
  }
}
