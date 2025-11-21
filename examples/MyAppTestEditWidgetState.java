/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.73.0

import io.javelit.core.Jt;

public class MyAppTestEditWidgetState {

  public static void main(String[] args) {
    // TODO CYRIL implement this and make sure this works fine
    Jt.button("Say hello").key("test").use();
    if (Jt.componentsState().size() > 0) {
      Jt.text("res 1:" + Jt.componentsState().get("test").toString()).use();
      // should not be possible to edit the state of a widget once it's been run - can rely on currentSession.components(); to know what's run already
      Jt.componentsState().put("test", "lol");
      Jt.text("res 1.1:" + Jt.componentsState().get("test").toString()).use();
    }

    Jt.button("Say hello").key("test2").use();
    if (Jt.componentsState().size() > 0) {
      Jt.text("res 2:" + Jt.componentsState().get("test2").toString()).use();
    }

    // Lol
    Jt.text("hihi").use();
  }
}
