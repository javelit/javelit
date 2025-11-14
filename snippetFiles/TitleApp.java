import io.javelit.core.Jt;

public class TitleApp {
  public static void main(String[] args) {
    // Basic title
    Jt.title("This is a title").use();

    // Title with Markdown and styling
    Jt.title("_Javelit_ is **cool** :sunglasses:").use();
  }
}
