///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.77.0-SNAPSHOT

import io.javelit.core.Jt;

import java.time.Duration;
import java.time.Instant;

public class SpinnerApp {

    public static void main(String[] args) {

        var overlay = Jt.toggle("owerlay").value(false).use();

        var sc = Jt.empty().key("spinner-container").use();

        var spinnerBuilder = Jt.spinner()
                .key("spinner")
                .message("**this is the spinner test**")
                .showTime(true);
        if(overlay) {
            spinnerBuilder.overlay(true).use(sc);
        }
        else {
            spinnerBuilder.use(sc);
        }

        try {
            Instant start = Instant.now();

            Thread.sleep(1000 * 5 );
            Duration duration = Duration.between(start, Instant.now());

            Jt.info("**Completed in** %ds".formatted(duration.toSeconds())).use(sc);

        } catch (InterruptedException e) {
            Jt.error( "interrupted exception");
        }
    }
}
