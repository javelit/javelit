import io.javelit.core.Jt;

public class RerunApp {
  public static void main(String[] args) {
    Jt.sessionState().computeIfAbsent("value", k -> "Title");

    // Display current value
    Jt.title(Jt.sessionState().getString("value")).use();

    if (Jt.button("Foo").use()) {
      Jt.sessionState().put("value", "Foo");
      Jt.rerun(false);
    }
  }
}
