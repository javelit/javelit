import io.javelit.core.Jt;

public class FormattedErrorApp {
  public static void main(String[] args) {
    Jt.error("**Connection Failed**: Unable to connect to the database. Please check your settings.").use();
  }
}
