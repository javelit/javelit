///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.77.0-SNAPSHOT

import io.javelit.core.Jt;

import java.time.Duration;
import java.time.Instant;

public class SpinnerApp {

    public static void main(String[] args) {

        var overlay = Jt.toggle("overlay").value(false).use();

        Jt.divider("hr1").use();

        var overlay = Jt.toggle("overlay").value(false).use();

        Jt.divider("hr1").use();

        if( Jt.button("show spinner (use case 1)").use() ) {

            SpinnerComponent.builder()
                    .message("**this is the spinner test**")
                    .showTime(true)
                    .onStart(() -> {
                        try {
                            Thread.sleep(1000 * 5);
                        } catch (InterruptedException e) {
                            Jt.error("interrupted exception");
                        }
                        return "my result";
                    })
                    .onComplete((result, elapsed) ->
                            Jt.info("**Completed in** %ds".formatted(elapsed.toSeconds())))
                    .overlay(overlay)
                    .use();
        }

        if( Jt.button("show spinner (use case 2)").use() ) {

            var spinner = SpinnerComponent.builder()
                    .message("**this is the spinner test**")
                    .showTime(true)
                    .overlay(overlay)
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
