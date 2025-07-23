package tech.catheu.jeamlit.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
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
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class JeamlitServer implements StateManager.RenderServer {
    private static final Logger logger = LoggerFactory.getLogger(JeamlitServer.class);
    private final int port;
    private final HotReloader hotReloader;
    private final FileWatcher fileWatcher;

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

    public JeamlitServer(final Path appPath, final String classpath, int port, @Nullable String headersFile) {
        this.port = port;
        this.hotReloader = new HotReloader(classpath, appPath);
        this.customHeaders = loadCustomHeaders(headersFile);
        this.fileWatcher = new FileWatcher(appPath);

        // register in the state manager
        StateManager.setRenderServer(this);
    }

    protected JeamlitServer(final int port,
                            final @Nullable String headersFile,
                            final HotReloader hotReloader,
                            final FileWatcher fileWatcher) {
        this.port = port;
        this.hotReloader = hotReloader;
        this.customHeaders = loadCustomHeaders(headersFile);
        this.fileWatcher = fileWatcher;
        
        // register in the state manager
        StateManager.setRenderServer(this);
    }

    public void start() {
        final PathHandler pathHandler = Handlers.path()
                .addPrefixPath("/ws", Handlers.websocket(new WebSocketHandler()))
                // static file serving - see https://docs.streamlit.io/get-started/fundamentals/additional-features#static-file-serving
                // FIXME CYRIL CLEAN THIS
                .addPrefixPath("/static",
                               new ResourceHandler(new ClassPathResourceManager(getClass().getClassLoader(),
                                                                                "static"))).addExactPath(
                        "/",
                        new HttpHandler() {
                            @Override
                            public void handleRequest(HttpServerExchange exchange) throws Exception {
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,
                                                                  "text/html");
                                exchange.getResponseSender().send(getIndexHtml());
                            }
                        });

        server = Undertow.builder().addHttpListener(port,
                                                    "0.0.0.0").setHandler(pathHandler).build();

        server.start();
        try {
            fileWatcher.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("Jeamlit server started on http://localhost:{}", port);
    }

    public void stop() {
        fileWatcher.stop();
        if (server != null) {
            server.stop();
        }
    }

    protected void notifyReload() {
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
                runAndRespond(sessionId);
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
                    StateManager.clearSession(sessionId);
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
                runAndRespond(sessionId);
            } catch (Exception e) {
                logger.error("Error in initial render", e);
                sendError(sessionId, e.getMessage());
            }
        }
    }

    private void handleMessage(final String sessionId, final Map<String, Object> message) {
        final String type = (String) message.get("type");

        if ("component_update".equals(type)) {
            final String componentKey = (String) message.get("componentKey");
            // TODO IMPLEMENT - run callbacks here - need to maintain the list of components somewhere though
            final Object value = message.get("value");

            final InternalSessionState session = StateManager.getSession(sessionId);
            if (session != null) {
                session.getComponentsState().put(componentKey, value);
                StateManager.registerCallback(sessionId, componentKey);
            } else {
                throw new IllegalStateException("No session with id %s. Implementation error ?".formatted(
                        sessionId));
            }

            // Re-run app
            runAndRespond(sessionId);
        }
    }

    private void runAndRespond(final String sessionId) {
        // Clear frontend before streaming new components
        sendClearMessage(sessionId);
        
        // Run app - components will stream individually via StateManager.addComponent()
        hotReloader.runApp(sessionId);
    }
    
    private void sendClearMessage(final String sessionId) {
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "clear");
        sendMessage(sessionId, message);
    }

    @Override
    public void send(final String sessionId, final JtComponent<?> component, final String operation) {
        // Handle component registration
        final Set<String> sessionRegisteredTypesForThisSession = sessionRegisteredTypes.computeIfAbsent(
                sessionId,
                k -> new HashSet<>());
        // note: there can only be one registration. keeping a list if micro-batching becomes a thing later
        final List<String> registrations = new ArrayList<>();
        
        final String componentType = component.getClass().getSimpleName();
        if (!sessionRegisteredTypesForThisSession.contains(componentType)) {
            registrations.add(component.register());
            sessionRegisteredTypesForThisSession.add(componentType);
        }

        // Send to frontend
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "stream");
        message.put("operation", operation);
        message.put("html", component.render());
        message.put("key", component.getKey());
        if (!registrations.isEmpty()) {
            message.put("registrations", registrations);
        }

        // Debug output
        logger.debug("Sending component update: {}", component.getKey());
        logger.debug("  Operation: {}", operation);
        logger.debug("  HTML: {}", component.render());
        logger.debug("  Registrations: {}", registrations.size());

        sendMessage(sessionId, message);
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
        indexTemplate.execute(writer,
                              Map.of("MATERIAL_SYMBOLS_CDN",
                                     JtComponent.MATERIAL_SYMBOLS_CDN,
                                     "LIT_DEPENDENCY",
                                     JtComponent.LIT_DEPENDENCY,
                                     "customHeaders",
                                     customHeaders,
                                     "port",
                                     port));
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
                logger.warn(
                        "The custom headers do not start with an html tag. You may want to double check the custom headers if the frontend is not able to load. Here is the custom headers: \n{}",
                        content);
            }
            return content;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read headers file from %s.".formatted(headersFile),
                                       e);
        }
    }

    protected class FileWatcher {
        private static final Logger LOG = LoggerFactory.getLogger(FileWatcher.class);

        private final Path watchedFile;
        private DirectoryWatcher watcher;
        private CompletableFuture<Void> watcherFuture;

        protected FileWatcher(final Path filePath) {
            this.watchedFile = filePath.toAbsolutePath();
        }

        protected void start() throws IOException {
            if (watcher != null) {
                throw new IllegalStateException("FileWatcher is already running");
            }
            final Path directory = watchedFile.getParent();

            LOG.info("Starting file watcher for: {}", watchedFile);
            LOG.info("Watching directory: {}", directory);

            watcher = DirectoryWatcher.builder().path(directory).listener(event -> {
                Path changedFile = event.path();

                // Only respond to changes to our specific file
                if (changedFile.equals(watchedFile)) {
                    if (event.eventType() == DirectoryChangeEvent.EventType.MODIFY) {
                        LOG.debug("File changed: {}", changedFile);
                        LOG.info("File changed: " + changedFile);
                        notifyReload();
                    } else {
                        // TODO CYRIL IMPLEMENT SUPPORT FOR ALL EVENT TYPES
                        LOG.warn("File changed: {} but even type is not managed: {}.", changedFile, event.eventType());
                    }
                }
            }).build();

            watcherFuture = watcher.watchAsync();
            LOG.info("File watcher started successfully");
        }

        protected void stop() {
            if (watcher != null) {
                try {
                    watcher.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (watcherFuture != null) {
                watcherFuture.cancel(true);
            }
        }
    }
}