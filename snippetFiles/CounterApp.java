import io.javelit.core.Jt;

public class CounterApp {
  public static void main(String[] args) {
    // initialize a counter
    Jt.sessionState().putIfAbsent("counter", 0);

    if (Jt.button("Increment").use()) {
      Jt.sessionState().computeInt("counter", (k, v) -> v + 1);
    }

    Jt.text("Counter: " + Jt.sessionState().get("counter")).use();
  }
}
