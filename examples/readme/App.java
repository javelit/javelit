///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.53.0

import io.javelit.core.Jt;

public class App {

    public static void main(String[] args) {
        double size = Jt.slider("How tall are you ? in cm").max(220).use();
        if (size > 195) {
            Jt.text("Damn, that huge!").use();
        }
    }

    private App() {
    }
}
