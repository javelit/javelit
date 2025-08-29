/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.catheu.jeamlit.core;

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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.methvin.watcher.DirectoryWatcher;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
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

import static tech.catheu.jeamlit.core.utils.LangUtils.optional;

public class Server implements StateManager.RenderServer {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    @VisibleForTesting
    public final int port;
    private final HotReloader hotReloader;
    private final FileWatcher fileWatcher;

    private Undertow server;
    private final Map<String, WebSocketChannel> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionRegisteredTypes = new ConcurrentHashMap<>();
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

    protected Server(final int port, final @Nullable String headersFile, final HotReloader hotReloader, final FileWatcher fileWatcher) {
        this.port = port;
        this.hotReloader = hotReloader;
        this.customHeaders = loadCustomHeaders(headersFile);
        this.fileWatcher = fileWatcher;

        // register in the state manager
        StateManager.setRenderServer(this);
    }

    public void start() {
        // Create custom fallback handler for SPA routing
        final WebSocketHandler sessionHandler = new WebSocketHandler();
        final HttpHandler spaHandler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                final String path = exchange.getRequestPath();
                // WebSocket connections
                if (path.startsWith("/ws")) {
                    Handlers.websocket(sessionHandler).handleRequest(exchange);
                    return;
                }
                // static files
                if (path.startsWith("/static")) {
                    new ResourceHandler(new ClassPathResourceManager(getClass().getClassLoader(),
                                                                     "static"))
                            .handleRequest(exchange);
                    return;
                }
                // All other paths - serve the main app (SPA routing)
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                exchange.getResponseSender().send(getIndexHtml());
            }
        };

        server = Undertow.builder().addHttpListener(port, "0.0.0.0").setHandler(spaHandler).build();

        server.start();
        try {
            fileWatcher.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Jeamlit server started on http://localhost:{}", port);
    }

    public void stop() {
        fileWatcher.stop();
        if (server != null) {
            server.stop();
        }
    }

    protected void notifyReload(final  @Nonnull HotReloader.ReloadStrategy reloadStrategy) {
        // reload the app and re-run the app for all sessions
        try {
            hotReloader.reloadFile(reloadStrategy);
        } catch (Exception e) {
            if (!(e instanceof CompilationException)) {
                LOG.error("Unknown error type: {}", e.getClass(), e);
            }
            sessions.keySet().forEach(sessionId -> sendCompilationError(sessionId, e.getMessage()));
            return;
        }

        for (final String sessionId : sessions.keySet()) {
            hotReloader.runApp(sessionId);
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
                        final Message msg = Shared.OBJECT_MAPPER.readValue(data, Message.class);
                        handleMessage(sessionId, msg);
                    } catch (Exception e) {
                        LOG.error("Error handling message", e);
                    }
                }

                @Override
                protected void onCloseMessage(final CloseMessage cm, final WebSocketChannel channel) {
                    sessions.remove(sessionId);
                    sessionRegisteredTypes.remove(sessionId);
                    StateManager.clearSession(sessionId);
                }
            });
            // No initial render - wait for path_update message from frontend
            channel.resumeReceives();
        }
    }


    private record Message(@Nonnull String type,
                           @Nullable String componentKey, @Nullable Object value,
                           // for component_update mesages
                           @Nullable String path,
                           @Nullable Map<String, List<String>> queryParameters
    ) {
    }

    private void handleMessage(final String sessionId, final Message message) {
        boolean doRerun = false;
        switch (message.type()) {
            case "component_update" -> {
                doRerun = StateManager.handleComponentUpdate(sessionId,
                                                             message.componentKey(),
                                                             message.value());
            }
            case "reload" -> doRerun = true;
            case "path_update" -> {
                final InternalSessionState.UrlContext urlContext = new InternalSessionState.UrlContext(
                        optional(message.path()).orElse(""),
                        optional(message.queryParameters()).orElse(Map.of()));
                StateManager.setUrlContext(sessionId, urlContext);
                // Trigger app execution with new URL context
                doRerun = true;
            }
            default -> LOG.warn("Unknown message type: {}", message.type());
        }

        if (doRerun) {
            try {
                hotReloader.runApp(sessionId);
            } catch (CompilationException e) {
                sendCompilationError(sessionId, e.getMessage());
            }
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
        LOG.debug("Sending delta: index={}, clearBefore={}, component={}",
                  index,
                  clearBefore,
                  component != null ? component.getKey() : null);
        LOG.debug("  HTML: {}", component != null ? component.render() : null);
        LOG.debug("  Registrations: {}", registrations.size());
        sendMessage(sessionId, message);
    }

    private void sendMessage(final String sessionId, final Map<String, Object> message) {
        final WebSocketChannel channel = sessions.get(sessionId);
        if (channel != null) {
            try {
                String json = Shared.OBJECT_MAPPER.writeValueAsString(message);
                WebSockets.sendText(json, channel, null);
            } catch (Exception e) {
                LOG.error("Error sending message", e);
            }
        }
    }

    private void sendCompilationError(final String sessionId, final String error) {
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "compilation_error");
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
            LOG.info("Loaded custom headers from {}", headersFile);
            // poor's man logic to check if the header looks valid and help the user debug in case of mistake
            // best would be to check full validity
            if (!content.replaceAll("\\s", "").startsWith("<")) {
                LOG.warn(
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
            final Path directory;
            if (hotReloader.buildSystem == HotReloader.BuildSystem.VANILLA) {
                directory = watchedFile.getParent();
            } else {
                directory = Paths.get("").toAbsolutePath();
            }

            LOG.info("Watching for file changes in parent directory: {}", directory);

            watcher = DirectoryWatcher.builder().path(directory).listener(event -> {
                final Path changedFile = event.path();
                // Only respond to changes to .java files in the source tree and pom.xml files
                // previously: changedFile.equals(watchedFile) to only watch the main file --> NOTE: this may be different for maven/gradle builds
                if (changedFile.getFileName().toString().endsWith(".java") || changedFile.getFileName().toString().equals("pom.xml")) {
                    switch (event.eventType()) {
                        case MODIFY -> {
                            LOG.info("File changed: {}. Rebuilding...", changedFile);
                            if (changedFile.equals(watchedFile)) {
                                notifyReload(HotReloader.ReloadStrategy.CLASS);
                            } else {
                                notifyReload(HotReloader.ReloadStrategy.BUILD_CLASSPATH_AND_CLASS);
                            }
                        }
                        case DELETE -> {
                            if (changedFile.equals(watchedFile)) {
                                LOG.warn(
                                        "The main app file {} was deleted. You may want to stop this server. If the app file is created anew, the server will attempt to load from this new file.",
                                        watchedFile);
                                sessions.keySet().forEach(id -> sendCompilationError(id,
                                                                                     "App file was deleted."));
                            }
                        }
                        case CREATE -> {
                            if (changedFile.equals(watchedFile)) {
                                LOG.warn(
                                        "App file {} recreated. Attempting to reload from the new file.",
                                        watchedFile);
                                notifyReload(HotReloader.ReloadStrategy.BUILD_CLASSPATH_AND_CLASS);
                            }
                        }
                        case OVERFLOW -> {
                            LOG.warn("Too many file events. Some events may have been skipped or lost. If the app is not up to date, you may want to perform another edit to trigger a reload.");
                        }
                        case null, default ->
                                LOG.warn("File changed: {} but event type is not managed: {}.",
                                         changedFile,
                                         event.eventType());
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
