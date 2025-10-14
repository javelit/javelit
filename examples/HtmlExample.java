///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.44.0

import io.jeamlit.core.Jt;

public class HtmlExample {
    public static void main(String[] args) {
        Jt.html("<h2>Hello HTML</h2><p>This is a <strong>test</strong>!</p>").use();
    }
}
