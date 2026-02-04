/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.84.0

import io.javelit.core.Jt;

public class PageLinkExample {
  public static void main(String[] args) {
    var c = Jt.container().use();

    var currentPage = Jt.navigation(
        Jt.page("/home", Home::app).title("Home").home(),
        Jt.page("/page1", Page1::app).title("Page 1"),
        Jt.page("/page2", Page2::app).title("Page 2")
    ).use();

    currentPage.run();

    Jt.text("Page Links Example").use(c);
    Jt.pageLink("/home").use(c);
    Jt.pageLink("/page1").use(c);
    Jt.pageLink("/page2").use(c);
    Jt.pageLink("https://example.com", "External Link").icon("🌐").use(c);
  }

  public static class Home {
    public static void app() {
      Jt.text("Home page content").use();
    }

    private Home() {
    }
  }

  public static class Page1 {
    public static void app() {
      Jt.text("The page 1 content").use();
    }

    private Page1() {
    }
  }

  public static class Page2 {
    public static void app() {
      Jt.text("The page 2 content").use();
    }

    private Page2() {
    }
  }
}
