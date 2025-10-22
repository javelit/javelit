///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.52.0

import io.javelit.core.Jt;

public class PageLinkExample {
    public static void main(String[] args) {
        var c = Jt.container().use();

        Jt.navigation(
            Jt.page(Home.class).title("Home").home(),
            Jt.page(Page1.class).title("Page 1"),
            Jt.page(Page2.class).title("Page 2")
        ).use();

        Jt.text("Page Links Example").use(c);
        Jt.pageLink(Home.class).use(c);
        Jt.pageLink(Page1.class).use(c);
        Jt.pageLink(Page2.class).use(c);
        Jt.pageLink("https://example.com", "External Link").icon("🌐").use(c);
    }
    
    public static class Home {
        public static void main(String[] args) {
            Jt.text("Home page content").use();
        }
    }

    public static class Page1 {
        public static void main(String[] args) {
            Jt.text("The page 1 content").use();
        }
    }

    public static class Page2 {
        public static void main(String[] args) {
            Jt.text("The page 2 content").use();
        }
    }
}
