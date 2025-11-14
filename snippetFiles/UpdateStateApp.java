import io.javelit.core.Jt;

public class UpdateStateApp {
  public static void main(String[] args) {
    String name = Jt.textInput("Name").key("name").use();
    Jt.button("Clear name")
      .onClick(b -> Jt.setComponentState("name", ""))
      .use();
    Jt.text("Hello " + name).use();
  }
}
