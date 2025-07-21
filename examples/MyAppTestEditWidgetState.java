package tech.catheu.jeamlit.cli;

import tech.catheu.jeamlit.core.Jt;

public class MyAppTestEditWidgetState {

    public static void main(String[] args) {
        // TODO CYRIL implement this and make sure this works fine
        Jt.use(Jt.button("Say hello").key("test"));
        if (Jt.sessionStateDebug().getWidgetStates().size() > 0) {
            Jt.use(Jt.text("res 1:" + Jt.sessionStateDebug().getWidgetStates().get("test").toString()));
            // should not be possible to edit the state of a widget once it's been run - can rely on currentSession.components(); to know what's run already
            Jt.sessionStateDebug().getWidgetStates().put("test", "lol");
            Jt.use(Jt.text("res 1.1:" + Jt.sessionStateDebug().getWidgetStates().get("test").toString()));
        }

        Jt.use(Jt.button("Say hello").key("test2"));
        if (Jt.sessionStateDebug().getWidgetStates().size() > 0) {
            Jt.use(Jt.text("res 2:" + Jt.sessionStateDebug().getWidgetStates().get("test2").toString()));
        }

        // Lol
        Jt.use(Jt.text("hihi"));
    }
}