import io.javelit.core.Jt;

public class PageLinkApp {

  public static void firstPage() {
    Jt.title("First Page").use();
    Jt.text("first page content").use();
  }

  public static void secondPage() {
    Jt.title("Second Page").use();
    Jt.text("Second page content").use();
  }

  public static void main(String[] args) {
    var page = Jt
        .navigation(Jt.page("/page1", () -> firstPage()).title("First page").icon("🔥"),
                    Jt.page("/page2", () -> secondPage()).title("Second page").icon(":favorite:"))
        .hidden()
        .use();

    Jt.divider("divider").use();
    Jt.pageLink("/page1").use();
    Jt.pageLink("/page2").use();
    Jt.pageLink("https://github.com/javelit/javelit", "Github project").icon(":link:").use();
  }
}
