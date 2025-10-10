import java.util.List;

import io.jeamlit.core.Jt;

// contrary to Streamlit (https://github.com/streamlit/streamlit/issues/6074), Jeamlit can maintain state of
// widgets that are not rendered in a run.
// States are maintained if a key is provided with .key()
// This can be disabled by calling .noPersist()
public class WidgetPersistenceAfterNoRender {
    public static void main(String[] args) {
        var view = Jt.radio("View", List.of("view1", "view2")).use();
        if ("view1".equals(view)) {
            Jt.textInput("Persisted text").key("text1").use();
            Jt.textInput("Not persisted text because no user key").use();
            Jt.textInput("Not persisted text because noPersist").key("text3").noPersist().use();
            Jt.text("☝️ Enter some text, then click on view2 above").use();
        } else if ("view2".equals(view)) {
            Jt.text("☝️ Now go back to view1 and see if your text is still there").use();
        }
    }
}
