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
package io.javelit.core;

import java.io.IOException;
import java.io.StringWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;
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
import io.undertow.util.ByteRange;
import io.undertow.util.Headers;
import io.undertow.util.Sessions;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.javelit.core.utils.LangUtils.optional;

public final class Server implements StateManager.RenderServer {
    private static final String SESSION_XSRF_ATTRIBUTE = "XSRF_TOKEN";
    private static final String XSRF_COOKIE_KEY = "javelit-xsrf";

    // visible for StateManager
    static final String MEDIA_PATH = "/_/media/";
    static final String SESSION_ID_QUERY_PARAM = "sid";

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    @VisibleForTesting public final int port;
    private final @Nonnull AppRunner appRunner;
    private final @Nullable FileWatcher fileWatcher;
    private final @Nonnull BuildSystem buildSystem;
    private final @Nullable Path appPath;
    private boolean ready;

    private Undertow server;
    private final Map<String, WebSocketChannel> session2WsChannel = new ConcurrentHashMap<>();
    private final Map<String, String> session2Xsrf = new ConcurrentHashMap<>();
    private final String customHeaders;

    private static final Mustache indexTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        indexTemplate = mf.compile("index.html.mustache");
    }

    private String lastCompilationErrorMessage;

    public static final class Builder {
        final @Nullable Path appPath;
        final @Nullable Class<?> appClass;
        final @Nullable JtRunnable appRunnable;
        final int port;
        @Nullable String classpath;
        @Nullable String headersFile;
        @Nullable BuildSystem buildSystem;

        private Builder(final @Nonnull Path appPath, final int port) {
            this.appPath = appPath;
            this.appClass = null;
            this.appRunnable = null;
            this.port = port;
        }

        // use a Builder(JtRunnable appRunnable, int port) instead
        @Deprecated(forRemoval = true)
        private Builder(final @Nonnull Class<?> appClass, final int port) {
            this.appPath = null;
            this.appClass = appClass;
            this.appRunnable = null;
            this.port = port;
            this.buildSystem = BuildSystem.RUNTIME;
        }

        private Builder(final @Nonnull JtRunnable appRunnable, final int port) {
            this.appPath = null;
            this.appClass = null;
            this.appRunnable = appRunnable;
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

    // use a builder(JtRunnable app, int port) instead
    @Deprecated(forRemoval = true)
    public static Builder builder(final @Nonnull Class<?> appClass, final int port) {
        return new Builder(appClass, port);
    }

    public static Builder builder(final @Nonnull JtRunnable app, final int port) {
        return new Builder(app, port);
    }

    private Server(final Builder builder) {
        this.port = builder.port;
        this.customHeaders = loadCustomHeaders(builder.headersFile);
        this.appRunner = new AppRunner(builder, this);
        this.appPath = builder.appPath;
        this.fileWatcher = builder.appPath == null ? null : new FileWatcher(builder.appPath);
        this.buildSystem = builder.buildSystem;
        this.ready = false;
    }

    public void start() {
        HttpHandler app = new PathHandler()
                .addExactPath("/_/health", new HealthHandler())
                .addExactPath("/_/ready", new ReadyHandler())
                .addExactPath("/_/ws",
                              Handlers.websocket(new WebSocketHandler()).addExtension(new PerMessageDeflateHandshake()))
                .addExactPath("/_/upload", new BlockingHandler(new UploadHandler()))
                .addPrefixPath(MEDIA_PATH, new BlockingHandler(new MediaHandler()))
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
                                           new InMemorySessionManager("javelit_session"),
                                           new SessionCookieConfig()
                                                   .setCookieName("javelit-session-id")
                                                   .setHttpOnly(true)
                                                   .setMaxAge(86400 * 7)// 7 days
                                                   .setPath("/")
                                           //make below configurable
                                           //.setSecure()
                                           //.setDomain()
        );
        server = Undertow.builder().setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 200 * 1024 * 1024L)// 200Mb
                         .addHttpListener(port, "0.0.0.0").setHandler(app).build();

        try {
            server.start();
        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof BindException) {
                // yes this is not good practice to match on string but dev experience is important for this one
                if (e.getMessage().contains("Address already in use")) {
                    throw new RuntimeException(
                            "Failed to launch the server. Port %s is already in use ? Try changing the port. In standalone mode, use --port <PORT>".formatted(
                                    port),
                            e);
                }
            }
            throw e;
        }
        if (fileWatcher != null) {
            try {
                fileWatcher.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ready = true;
        LOG.info("Javelit server started on http://localhost:{}", port);
    }

    private @Nullable ResourceManager buildStaticResourceManager() {
        if (appPath != null) {
            // add static file serving
            final Path staticPath = appPath.toAbsolutePath().getParent().resolve("static");
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

    private void notifyReload() {
        // reload the app and re-run the app for all sessions
        try {
            appRunner.reload();
            lastCompilationErrorMessage = null;
        } catch (Exception e) {
            if (!(e instanceof CompilationException)) {
                LOG.error("Unknown error type: {}", e.getClass(), e);
            }
            lastCompilationErrorMessage = e.getMessage();
            session2WsChannel
                    .keySet()
                    .forEach(sessionId -> sendCompilationError(sessionId, lastCompilationErrorMessage));
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
            final boolean devMode = isLocalClient(exchange.getSourceAddress());
            exchange.getResponseSender().send(getIndexHtml(xsrfToken, devMode));
        }
    }

    // Separate XSRF validation handler
    private class XsrfValidationHandler implements HttpHandler {

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

    private static class HealthHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) {
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("OK");
        }
    }

    private class ReadyHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            if (ready) {
                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseSender().send("OK");
            } else {
                exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
                exchange.getResponseSender().send("Service Unavailable");
            }
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

    private class MediaHandler implements HttpHandler {

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod().toString())) {
                handleGets(exchange);
            } else {
                // invalid endpoint / method
                exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                exchange.getResponseSender().send("bad request.");
            }
        }

        private void handleGets(final @Nonnull HttpServerExchange exchange) {
            // security checks - it's not possible to read a media from a different xsrf token
            final String sessionId = optional(exchange.getQueryParameters().get(SESSION_ID_QUERY_PARAM))
                    .map(Deque::getFirst)
                    .orElse(null);
            if (sessionId == null) {
                exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                exchange.getResponseSender().send("Missing session ID");
                return;
            }
            final String cookieProvidedToken = optional(exchange.getRequestCookie(XSRF_COOKIE_KEY))
                    .map(Cookie::getValue)
                    .orElse(null);

            final String sessionXsrf = session2Xsrf.get(sessionId);
            if (sessionXsrf == null || !sessionXsrf.equals(cookieProvidedToken)) {
                exchange.setStatusCode(StatusCodes.FORBIDDEN);
                exchange.getResponseSender().send("XSRF token mismatch");
                return;
            }

            final String hash = exchange.getRelativePath().substring(1);
            final MediaEntry media = StateManager.getMedia(sessionId, hash);
            writeResponse(exchange, media, hash);
        }

        private void writeResponse(final @NotNull HttpServerExchange exchange,
                                   final @Nullable MediaEntry media,
                                   final @Nonnull String hash) {
            if (media == null || hash.isBlank()) {
                exchange.setStatusCode(StatusCodes.NOT_FOUND);
                return;
            }
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, media.format());
            exchange.getResponseHeaders().put(Headers.ACCEPT_RANGES, "bytes");
            exchange.getResponseHeaders().put(Headers.ETAG, hash);
            exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "private, max-age=3600, must-revalidate");
            final byte[] data = media.bytes();
            final String rangeHeader = exchange.getRequestHeaders().getFirst(Headers.RANGE);
            final ByteRange range = ByteRange.parse(rangeHeader);
            if (range == null) {
                // no range header, or invalid / unsupported range format (e.g., multi-range)
                writeFullContent(exchange, data);
                return;
            }
            final ByteRange.RangeResponseResult result = range.getResponseResult(
                    data.length,
                    exchange.getRequestHeaders().getFirst(Headers.IF_RANGE),
                    null, // lastModified
                    hash
            );
            if (result.getStatusCode() == StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE) {
                exchange.setStatusCode(StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE);
                exchange.getResponseHeaders().put(Headers.CONTENT_RANGE, "bytes */" + data.length);
                return;
            }
            if (result.getStatusCode() == StatusCodes.OK) {
                // Range not satisfiable for some reason (e.g., If-Range mismatch) - serve full content
                writeFullContent(exchange, data);
                return;
            }
            // Handle partial content (206)
            final long start = result.getStart();
            final long end = result.getEnd();
            final int length = (int) (end - start + 1);
            exchange.setStatusCode(StatusCodes.PARTIAL_CONTENT);
            exchange.getResponseHeaders().put(Headers.CONTENT_RANGE, result.getContentRange());
            exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, length);
            exchange.getResponseSender().send(ByteBuffer.wrap(data, (int) start, length));
        }

        private void writeFullContent(final @Nonnull HttpServerExchange exchange, final byte[] data) {
            exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, data.length);
            exchange.getResponseSender().send(ByteBuffer.wrap(data));
        }
    }

    private static boolean isLocalClient(final @Nullable InetSocketAddress inetSocketAddress) {
        try {
            return optional(inetSocketAddress)
                    .map(InetSocketAddress::getAddress)
                    .map(InetAddress::getHostAddress)
                    .map(ip -> Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1").contains(ip))
                    .orElse(false);
        } catch (Exception e) {
            LOG.warn(
                    "Failed to determine whether client is local. Assuming client is not local. dev features will not be activated for this client.",
                    e);
            return false;
        }
    }

    private class WebSocketHandler implements WebSocketConnectionCallback {

        @Override
        public void onConnect(final WebSocketHttpExchange exchange, final WebSocketChannel channel) {
            final String sessionId = UUID.randomUUID().toString();
            session2WsChannel.put(sessionId, channel);
            if (isLocalClient(channel.getSourceAddress())) {
                StateManager.registerDeveloperSession(sessionId);
            }

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
                    StateManager.clearSession(sessionId);
                }
            });
            // No initial render - wait for path_update message from frontend
            channel.resumeReceives();
        }

        @SuppressWarnings("StringSplitter")
        // see https://errorprone.info/bugpattern/StringSplitter - checking for blank string should be enough here
        private @Nullable Session getHttpSessionFromWebSocket(WebSocketHttpExchange exchange) {
            final String cookieHeader = exchange.getRequestHeader("Cookie");
            if (cookieHeader != null) {
                // Parse javelit-session-id cookie
                final String[] cookies = cookieHeader.isBlank() ? new String[]{} : cookieHeader.split(";");
                for (String cookie : cookies) {
                    cookie = cookie.trim();
                    if (cookie.startsWith("javelit-session-id=")) {
                        String sessionId = cookie.substring("javelit-session-id=".length());
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
        try {
            switch (frontendMessage.type()) {
                case "component_update" -> {
                    doRerun = StateManager.handleComponentUpdate(sessionId,
                                                                 frontendMessage.componentKey(),
                                                                 frontendMessage.value());
                }
                case "reload" -> doRerun = true;
                case "path_update" -> {
                    final UrlContext urlContext = new UrlContext(optional(
                            frontendMessage.path()).orElse(""),
                                                                 optional(
                                                                         frontendMessage.queryParameters()).orElse(
                                                                         Map.of()));
                    StateManager.setUrlContext(sessionId, urlContext);
                    // Trigger app execution with new URL context
                    doRerun = true;
                }
                case "clear_cache" -> {
                    // only allow cache clearing from localhost
                    if (StateManager.isDeveloperSession(sessionId)) {
                        StateManager.developerReset();
                        LOG.info("Cache cleared by developer user request from localhost");
                        doRerun = true;
                    } else {
                        LOG.warn("clear_cache request rejected from non-localhost session: {}", sessionId);
                    }
                }
                default -> LOG.warn("Unknown message type: {}", frontendMessage.type());
            }
        } catch (Exception e) {
            // log because it's really unexpected
            LOG.error("Error handling client message", e);
            sendFullScreenModalError(sessionId,
                                     "Client message processing error",
                                     "The server was not able to process the client message. Please reach out to support if this error is unexpected.",
                                     e.getMessage(),
                                     true);
        }


        if (doRerun) {
            if (lastCompilationErrorMessage != null) {
                sendCompilationError(sessionId, lastCompilationErrorMessage);
            } else {
                try {
                    appRunner.runApp(sessionId);
                    lastCompilationErrorMessage = null;
                } catch (CompilationException e) {
                    lastCompilationErrorMessage = e.getMessage();
                    sendCompilationError(sessionId, e.getMessage());
                }
            }
        }
    }

    @Override
    public void send(final @Nonnull String sessionId,
                     final @Nullable String renderHtml,
                     final @Nullable String registrationHtml,
                     final @NotNull JtContainer container,
                     final @Nullable Integer index,
                     final boolean clearBefore) {
        // Send message to frontend
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "delta");
        message.put("html", renderHtml);
        message.put("container", container.frontendDataContainerField());
        if (index != null) {
            message.put("index", index);
        }
        if (clearBefore) {
            message.put("clearBefore", true);
        }
        if (registrationHtml != null && !registrationHtml.isBlank()) {
            message.put("registrations", List.of(registrationHtml));
        }
        LOG.debug("Sending delta to session {}: {}", sessionId, message);
        sendMessage(sessionId, message);
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    // StateManager.ExecutionStatus is not meant to be public but is used as interface method param which must be public
    @Override
    public void sendStatus(final @Nonnull String sessionId, @NotNull StateManager.ExecutionStatus executionStatus,
                           final @Nullable Map<String, Integer> unusedComponents) {
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "status");
        message.put("status", executionStatus);
        if (StateManager.isDeveloperSession(sessionId) && unusedComponents != null && !unusedComponents.isEmpty()) {
            message.put("toastDuration", 10);
            final List<String> unusedComponentsListItems = unusedComponents.entrySet().stream()
                                                                           .map(e -> unusedComponentToMarkdownLi(e.getKey(),
                                                                                                                 e.getValue()))
                                                                           .toList();
            final @Language("markdown") String toastBody = """
                    The following components were created but never used: \s
                    %s
                    
                    Did you forget to call `.use()`? \s
                    
                    <sup>_This message only appears in **Dev Mode**_</sup>
                    """.formatted(String.join("\n", unusedComponentsListItems));
            message.put("toastBody", MarkdownUtils.markdownToHtml(toastBody, false));
            message.put("toastIcon", ":warning:");
        }
        sendMessage(sessionId, message);
    }

    private static String unusedComponentToMarkdownLi(final String name, final Integer unusedCount) {
        final String userFriendlyName = name.substring(name.lastIndexOf(".") + 1).replace("Component", "");
        return "- " + userFriendlyName + " - _" + unusedCount + "_";

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

    private void sendFullScreenModalError(final String sessionId,
                                          final String title,
                                          final String paragraph,
                                          final String error,
                                          final boolean closable) {
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "modal_error");
        message.put("title", title);
        message.put("paragraph", paragraph);
        message.put("error", error);
        message.put("closable", closable);
        sendMessage(sessionId, message);
    }

    private void sendCompilationError(final String sessionId, final String error) {
        sendFullScreenModalError(sessionId, "Compilation error",
                                 "Fix the compilation errors below and save the file to continue:",
                                 error, false);
    }

    private String getIndexHtml(final String xsrfToken, boolean devMode) {
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
                                     JtComponent.PRISM_CSS,
                                     "DEV_MODE",
                                     devMode));
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

            watcher = DirectoryWatcher
                    .builder()
                    .path(directory)
                    .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                    .listener(event -> {
                        final Path changedFile = event.path();
                        // Only respond to changes to .java files in the source tree
                        if (changedFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".java")) {
                            switch (event.eventType()) {
                                case MODIFY -> {
                                    LOG.info("File changed: {}. Rebuilding...", changedFile);
                                    notifyReload();
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
                                        LOG.warn("App file {} recreated. Attempting to reload from the new file.",
                                                 watchedFile);
                                        notifyReload();
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

            LOG.info("Initializing file watch in parent directory: {}", directory);
            // see https://github.com/gmethvin/directory-watcher/issues/102 - the first step of watchAsync is actually blocking
            // and may be too long if the user started javelit in a parent folder with many files or with files in cloud
            final CompletableFuture<CompletableFuture<Void>> watcherFutureWrapper = CompletableFuture.supplyAsync(() -> watcher.watchAsync());
            try {
                watcherFuture = watcherFutureWrapper.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException(
                        "Initializing file watch timed out after 10 seconds. Try to run the javelit app in a parent directory with less files. Also, do not run the javelit app in a parent directory that contains Cloud files (iCloud, Dropbox, etc...).",
                        e);
            } catch (InterruptedException e) {
                throw new RuntimeException("Initializing file watch was interrupted.", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Initializing file watch failed.", e);
            }
            LOG.info("File watch started successfully");
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
