package tech.catheu.jeamlit.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.exception.CompilationException;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static tech.catheu.jeamlit.components.JsConstants.*;

public class JeamlitServer {
    private static final Logger logger = LoggerFactory.getLogger(JeamlitServer.class);
    private final int port;
    private final JeamlitAgent.HotReloader hotReloader;

    private Undertow server;
    private final Map<String, WebSocketChannel> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionRegisteredTypes = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String customHeaders;

    private static final Mustache indexTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        indexTemplate = mf.compile("index.html.mustache");
    }

    public JeamlitServer(int port, @Nullable String headersFile, @Nonnull JeamlitAgent.HotReloader hotReloader) {
        this.port = port;
        this.hotReloader = hotReloader;
        this.customHeaders = loadCustomHeaders(headersFile);
    }

    public void start() {
        final PathHandler pathHandler = Handlers.path()
                .addPrefixPath("/ws", Handlers.websocket(new WebSocketHandler()))
                // static file serving - see https://docs.streamlit.io/get-started/fundamentals/additional-features#static-file-serving
                .addPrefixPath("/static", new ResourceHandler(
                        new ClassPathResourceManager(getClass().getClassLoader(), "static")
                ))
                .addExactPath("/", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                        exchange.getResponseSender().send(getIndexHtml());
                    }
                });

        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(pathHandler)
                .build();

        server.start();
        logger.info("Jeamlit server started on http://localhost:{}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public void notifyReload() {
        // reload the app and re-run the app for all sessions
        try {
            hotReloader.reloadFile();
        } catch (CompilationException e) {
            // FIXME CYRIL - here on CompilationException failure, report the error properly
            // use sendError or something like that
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (final String sessionId : sessions.keySet()) {
            try {
                runApp(sessionId);
            } catch (Exception e) {
                logger.error("Error reloading session " + sessionId, e);
            }
        }
    }

    private class WebSocketHandler implements WebSocketConnectionCallback {
        @Override
        public void onConnect(final WebSocketHttpExchange exchange, final WebSocketChannel channel) {
            final String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, channel);

            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                @Override
                protected void onFullTextMessage(final WebSocketChannel channel, final BufferedTextMessage message) {
                    try {
                        final String data = message.getData();
                        final Map<String, Object> msg = objectMapper.readValue(data, Map.class);
                        handleMessage(sessionId, msg);
                    } catch (Exception e) {
                        logger.error("Error handling message", e);
                    }
                }

                @Override
                protected void onCloseMessage(final CloseMessage cm, final WebSocketChannel channel) {
                    sessions.remove(sessionId);
                    sessionRegisteredTypes.remove(sessionId);
                    Jt.clearSession(sessionId);
                }
            });

            channel.resumeReceives();

            try {
                hotReloader.reloadFile();
            } catch (CompilationException e) {
                // FIXME CYRIL - here on CompilationException failure, report the error properly
                // use sendError or something like that
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Send initial render
            try {
                runApp(sessionId);
            } catch (Exception e) {
                logger.error("Error in initial render", e);
                sendError(sessionId, e.getMessage());
            }
        }
    }

    private void handleMessage(final String sessionId, final Map<String, Object> message) throws Exception {
        String type = (String) message.get("type");

        if ("component_update".equals(type)) {
            final String componentKey = (String) message.get("componentKey");
            // TODO IMPLEMENT - run callbacks here - need to maintain the list of components somewhere though
            final Object value = message.get("value");

            final SessionState session = Jt.SESSIONS.get(sessionId);
            if (session != null) {
                session.getWidgetStates().put(componentKey, value);
            } else {
                throw new IllegalStateException("No session with id %s. Implementation error ?".formatted(sessionId));
            }

            // Re-run app
            runApp(sessionId);
        }

    }

    private void runApp(final String sessionId) {
        final List<JtComponent<?>> resultJtComponents = hotReloader.runApp(sessionId);
        // Collect component registrations to send to the frontend
        final Set<String> sessionRegisteredTypesForThisSession = sessionRegisteredTypes.computeIfAbsent(
                sessionId,
                k -> new HashSet<>());
        final List<String> registrations = new ArrayList<>();

        for (final JtComponent<?> component : resultJtComponents) {
            // FIXME CYRIL - would be better if we were able to identify if a Component was updated, and if so, add it to the list - useful for custom components written by the user
            //   (on the frontend side, if the component is implemented with lit, this will still mean the logic will have to support a removal / register of the component)
            final String componentType = component.getClass().getSimpleName();
            if (!sessionRegisteredTypesForThisSession.contains(componentType)) {
                registrations.add(component.register());
                sessionRegisteredTypesForThisSession.add(componentType);
            }
        }

        // Collect component HTML
        final List<String> componentsHtml = resultJtComponents.stream().map(JtComponent::render).toList();

        // Send to frontend
        final Map<String, Object> response = new HashMap<>();
        response.put("type", "render");
        response.put("html", String.join("\n", componentsHtml));
        if (!registrations.isEmpty()) {
            response.put("registrations", registrations);
        }

        // Debug output
        logger.debug("Sending response:");
        logger.debug("  Components HTML: " + componentsHtml.size());
        logger.debug("  Registrations: " + registrations.size());
        logger.debug("  HTML content: " + String.join("\n", componentsHtml));

        sendMessage(sessionId, response);
    }

    private void sendMessage(final String sessionId, final Map<String, Object> message) {
        final WebSocketChannel channel = sessions.get(sessionId);
        if (channel != null) {
            try {
                String json = objectMapper.writeValueAsString(message);
                WebSockets.sendText(json, channel, null);
            } catch (Exception e) {
                logger.error("Error sending message", e);
            }
        }
    }

    private void sendError(final String sessionId, final String error) {
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        message.put("error", error);
        sendMessage(sessionId, message);
    }


    private String getIndexHtml() {
        final StringWriter writer = new StringWriter();
        indexTemplate.execute(writer, Map.of(
                "MATERIAL_SYMBOLS_CDN", MATERIAL_SYMBOLS_CDN,
                "LIT_DEPENDENCY", LIT_DEPENDENCY,
                "customHeaders", customHeaders,
                "port", port
        ));
        return writer.toString();
    }

    private static String loadCustomHeaders(final @Nullable String headersFile) {
        if (headersFile == null) {
            return "";
        }
        final Path headerPath = Paths.get(headersFile);
        if (!Files.exists(headerPath)) {
            throw new IllegalArgumentException("Custom headers file not found: " + headersFile);
        }
        try {
            final String content = Files.readString(headerPath);
            logger.info("Loaded custom headers from {}", headersFile);
            // poor's man logic to check if the header looks valid and help the user debug in case of mistake
            // best would be to check full validity
            if (!content.replaceAll("\\s", "").startsWith("<")) {
                logger.warn("The custom headers do not start with an html tag. You may want to double check the custom headers if the frontend is not able to load. Here is the custom headers: \n{}", content);
            }
            return content;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read headers file from %s.".formatted(headersFile), e);
        }
    }
}