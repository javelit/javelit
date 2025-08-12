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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server implements StateManager.RenderServer {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    @VisibleForTesting
    public final int port;
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

    public Server(final Path appPath, final String classpath, int port, @Nullable String headersFile) {
        this.port = port;
        this.hotReloader = new HotReloader(classpath, appPath);
        this.customHeaders = loadCustomHeaders(headersFile);
        this.fileWatcher = new FileWatcher(appPath);

        // register in the state manager
        StateManager.setRenderServer(this);
    }

    protected Server(final int port,
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
                hotReloader.runApp(sessionId);
            } catch (Exception e) {
                logger.error("Error reloading session " + sessionId, e);
            }
        }
    }

    private class WebSocketHandler implements WebSocketConnectionCallback {

        private final AtomicBoolean neverLoaded = new AtomicBoolean(true);
        private final Semaphore reloadAvailable = new Semaphore(1, true);

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
                if (neverLoaded.get()) {
                    reloadAvailable.acquire();
                    if (neverLoaded.get()) {
                        logger.warn("Compiling the app for the first time.");
                        hotReloader.reloadFile();
                        neverLoaded.set(false);
                    }
                }
            } catch (CompilationException e) {
                // FIXME CYRIL - here on CompilationException failure, report the error properly
                // use sendError or something like that
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                reloadAvailable.release();
            }

            // Send initial render
            try {
                hotReloader.runApp(sessionId);
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

            final boolean doRerun = StateManager.handleComponentUpdate(sessionId, componentKey, value);
            if (doRerun) {
                hotReloader.runApp(sessionId);
            }
        } else if ("reload".equals(type)) {
            hotReloader.runApp(sessionId);
        }
    }

    @Override
    public void send(final @Nonnull String sessionId, final @Nullable JtComponent<?> component, @NotNull JtContainer container, final @Nullable Integer index, final boolean clearBefore) {
        // Handle component registration
        final Set<String> componentsAlreadyRegistered = sessionRegisteredTypes.computeIfAbsent(
                sessionId,
                k -> new HashSet<>());
        final List<String> registrations = new ArrayList<>();

        if (component != null) {
            // note: hot reload does not work for changes in the register() method
            final String componentType = component.getClass().getName();
            if (!componentsAlreadyRegistered.contains(componentType)) {
                registrations.add(component.register());
                componentsAlreadyRegistered.add(componentType);
            }
        }

        // Send message to frontend
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "delta");
        message.put("html", component != null ? component.render() : null);
        message.put("container", container.frontendDataContainerField());
        if (index != null) {
            message.put("index", index);
        }
        if (clearBefore) {
            message.put("clearBefore", true);
        }
        if (!registrations.isEmpty()) {
            message.put("registrations", registrations);
        }
        logger.debug("Sending delta: index={}, clearBefore={}, component={}", index, clearBefore, component != null ? component.getKey() : null);
        logger.debug("  HTML: {}", component != null ? component.render() : null);
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
                final Path changedFile = event.path();

                // Only respond to changes to our specific file
                if (changedFile.equals(watchedFile)) {
                    if (event.eventType() == DirectoryChangeEvent.EventType.MODIFY) {
                        LOG.debug("File changed: {}", changedFile);
                        LOG.info("File changed: " + changedFile);
                        LOG.debug("Re-compiling because of file event");
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