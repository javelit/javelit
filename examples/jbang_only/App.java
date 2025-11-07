///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.67.0
//DEPS ch.qos.logback:logback-classic:1.5.19

import io.javelit.core.Jt;
import io.javelit.core.Server;

// run with jbang App.java
// run with jbang --debug App.java to get IDE hot-reload (less powerful than javelit hot-reload)
// see https://docs.javelit.io/get-started/installation/embedded-vanilla#development-with-hot-reload
public class App {

    public static void main(String[] args) {
        var server = Server.builder(() -> app(), 8888).build();

        // start the server - this is non-blocking, user thread
        server.start();
    }

    private static void app() {
        Jt.text("Hello World").use();
    }

    private App() {
    }
}
