/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.56.0

import java.util.List;

import io.javelit.core.Jt;

// even if a widget has a key, its value is reset if its configuration has changed
public class WidgetIdentity {

    public static void main(String[] args) {
        int minimum = Jt.numberInput("mini", Integer.class).minValue(0).maxValue(10).use();
        int slider1Value = Math.max(minimum, Jt.componentsState().getOrDefaultDouble("slider1", 0.).intValue());
        int slider1 = Jt.slider("slide it").key("slider1").value(slider1Value).min(minimum).use().intValue();
    }
}
