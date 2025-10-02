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
package io.jeamlit.core;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
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
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.util.Headers;
import io.undertow.util.Sessions;
import io.undertow.util.StatusCodes;
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

import static io.jeamlit.core.utils.LangUtils.optional;
import static io.jeamlit.core.utils.Preconditions.checkArgument;
import static io.jeamlit.core.utils.Preconditions.checkState;

public final class Server implements StateManager.RenderServer {
    private static final String SESSION_XSRF_ATTRIBUTE = "XSRF_TOKEN";

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    @VisibleForTesting public final int port;
    private final @Nonnull AppRunner appRunner;
    private final @Nullable FileWatcher fileWatcher;
    private final @Nonnull BuildSystem buildSystem;
    private final @Nullable Path appPath;

    private Undertow server;
    private final Map<String, WebSocketChannel> session2WsChannel = new ConcurrentHashMap<>();
    private final Map<String, String> session2Xsrf = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionRegisteredTypes = new ConcurrentHashMap<>();
    private final String customHeaders;

    private static final Mustache indexTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        indexTemplate = mf.compile("index.html.mustache");
    }

    public static final class Builder {
        final @Nullable Path appPath;
        final @Nullable Class<?> appClass;
        final int port;
        @Nullable String classpath;
        @Nullable String headersFile;
        @Nullable BuildSystem buildSystem;
        @Nullable String[] customClasspathCmdArgs;
        @Nullable String[] customCompileCmdArgs;

        private Builder(final @Nonnull Path appPath, final int port) {
            this.appPath = appPath;
            this.appClass = null;
            this.port = port;
        }

        private Builder(final @Nonnull Class<?> appClass, final int port) {
            this.appPath = null;
            this.appClass = appClass;
            this.port = port;
            this.buildSystem = BuildSystem.RUNTIME;
        }

        public Builder additionalClasspath(@Nullable String additionalClasspath) {
            this.classpath = additionalClasspath;
            return this;
        }

        public Builder headersFile(@Nullable String headersFile) {
            this.headersFile = headersFile;
            return this;
        }

        public Builder buildSystem(@Nullable BuildSystem buildSystem) {
            checkState(appClass == null, "Cannot set build system when appClass is provided directly.");
            this.buildSystem = buildSystem;
            return this;
        }

        public Builder customClasspathCmdArgs(@Nullable String[] customClasspathCmdArgs) {
            this.customClasspathCmdArgs = customClasspathCmdArgs;
            return this;
        }

        public Builder customCompileCmdArgs(@Nullable String[] customCompileCmdArgs) {
            this.customCompileCmdArgs = customCompileCmdArgs;
            return this;
        }

        public Server build() {
            if (buildSystem == null) {
                buildSystem = BuildSystem.inferBuildSystem();
            }
            return new Server(this);
        }
    }

    public static Builder builder(final @Nonnull Path appPath, final int port) {
        return new Builder(appPath, port);
    }

    public static Builder builder(final @Nonnull Class<?> appClass, final int port) {
        return new Builder(appClass, port);
    }

    private Server(final Builder builder) {
        this.port = builder.port;
        this.customHeaders = loadCustomHeaders(builder.headersFile);
        this.appRunner = new AppRunner(builder);
        this.appPath = builder.appPath;
        this.fileWatcher = builder.appPath == null ? null : new FileWatcher(builder.appPath);
        this.buildSystem = builder.buildSystem;

        // register in the state manager
        StateManager.setRenderServer(this);
    }

    public void start() {
        HttpHandler app = new PathHandler()
                .addExactPath("/_/ws", Handlers.websocket(new WebSocketHandler()))
                .addExactPath("/_/upload", new BlockingHandler(new UploadHandler()))
                // internal static files
                .addPrefixPath("/_/static",
                               resource(new ClassPathResourceManager(getClass().getClassLoader(), "static")))
                .addPrefixPath("/", new IndexHandler());
        final ResourceManager staticRm = buildStaticResourceManager();
        if (staticRm != null) {
            ((PathHandler) app).addPrefixPath("/app/static", resource(staticRm));
        }
        app = new XsrfValidationHandler(app);
        // attach a BROWSER session cookie - this is not the same as app state "session" used downstream
        app = new SessionAttachmentHandler(app,
                                           new InMemorySessionManager("jeamlit_session"),
                                           new SessionCookieConfig()
                                                   .setCookieName("jeamlit-session-id")
                                                   .setHttpOnly(true)
                                                   .setMaxAge(86400 * 7)// 7 days
                                                   .setPath("/")
                                           //make below configurable
                                           //.setSecure()
                                           //.setDomain()
        );
        server = Undertow.builder().setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 200 * 1024 * 1024L)// 200Mb
                         .addHttpListener(port, "0.0.0.0").setHandler(app).build();

        server.start();
        if (fileWatcher != null) {
            try {
                fileWatcher.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        LOG.info("Jeamlit server started on http://localhost:{}", port);
    }

    private @Nullable ResourceManager buildStaticResourceManager() {
        if (appPath != null) {
            // add static file serving
            final Path staticPath = appPath.getParent().resolve("static");
            if (Files.exists(staticPath) && Files.isDirectory(staticPath)) {
                LOG.info("Serving static files from: {}", staticPath.toAbsolutePath());
                return new PathResourceManager(staticPath, 100);
            } else {
                return null;
            }
        } else {
            LOG.info("Serving static files from resources static folder");
            return new ClassPathResourceManager(getClass().getClassLoader(), "static");
        }
    }

    public void stop() {
        if (fileWatcher != null) {
            fileWatcher.stop();
        }
        if (server != null) {
            server.stop();
        }
    }

    private static HttpHandler resource(final @Nonnull ResourceManager resourceManager) {
        return new SetHeaderHandler(new ResourceHandler(resourceManager).setDirectoryListingEnabled(false)
                                                                        .setCacheTime(3600), // 1 hour cache
                                    "X-Content-Type-Options",
                                    "nosniff");
    }

    private void notifyReload(final @Nonnull Reloader.ReloadStrategy reloadStrategy) {
        // reload the app and re-run the app for all sessions
        try {
            appRunner.reload(reloadStrategy);
        } catch (Exception e) {
            if (!(e instanceof CompilationException)) {
                LOG.error("Unknown error type: {}", e.getClass(), e);
            }
            session2WsChannel.keySet().forEach(sessionId -> sendCompilationError(sessionId, e.getMessage()));
            return;
        }

        for (final String sessionId : session2WsChannel.keySet()) {
            appRunner.runApp(sessionId);
        }
    }

    private class IndexHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) {
            // get or create session, then generate and attach XSRF token cookie
            final Session currentSession = Sessions.getOrCreateSession(exchange);
            final String xsrfToken = (String) currentSession.getAttribute(SESSION_XSRF_ATTRIBUTE);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
            exchange.getResponseSender().send(getIndexHtml(xsrfToken));
        }
    }

    // Separate XSRF validation handler
    private class XsrfValidationHandler implements HttpHandler {
        private static final String XSRF_COOKIE_KEY = "jeamlit-xsrf";

        private final HttpHandler next;

        private XsrfValidationHandler(final @Nonnull HttpHandler next) {
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            final boolean requiresXsrfValidation = Set
                    .of("POST", "PUT", "PATCH", "DELETE")
                    .contains(exchange.getRequestMethod().toString());
            final Session currentSession = Sessions.getOrCreateSession(exchange);
            if (requiresXsrfValidation) {
                // validate Xsrf Token
                final String expectedToken = (String) currentSession.getAttribute(SESSION_XSRF_ATTRIBUTE);
                if (expectedToken == null) {
                    // the session just got created - this should not happen
                    exchange.setStatusCode(StatusCodes.FORBIDDEN);
                    exchange.getResponseSender().send("Request coming from invalid session.");
                    return;
                }
                // perform XSRF validation
                final String providedToken = exchange.getRequestHeaders().getFirst("X-XSRF-TOKEN");
                final String cookieProvidedToken = optional(exchange.getRequestCookie(XSRF_COOKIE_KEY))
                        .map(Cookie::getValue)
                        .orElse(null);
                if (providedToken == null || !providedToken.equals(cookieProvidedToken) || !providedToken.equals(
                        expectedToken)) {
                    exchange.setStatusCode(StatusCodes.FORBIDDEN);
                    exchange.getResponseSender().send("Invalid XSRF token");
                    return;
                }

                final String pageSessionId = exchange.getRequestHeaders().getFirst("X-Session-ID");
                if (pageSessionId == null || !session2WsChannel.containsKey(pageSessionId)) {
                    exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
                    exchange.getResponseSender().send("Invalid session");
                    return;
                }
                // Verify XSRF token matches the one stored for this WebSocket session
                final String sessionXsrf = session2Xsrf.get(pageSessionId);
                if (sessionXsrf == null || !sessionXsrf.equals(providedToken)) {
                    exchange.setStatusCode(StatusCodes.FORBIDDEN);
                    exchange.getResponseSender().send("XSRF token mismatch");
                    return;
                }
            } else {
                // attempt to create one if need be
                if (!currentSession.getAttributeNames().contains(SESSION_XSRF_ATTRIBUTE)) {
                    final String xsrfToken = generateSecureXsrfToken();
                    currentSession.setAttribute(SESSION_XSRF_ATTRIBUTE, xsrfToken);
                    exchange.setResponseCookie(new CookieImpl(XSRF_COOKIE_KEY, xsrfToken)
                                                       .setHttpOnly(false)
                                                       .setSameSite(true)
                                                       .setPath("/")
                                                       .setMaxAge(86400 * 7));
                }
            }

            next.handleRequest(exchange); // Continue to next handler
        }
    }


    private class UploadHandler implements HttpHandler {

        private final FormParserFactory formParserFactory;

        private UploadHandler() {
            final FormParserFactory.Builder parserBuilder = FormParserFactory.builder();
            parserBuilder.setDefaultCharset(StandardCharsets.UTF_8.name());
            this.formParserFactory = parserBuilder.build();
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) {
            switch (exchange.getRequestMethod().toString()) {
                case "PUT" -> {
                    handlePuts(exchange);
                }
                case null, default -> {
                    // invalid endpoint / method
                    exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                    exchange.getResponseSender().send("bad request.");
                }
            }
        }

        private void handlePuts(HttpServerExchange exchange) {
            try (final FormDataParser parser = formParserFactory.createParser(exchange)) {
                if (parser == null) {
                    exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                    exchange.getResponseSender().send("Request is not multipart/form-data");
                    return;
                }
                final FormData formData = parser.parseBlocking();
                // either an element is a file, has fileName and fileItem, either it has value set.

                final List<JtUploadedFile> uploadedFiles = new ArrayList<>();
                for (final @Nonnull String fieldName : formData) {
                    final FormData.FormValue formValue = formData.getFirst(fieldName);
                    checkArgument(formValue.isFileItem(), "Upload form data is not a file item: %s", fieldName);
                    final FormData.FileItem fileItem = formValue.getFileItem();
                    final byte[] content;
                    try {
                        content = fileItem.getInputStream().readAllBytes();
                    } catch (IOException e) {
                        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                        exchange.getResponseSender().send("Failed to read uploaded file: " + formValue.getFileName());
                        return;
                    }
                    final JtUploadedFile f = new JtUploadedFile(formValue.getFileName(),
                                                                formValue.getHeaders().getFirst("Content-Type"),
                                                                content);
                    uploadedFiles.add(f);
                }

                // TODO NEED TO GET THE SESSION ID PROPERLY
                final String sessionId = exchange.getRequestHeaders().getFirst("X-Session-ID");
                final String componentKey = exchange.getRequestHeaders().getFirst("X-Component-Key");
                FrontendMessage componentUpdate = new FrontendMessage("component_update",
                                                                      componentKey,
                                                                      uploadedFiles,
                                                                      null,
                                                                      null);
                handleMessage(sessionId, componentUpdate);
                exchange.setStatusCode(StatusCodes.OK);
            } catch (Exception e) {
                LOG.error("Error processing file upload", e);
                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                exchange.getResponseSender().send("Upload failed:  " + e.getMessage());
            }
        }
    }

    private class WebSocketHandler implements WebSocketConnectionCallback {

        @Override
        public void onConnect(final WebSocketHttpExchange exchange, final WebSocketChannel channel) {
            final String sessionId = UUID.randomUUID().toString();
            session2WsChannel.put(sessionId, channel);
            // Send session ID to frontend immediately
            final Map<String, Object> sessionInitMessage = new HashMap<>();
            sessionInitMessage.put("type", "session_init");
            sessionInitMessage.put("sessionId", sessionId);
            sendMessage(channel, sessionInitMessage);
            final Session currentSession = getHttpSessionFromWebSocket(exchange);
            if (currentSession == null) {
                throw new RuntimeException("No session found for sessionId: " + sessionId);
            }
            final @Nullable String xsrf = (String) currentSession.getAttribute(SESSION_XSRF_ATTRIBUTE);
            if (xsrf == null) {
                throw new RuntimeException("Session did not provide a valid xsrf token: " + sessionId);
            }
            session2Xsrf.put(sessionId, xsrf);

            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                @Override
                protected void onFullTextMessage(final WebSocketChannel channel, final BufferedTextMessage message) {
                    try {
                        final String data = message.getData();
                        final FrontendMessage msg = Shared.OBJECT_MAPPER.readValue(data, FrontendMessage.class);
                        handleMessage(sessionId, msg);
                    } catch (Exception e) {
                        LOG.error("Error handling message", e);
                    }
                }

                @Override
                protected void onCloseMessage(final CloseMessage cm, final WebSocketChannel channel) {
                    session2WsChannel.remove(sessionId);
                    session2Xsrf.remove(sessionId);
                    sessionRegisteredTypes.remove(sessionId);
                    StateManager.clearSession(sessionId);
                }
            });
            // No initial render - wait for path_update message from frontend
            channel.resumeReceives();
        }

        @SuppressWarnings("StringSplitter") // see https://errorprone.info/bugpattern/StringSplitter - checking for blank string should be enough here
        private @Nullable Session getHttpSessionFromWebSocket(WebSocketHttpExchange exchange) {
            final String cookieHeader = exchange.getRequestHeader("Cookie");
            if (cookieHeader != null) {
                // Parse jeamlit-session-id cookie
                final String[] cookies = cookieHeader.isBlank() ? new String[]{} : cookieHeader.split(";");
                for (String cookie : cookies) {
                    cookie = cookie.trim();
                    if (cookie.startsWith("jeamlit-session-id=")) {
                        String sessionId = cookie.substring("jeamlit-session-id=".length());
                        SessionManager sessionManager = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
                        return sessionManager.getSession(sessionId);
                    }
                }
            }
            return null;
        }
    }

    private record FrontendMessage(@Nonnull String type,
                                   // for component_update message
                                   @Nullable String componentKey, @Nullable Object value,
                                   // for path_update message
                                   @Nullable String path, @Nullable Map<String, List<String>> queryParameters) {
    }

    private void handleMessage(final String sessionId, final FrontendMessage frontendMessage) {
        boolean doRerun = false;
        switch (frontendMessage.type()) {
            case "component_update" -> {
                doRerun = StateManager.handleComponentUpdate(sessionId,
                                                             frontendMessage.componentKey(),
                                                             frontendMessage.value());
            }
            case "reload" -> doRerun = true;
            case "path_update" -> {
                final InternalSessionState.UrlContext urlContext = new InternalSessionState.UrlContext(optional(
                        frontendMessage.path()).orElse(""),
                                                                                                       optional(
                                                                                                               frontendMessage.queryParameters()).orElse(
                                                                                                               Map.of()));
                StateManager.setUrlContext(sessionId, urlContext);
                // Trigger app execution with new URL context
                doRerun = true;
            }
            default -> LOG.warn("Unknown message type: {}", frontendMessage.type());
        }

        if (doRerun) {
            try {
                appRunner.runApp(sessionId);
            } catch (CompilationException e) {
                sendCompilationError(sessionId, e.getMessage());
            }
        }
    }

    @Override
    public void send(final @Nonnull String sessionId,
                     final @Nullable JtComponent<?> component,
                     @NotNull JtContainer container,
                     final @Nullable Integer index,
                     final boolean clearBefore) {
        // Handle component registration
        final Set<String> componentsAlreadyRegistered = sessionRegisteredTypes.computeIfAbsent(sessionId,
                                                                                               k -> new HashSet<>());
        final List<String> registrations = new ArrayList<>();

        if (component != null) {
            // note: hot reload does not work for changes in the register() method
            final String componentType = component.getClass().getName();
            if (!componentsAlreadyRegistered.contains(componentType)) {
                final @Nullable String registerCode = component.register();
                if (registerCode != null) {
                    registrations.add(registerCode);
                }
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

    @SuppressWarnings("ClassEscapesDefinedScope") // StateManager.ExecutionStatus is not meant to be public but is used as interface method param which must be public
    @Override
    public void sendStatus(final @Nonnull String sessionId, @NotNull StateManager.ExecutionStatus executionStatus) {
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "status");
        message.put("status", executionStatus);
        sendMessage(sessionId, message);
    }

    private void sendMessage(final String sessionId, final Map<String, Object> message) {
        final WebSocketChannel channel = session2WsChannel.get(sessionId);
        if (channel != null) {
            sendMessage(channel, message);
        } else {
            LOG.error("Error sending message. Unknown sessionId: {}", sessionId);
        }
    }

    private static void sendMessage(final @Nonnull WebSocketChannel channel, final Map<String, Object> message) {
        try {
            String json = Shared.OBJECT_MAPPER.writeValueAsString(message);
            WebSockets.sendText(json, channel, null);
        } catch (Exception e) {
            LOG.error("Error sending message", e);
        }
    }

    private void sendCompilationError(final String sessionId, final String error) {
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "compilation_error");
        message.put("error", error);
        sendMessage(sessionId, message);
    }

    private String getIndexHtml(final String xsrfToken) {
        final StringWriter writer = new StringWriter();
        indexTemplate.execute(writer,
                              Map.of("MATERIAL_SYMBOLS_CDN",
                                     JtComponent.MATERIAL_SYMBOLS_CDN,
                                     "LIT_DEPENDENCY",
                                     JtComponent.LIT_DEPENDENCY,
                                     "customHeaders",
                                     customHeaders,
                                     "port",
                                     port,
                                     "XSRF_TOKEN",
                                     xsrfToken,
                                     "PRISM_SETUP_SNIPPET",
                                     JtComponent.PRISM_SETUP_SNIPPET,
                                     "PRISM_CSS",
                                     JtComponent.PRISM_CSS));
        return writer.toString();
    }

    private static String generateSecureXsrfToken() {
        final SecureRandom random;
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        ;
        final byte[] bytes = new byte[32]; // 256-bit token
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
            throw new RuntimeException("Failed to read headers file from %s.".formatted(headersFile), e);
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
            if (buildSystem == BuildSystem.FATJAR_AND_JBANG || buildSystem == BuildSystem.RUNTIME) {
                directory = watchedFile.getParent();
            } else {
                directory = Paths.get("").toAbsolutePath();
            }

            LOG.info("Watching for file changes in parent directory: {}", directory);

            watcher = DirectoryWatcher.builder().path(directory).listener(event -> {
                final Path changedFile = event.path();
                // Only respond to changes to .java files in the source tree and pom.xml files
                // previously: changedFile.equals(watchedFile) to only watch the main file --> NOTE: this may be different for maven/gradle builds
                if (changedFile.getFileName().toString().endsWith(".java") || "pom.xml".equals(changedFile
                                                                                                       .getFileName()
                                                                                                       .toString())) {
                    switch (event.eventType()) {
                        case MODIFY -> {
                            LOG.info("File changed: {}. Rebuilding...", changedFile);
                            if (changedFile.equals(watchedFile)) {
                                notifyReload(Reloader.ReloadStrategy.CLASS);
                            } else {
                                notifyReload(Reloader.ReloadStrategy.BUILD_CLASSPATH_AND_CLASS);
                            }
                        }
                        case DELETE -> {
                            if (changedFile.equals(watchedFile)) {
                                LOG.warn(
                                        "The main app file {} was deleted. You may want to stop this server. If the app file is created anew, the server will attempt to load from this new file.",
                                        watchedFile);
                                session2WsChannel
                                        .keySet()
                                        .forEach(id -> sendCompilationError(id, "App file was deleted."));
                            }
                        }
                        case CREATE -> {
                            if (changedFile.equals(watchedFile)) {
                                LOG.warn("App file {} recreated. Attempting to reload from the new file.", watchedFile);
                                notifyReload(Reloader.ReloadStrategy.BUILD_CLASSPATH_AND_CLASS);
                            }
                        }
                        case OVERFLOW -> {
                            LOG.warn(
                                    "Too many file events. Some events may have been skipped or lost. If the app is not up to date, you may want to perform another edit to trigger a reload.");
                        }
                        case null, default -> LOG.warn("File changed: {} but event type is not managed: {}.",
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
