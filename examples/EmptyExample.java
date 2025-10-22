


/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.53.0

import java.util.List;

import io.javelit.core.Jt;

public class EmptyExample {

    public static void main(String[] args) throws InterruptedException {
        var emptyContainer = Jt.empty().use();
        for (int seconds : List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) {
            Jt.text("⏳ %s seconds have passed".formatted(seconds)).use(emptyContainer);
            Thread.sleep(1000);
        }
        Jt.text("The 10 seconds are over!").use(emptyContainer);
        Jt.button("re-run").use();
    }
}
