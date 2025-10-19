///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.49.0

import io.jeamlit.core.Jt;

public class Sleeping {
    public static void main(String[] args) throws InterruptedException {

        for (int i = 0; i < 10; i++) {
            Jt.text("Let me sleep for 1 sec").key(String.valueOf(i)).use();
            Thread.sleep(1000);
        }
    }
}
