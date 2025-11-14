import io.javelit.core.Jt;

public class ButtonApp {
  public static void main(String[] args) {
    if (Jt.button("Say hello").use()) {
      Jt.text("Why hello there").use();
    } else {
      Jt.text("Goodbye").use();
    }
  }
}
