import io.javelit.core.Jt;

public class HeaderApp {
  public static void main(String[] args) {
    // Basic header
    Jt.header("This is a title").use();

    // Header with Markdown and styling
    Jt.header("_Javelit_ is **cool** :sunglasses:").use();
  }
}
