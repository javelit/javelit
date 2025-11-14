import io.javelit.core.Jt;

public class HtmlApp {
  public static void main(String[] args) {
    Jt.html("<h3>Custom HTML Header</h3>").use();
    Jt.html("<p style='color: blue;'>This is blue text</p>").use();
    Jt.html("<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>").use();
  }
}
