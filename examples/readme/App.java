///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.42.0

import io.jeamlit.core.Jt;

public class App {

    public static void main(String[] args) {
        double size = Jt.slider("How tall are you ? in cm").max(220).use();
        if (size > 195) {
            Jt.text("Damn, that huge!").use();
        }
    }
}
