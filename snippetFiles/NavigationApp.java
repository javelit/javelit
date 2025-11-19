import io.javelit.core.Jt;

public class NavigationApp {
  public static void page1() {
    Jt.title("First Page").use();
  }

  public static void page2() {
    Jt.title("Second Page").use();
  }

  public static void main(String[] args) {
    var page = Jt.navigation(
                     Jt.page("page1", NavigationApp::page1).title("First page").icon("🔥"),
                     Jt.page("page2", NavigationApp::page2).title("Second page").icon(":favorite:"))
                 .use();
    page.run();
  }
}
