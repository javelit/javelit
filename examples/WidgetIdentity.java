/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.32.0

import java.util.List;

import io.jeamlit.core.Jt;

// even if a widget has a key, its value is reset if its configuration has changed
public class WidgetIdentity {

    public static void main(String[] args) {
        int minimum = Jt.numberInput("mini", Integer.class).minValue(0).maxValue(10).use();
        int slider1 = Jt.slider("no key").min(minimum).use().intValue();
        Jt.text("keyed value before: " + String.valueOf(Jt.componentsState().get("key1"))).use();
        int slider2 = Jt.slider("with key").key("key1").min(minimum).use().intValue();
        Jt.text("keyed value after: " + String.valueOf(Jt.componentsState().get("key1"))).use();
    }
}
