import io.jeamlit.core.Jt;

// contrary to Streamlit (https://github.com/streamlit/streamlit/issues/6074), Jeamlit can maintain state of
// widgets across pages. States are maintained if a key is provided with .key()
// To clear all states of a page when the page is left, call .noPersistWhenLeft() on the page.
public class WidgetPersistenceMultiPage {
    public static void main(String[] args) {
        var page = Jt.navigation(Jt.page(Page1.class), Jt.page(Page2.class),
                                 Jt.page(Page3.class).noPersistWhenLeft()).use();
        page.run();
    }

    public class Page1 {
        public static void main(String[] args) {
            Jt.textInput("Some input").use();
            Jt.textInput("Some input persisted").key("key1").use();
            Jt.textInput("Some input not persisted").key("key2").noPersist().use();

            Jt.text("key1:" + Jt.componentsState().get("key1")).use();
            Jt.text("key2:" + Jt.componentsState().get("key2")).use();
        }
    }

    public class Page2 {
        public static void main(String[] args) {
            Jt.textInput("Some input").use();
            Jt.textInput("Some input persisted").key("key1").use();
            Jt.textInput("Some input not persisted").key("key2").noPersist().use();

            Jt.textInput("Other input").use();
            Jt.textInput("Other input persisted").key("key3").use();
            Jt.textInput("Other input not persisted").key("other key").noPersist().use();

            Jt.text("key1:" + Jt.componentsState().get("key1")).use();
            Jt.text("key2:" + Jt.componentsState().get("key2")).use();
            Jt.text("key3:" + Jt.componentsState().get("key3")).use();
            Jt.text("key4:" + Jt.componentsState().get("key4")).use();
        }
    }

    public class Page3 {
        public static void main(String[] args) {
            Jt.textInput("Some input").use();
            Jt.textInput("Some input persisted").key("key1").use();
            Jt.textInput("Some input not persisted").key("key2").noPersist().use();

            Jt.textInput("Other input").use();
            Jt.textInput("Other input persisted").key("key3").use();
            Jt.textInput("Other input not persisted").key("other key").noPersist().use();

            Jt.text("key1:" + Jt.componentsState().get("key1")).use();
            Jt.text("key2:" + Jt.componentsState().get("key2")).use();
            Jt.text("key3:" + Jt.componentsState().get("key3")).use();
            Jt.text("key4:" + Jt.componentsState().get("key4")).use();
        }
    }
}
