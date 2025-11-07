/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.67.0


import java.util.List;

import io.javelit.core.Jt;

// contrary to Streamlit (https://github.com/streamlit/streamlit/issues/6074), Javelit can maintain state of
// widgets across pages. States are maintained if a key is provided with .key()
// To clear all states of a page when the page is left, call .noPersistWhenLeft() on the page.
public class WidgetPersistenceMultiPage {
    public static void main(String[] args) {
        var page = Jt
                .navigation(Jt.page("/page1", () -> page1()).section("hehe"),
                            Jt.page("/page2", () -> page2()).noPersistWhenLeft())
                .use();
        page.run();
    }

    public static void page1() {
        Jt.title("Page 1 - Persisted values remain when the page is left").use();
        Jt.markdown("Page initialization: `Jt.page(\"/page1\", Page1::app)`").use();
        var view = Jt.radio("View", List.of("view1", "view2")).use();
        if ("view1".equals(view)) {
            Jt.text("☝️ Enter some text in the 3 inputs, then click on view2 above").use();
            Jt.textInput("Not persisted text because no key is provided:  \n`Jt.textInput(...).use();`").use();
            Jt
                    .textInput("Persisted text because a key is provided:  \n`Jt.textInput(...).key(\"text1\").use();`")
                    .key("text1")
                    .use();
            Jt
                    .textInput(
                            "Not persisted text because a key is provided with noPersist:  \n`Jt.textInput(...).key(\"text3\").noPersist().use();`")
                    .key("text3")
                    .noPersist()
                    .use();
        } else if ("view2".equals(view)) {
            Jt.text("☝️ Now go back to view1 and see if your text is still there").use();
        }
    }

    public static void page2() {
        Jt.title("Page 2 - All values are cleared when the page is left").use();
        Jt.markdown("Page initialization: `Jt.page(\"/page2\", Page2::app).noPersistWhenLeft()`").use();
        var view = Jt.radio("View", List.of("view1", "view2")).use();
        if ("view1".equals(view)) {
            Jt.text("☝️ Enter some text in the 3 inputs, then click on view2 above").use();
            Jt.textInput("Not persisted text because no key is provided:  \n`Jt.textInput(...).use();`").use();
            Jt
                    .textInput("Persisted text because a key is provided:  \n`Jt.textInput(...).key(\"text1\").use();`")
                    .key("text1")
                    .use();
            Jt
                    .textInput(
                            "Not persisted text because a key is provided with noPersist:  \n`Jt.textInput(...).key(\"text3\").noPersist().use();`")
                    .key("text3")
                    .noPersist()
                    .use();
        } else if ("view2".equals(view)) {
            Jt.text("☝️ Now go back to view1 and see if your text is still there").use();
        }
    }
}
