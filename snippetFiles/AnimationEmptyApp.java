import io.javelit.core.Jt;

public class AnimationEmptyApp {
  public static void main(String[] args) throws InterruptedException {
    var emptyContainer = Jt.empty().use();
    for (int i = 10; i >= 1; i--) {
      Jt.text(i + "!").use(emptyContainer);
      Thread.sleep(1000);
    }
    Jt.text("Happy new Year !").use(emptyContainer);
    Jt.button("rerun").use();
  }
}
