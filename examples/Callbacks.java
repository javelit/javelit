

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.53.0

import java.util.function.Consumer;import io.javelit.core.Jt;

public class Callbacks {
    public static void main(String[] args) {

        Jt.text("Written first ? ").use();

        final Consumer<Boolean> clickCallback = valueBeforeEvent -> {
            Jt.text("Written before, because executed by a callback, before everything else.").use();
            Jt.text("Old button value: " + valueBeforeEvent.toString()).use();
            final Boolean valueAfterEvent = Jt.componentsState().getBoolean("buttonKey");
            Jt.text("New button value: " + valueAfterEvent.toString()).use();
        };

        if (Jt.button("Click Me!").key("buttonKey").onClick(clickCallback).use()) {
            Jt.text("The button was clicked").use();
        }
    }
}
