///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.jeamlit:jeamlit:0.49.0
//DEPS ch.qos.logback:logback-classic:1.5.19

import io.jeamlit.core.Jt;
import io.jeamlit.core.Server;

// run with jbang App.java
// run with jbang --debug App.java to get IDE hot-reload (less powerful than jeamlit hot-reload)
// see https://docs.jeamlit.io/get-started/installation/embedded-vanilla#development-with-hot-reload
public class App {

    public static void main(String[] args) {
        var server = Server.builder(WebApp.class, 8888).build();

        // start the server - this is non-blocking, user thread
        server.start();
    }

    public static class WebApp {
        public static void main(String[] args) {
            Jt.text("Hello World").use();
        }

        private WebApp() {
        }
    }

    private App() {
    }
}
