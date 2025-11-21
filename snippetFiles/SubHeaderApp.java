import io.javelit.core.Jt;

public class SubHeaderApp {
  public static void main(String[] args) {
    // Basic subheader
    Jt.subheader("This is a subheader").use();

    // Subheader with Markdown and styling
    Jt.subheader("_Javelit_ is **cool** :sunglasses:").use();
  }
}
