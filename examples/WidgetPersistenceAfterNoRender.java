/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.81.0

import java.util.List;

import io.javelit.core.Jt;

// contrary to Streamlit (https://github.com/streamlit/streamlit/issues/6074), Javelit can maintain state of
// widgets that are not rendered in a run.
// States are maintained if a key is provided with .key()
// This can be disabled by calling .noPersist()
public class WidgetPersistenceAfterNoRender {
  public static void main(String[] args) {
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
