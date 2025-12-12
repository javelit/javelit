///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.77.0-SNAPSHOT

import io.javelit.core.Jt;

import java.time.Duration;
import java.time.Instant;

public class SpinnerApp {

    public static void main(String[] args) {

        if( Jt.button("start spinner").use() ) {

            var spinner = Jt.spinner()
                    .message("**this is the spinner test**")
                    .use();

            final var start = Instant.now();
            try {
                Thread.sleep(1000 * 5);
                final var elapsed = Duration.between(start, Instant.now());

                Jt.info("**Completed in** %ds".formatted(elapsed.toSeconds())).use(spinner);
            } catch (InterruptedException e) {
                Jt.error("interrupted exception").use(spinner);
            }


        }
    }
}
