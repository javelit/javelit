///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.51.0
//DEPS ch.qos.logback:logback-classic:1.5.19

import java.nio.file.Path;

import io.javelit.core.Server;

// run with jbang App.java
// javelit hot-reload available
public class App {

    public static void main(String[] args) {
        int port = 8888;
        var server = Server.builder(Path.of("WebApp.java"), port).build();

        // start the server - this is non-blocking, user thread
        server.start();
    }
}
