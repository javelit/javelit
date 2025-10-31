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
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static io.javelit.core.Shared.MEDIA_PATH;
import static io.javelit.core.Shared.SESSION_ID_QUERY_PARAM;

/**
 * Framework-agnostic HTTP and WebSocket handler for Javelit applications.
 * Contains all business logic for handling requests, independent of the underlying
 * server framework (Undertow, Servlet, etc.).
 */
public class JavelitHandler implements StateManager.RenderServer {

    private static final String SESSION_XSRF_ATTRIBUTE = "XSRF_TOKEN";
    private static final String XSRF_COOKIE_KEY = "javelit-xsrf";
    private static final Logger LOG = LoggerFactory.getLogger(JavelitHandler.class);

    private final AppRunner appRunner;
    private final Map<String, WebSocketSession> session2WsChannel = new ConcurrentHashMap<>();
    private final Map<String, String> session2Xsrf = new ConcurrentHashMap<>();
    private final String customHeaders;
    private final int port;
    private final @Nullable Path appPath;
    private final @Nullable BuildSystem buildSystem;

    // FileWatcher fields
    private @Nullable DirectoryWatcher directoryWatcher;
    private @Nullable CompletableFuture<Void> watcherFuture;

    private static final Mustache indexTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        indexTemplate = mf.compile("index.html.mustache");
    }

    private String lastCompilationErrorMessage;

    /**
     * Creates a new JavelitHandler instance.
     */
    public JavelitHandler(
            @Nullable Path appPath,
            @Nullable Class<?> appClass,
            @Nullable JtRunnable appRunnable,
            @Nullable String classpath,
            @Nullable String headersFile,
            @Nullable BuildSystem buildSystem,
            int port) {

        this.port = port;
        this.appPath = appPath;
        this.buildSystem = buildSystem != null ? buildSystem : BuildSystem.RUNTIME;
        this.customHeaders = headersFile != null ? loadHeaders(headersFile) : "";

        final AppConfig config = new AppConfig(
                appPath,
                appClass,
                appRunnable,
                classpath,
                this.buildSystem
        );
        this.appRunner = new AppRunner(config, this);
    }

    private static String loadHeaders(@Nonnull String headersFile) {
        try {
            return java.nio.file.Files.readString(Path.of(headersFile));
        } catch (IOException e) {
            LOG.warn("Failed to load headers file: {}", headersFile, e);
            return "";
        }
    }

    // ==================== Public HTTP Handlers ====================

    /**
     * Handles the main index page request.
     */
    public void handleIndex(HttpRequest request, HttpResponse response) throws IOException {
        final HttpSession session = request.getSession();
        String xsrfToken = (String) session.getAttribute(SESSION_XSRF_ATTRIBUTE);

        // Create XSRF token if it doesn't exist
        if (xsrfToken == null) {
            createXsrfToken(request, response);
            xsrfToken = (String) session.getAttribute(SESSION_XSRF_ATTRIBUTE);
        }

        response.setHeader("Content-Type", "text/html");
        response.sendText(getIndexHtml(xsrfToken));
    }

    /**
     * Handles media file requests with support for range requests (HTTP 206 Partial Content).
     */
    public void handleMedia(HttpRequest request, HttpResponse response) throws IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(400);
            response.sendText("bad request.");
            return;
        }

        // Security checks - validate session ID and XSRF token
        final String sessionId = request.getQueryParameter(SESSION_ID_QUERY_PARAM);
        if (sessionId == null) {
            response.setStatus(400);
            response.sendText("Missing session ID");
            return;
        }

        final String cookieProvidedToken = request.getCookie(XSRF_COOKIE_KEY);
        final String sessionXsrf = session2Xsrf.get(sessionId);
        if (sessionXsrf == null || !sessionXsrf.equals(cookieProvidedToken)) {
            response.setStatus(403);
            response.sendText("XSRF token mismatch");
            return;
        }

        // Extract media hash from path (e.g., "/_/media/abc123" -> "abc123")
        final String path = request.getPath();
        final String hash = path.substring(MEDIA_PATH.length());
        final MediaEntry media = StateManager.getMedia(sessionId, hash);

        if (media == null || hash.isBlank()) {
            response.setStatus(404);
            return;
        }

        response.setHeader("Content-Type", media.format());
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", hash);
        response.setHeader("Cache-Control", "private, max-age=3600, must-revalidate");

        final byte[] data = media.bytes();
        final String rangeHeader = request.getHeader("Range");

        if (rangeHeader == null) {
            // No range request - send full content
            response.setHeader("Content-Length", String.valueOf(data.length));
            response.sendBytes(data);
            return;
        }

        // Parse range header (e.g., "bytes=0-1023")
        final RangeResult range = parseRange(rangeHeader, data.length, request.getHeader("If-Range"), hash);

        if (range.statusCode == 416) {
            response.setStatus(416);
            response.setHeader("Content-Range", "bytes */" + data.length);
            return;
        }

        if (range.statusCode == 200) {
            // Range not satisfiable - send full content
            response.setHeader("Content-Length", String.valueOf(data.length));
            response.sendBytes(data);
            return;
        }

        // Send partial content (206)
        final int length = (int) (range.end - range.start + 1);
        response.setStatus(206);
        response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + data.length);
        response.setHeader("Content-Length", String.valueOf(length));
        response.sendBytes(data, (int) range.start, length);
    }

    /**
     * Handles file upload requests.
     */
    public void handleUpload(HttpRequest request, HttpResponse response) throws IOException {
        if (!"PUT".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(400);
            response.sendText("bad request.");
            return;
        }

        try {
            final MultipartFormData formData = request.parseMultipartForm();
            final List<JtUploadedFile> uploadedFiles = new ArrayList<>();

            for (final String fieldName : formData.getFieldNames()) {
                final MultipartFormData.FormField formField = formData.getFirst(fieldName);
                if (formField == null) {
                    continue;
                }

                checkArgument(formField.isFile(), "Upload form data is not a file item: %s", fieldName);

                final byte[] content = formField.getBytes();
                final JtUploadedFile f = new JtUploadedFile(
                        formField.getFileName(),
                        formField.getContentType(),
                        content
                );
                uploadedFiles.add(f);
            }

            final String sessionId = request.getHeader("X-Session-ID");
            final String componentKey = request.getHeader("X-Component-Key");
            FrontendMessage componentUpdate = new FrontendMessage(
                    "component_update",
                    componentKey,
                    uploadedFiles,
                    null,
                    null
            );
            handleMessage(sessionId, componentUpdate);
            response.setStatus(200);

        } catch (Exception e) {
            LOG.error("Error processing file upload", e);
            response.setStatus(500);
            response.sendText("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Handles static file requests for both internal framework resources and application files.
     * Serves files from:
     * - /_/static/* - Internal framework resources from classpath
     * - /app/static/* - Application-specific files from filesystem
     */
    public void handleStatic(HttpRequest request, HttpResponse response) throws IOException {
        final String path = request.getPath();

        if (path.startsWith("/_/static/")) {
            // Internal framework resources from classpath
            final String resourcePath = path.substring("/_/static/".length());
            serveClasspathResource(resourcePath, response);
        } else if (path.startsWith("/app/static/")) {
            // App-specific resources from filesystem
            final String resourcePath = path.substring("/app/static/".length());
            serveFilesystemResource(resourcePath, response);
        } else {
            response.setStatus(404);
            response.sendText("Not found");
        }
    }

    /**
     * Serves a resource from the classpath (internal framework resources).
     */
    private void serveClasspathResource(String resourcePath, HttpResponse response) throws IOException {
        // Prevent directory traversal
        if (resourcePath.contains("..")) {
            response.setStatus(403);
            response.sendText("Forbidden");
            return;
        }

        final InputStream stream = JavelitHandler.class.getClassLoader()
                .getResourceAsStream("static/" + resourcePath);

        if (stream == null) {
            response.setStatus(404);
            response.sendText("Not found");
            return;
        }

        try {
            final byte[] content = stream.readAllBytes();
            serveResource(resourcePath, content, response);
        } finally {
            stream.close();
        }
    }

    /**
     * Serves a resource from the filesystem (application static directory).
     */
    private void serveFilesystemResource(String resourcePath, HttpResponse response) throws IOException {
        if (appPath == null) {
            response.setStatus(404);
            response.sendText("Not found");
            return;
        }

        final Path staticDir = appPath.toAbsolutePath().getParent().resolve("static");
        final Path requestedFile = staticDir.resolve(resourcePath).normalize();

        // Security: Prevent directory traversal attacks
        if (!requestedFile.startsWith(staticDir)) {
            LOG.warn("Directory traversal attempt blocked: {}", resourcePath);
            response.setStatus(403);
            response.sendText("Forbidden");
            return;
        }

        // Check if file exists and is not a directory
        if (!Files.exists(requestedFile) || Files.isDirectory(requestedFile)) {
            response.setStatus(404);
            response.sendText("Not found");
            return;
        }

        final byte[] content = Files.readAllBytes(requestedFile);
        serveResource(resourcePath, content, response);
    }

    /**
     * Sends resource content with appropriate headers.
     */
    private void serveResource(String path, byte[] content, HttpResponse response) throws IOException {
        final String contentType = detectContentType(path);

        response.setHeader("Content-Type", contentType);
        response.setHeader("Cache-Control", "public, max-age=3600");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Length", String.valueOf(content.length));
        response.setStatus(200);
        response.sendBytes(content);
    }

    /**
     * Detects MIME content type based on file extension.
     */
    private String detectContentType(String path) {
        final String lowerPath = path.toLowerCase(Locale.ROOT);

        if (lowerPath.endsWith(".css")) return "text/css";
        if (lowerPath.endsWith(".js")) return "application/javascript";
        if (lowerPath.endsWith(".json")) return "application/json";
        if (lowerPath.endsWith(".png")) return "image/png";
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) return "image/jpeg";
        if (lowerPath.endsWith(".gif")) return "image/gif";
        if (lowerPath.endsWith(".svg")) return "image/svg+xml";
        if (lowerPath.endsWith(".ico")) return "image/x-icon";
        if (lowerPath.endsWith(".woff")) return "font/woff";
        if (lowerPath.endsWith(".woff2")) return "font/woff2";
        if (lowerPath.endsWith(".ttf")) return "font/ttf";
        if (lowerPath.endsWith(".eot")) return "application/vnd.ms-fontobject";
        if (lowerPath.endsWith(".txt")) return "text/plain";
        if (lowerPath.endsWith(".html") || lowerPath.endsWith(".htm")) return "text/html";
        if (lowerPath.endsWith(".xml")) return "application/xml";
        if (lowerPath.endsWith(".pdf")) return "application/pdf";
        if (lowerPath.endsWith(".zip")) return "application/zip";

        return "application/octet-stream";
    }

    /**
     * Returns the path to the application's static directory, if it exists.
     * Used by deployment environments (Spring Boot, Quarkus) to configure optimized static handlers.
     */
    public @Nullable Path getAppStaticPath() {
        if (appPath == null) {
            return null;
        }
        final Path staticDir = appPath.toAbsolutePath().getParent().resolve("static");
        if (Files.exists(staticDir) && Files.isDirectory(staticDir)) {
            return staticDir;
        }
        return null;
    }

    // ==================== XSRF Token Handling ====================

    /**
     * Validates XSRF token for state-changing requests (POST, PUT, PATCH, DELETE).
     * Returns true if validation passes or not required (GET requests).
     */
    public boolean validateXsrf(HttpRequest request, HttpResponse response) throws IOException {
        final boolean requiresXsrfValidation = Set.of("POST", "PUT", "PATCH", "DELETE")
                .contains(request.getMethod());

        final HttpSession session = request.getSession();
        String expectedToken = (String) session.getAttribute(SESSION_XSRF_ATTRIBUTE);

        // For non-validating requests, create XSRF token if it doesn't exist (like old XsrfValidationHandler)
        if (!requiresXsrfValidation) {
            if (expectedToken == null) {
                createXsrfToken(request, response);
            }
            return true; // GET requests don't require XSRF validation
        }

        // For POST/PUT/PATCH/DELETE, token must already exist
        if (expectedToken == null) {
            LOG.warn("XSRF validation failed: no token in session");
            return false;
        }

        // Perform triple validation: header + cookie + session
        final String providedToken = request.getHeader("X-XSRF-TOKEN");
        final String cookieProvidedToken = request.getCookie(XSRF_COOKIE_KEY);

        if (providedToken == null || !providedToken.equals(cookieProvidedToken) ||
                !providedToken.equals(expectedToken)) {
            LOG.warn("XSRF validation failed: token mismatch");
            return false;
        }

        // Validate page session ID
        final String pageSessionId = request.getHeader("X-Session-ID");
        if (pageSessionId == null || !session2WsChannel.containsKey(pageSessionId)) {
            LOG.warn("XSRF validation failed: invalid session");
            return false;
        }

        // Verify XSRF token matches the one stored for this WebSocket session
        final String sessionXsrf = session2Xsrf.get(pageSessionId);
        if (sessionXsrf == null || !sessionXsrf.equals(providedToken)) {
            LOG.warn("XSRF validation failed: session token mismatch");
            return false;
        }

        return true;
    }

    /**
     * Creates and stores a new XSRF token if one doesn't exist.
     */
    public void createXsrfToken(HttpRequest request, HttpResponse response) throws IOException {
        final HttpSession session = request.getSession();

        if (!session.getAttributeNames().contains(SESSION_XSRF_ATTRIBUTE)) {
            final String xsrfToken = generateSecureXsrfToken();
            session.setAttribute(SESSION_XSRF_ATTRIBUTE, xsrfToken);

            final HttpCookie cookie = new HttpCookie(
                    XSRF_COOKIE_KEY,
                    xsrfToken,
                    "/",
                    86400 * 7, // 7 days
                    false, // not HttpOnly (needs to be readable by JavaScript)
                    true   // SameSite
            );
            response.setCookie(cookie);
        }
    }

    /**
     * Checks if the session already has an XSRF token.
     */
    public boolean hasXsrfToken(HttpRequest request) {
        final HttpSession session = request.getSession();
        return session.getAttributeNames().contains(SESSION_XSRF_ATTRIBUTE);
    }

    // ==================== WebSocket Handling ====================

    /**
     * Handles new WebSocket connection.
     */
    public void handleWebSocketConnect(WebSocketSession ws, @Nullable String httpSessionId) {
        final String sessionId = ws.getSessionId();
        session2WsChannel.put(sessionId, ws);

        // Detect if this is a localhost connection (for developer features)
        // Use exact IP matching like old Server.java.backup implementation
        final InetSocketAddress remoteAddr = ws.getRemoteAddress();
        final boolean isLocalhost = remoteAddr != null &&
                Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1")
                        .contains(remoteAddr.getAddress().getHostAddress());
        if (isLocalhost) {
            StateManager.registerDeveloperSession(sessionId);
        }

        // Send session initialization message
        try {
            ws.sendText(Shared.OBJECT_MAPPER.writeValueAsString(
                    Map.of("type", "session_init", "sessionId", sessionId)
            ));
        } catch (IOException e) {
            LOG.error("Error sending session init message", e);
        }

        // Store XSRF token for this WebSocket session (extract from HTTP session if available)
        // Note: httpSessionId may be used to lookup XSRF token from HTTP session
        // For now, we'll let it be set when the first request comes in

        LOG.info("WebSocket connected: sessionId={}, localhost={}", sessionId, isLocalhost);
    }

    /**
     * Handles WebSocket message from client.
     */
    public void handleWebSocketMessage(String sessionId, String messageJson) {
        try {
            final FrontendMessage msg = Shared.OBJECT_MAPPER.readValue(messageJson, FrontendMessage.class);
            handleMessage(sessionId, msg);
        } catch (Exception e) {
            LOG.error("Error handling WebSocket message", e);
        }
    }

    /**
     * Handles WebSocket disconnect.
     */
    public void handleWebSocketClose(String sessionId) {
        session2WsChannel.remove(sessionId);
        session2Xsrf.remove(sessionId);
        StateManager.clearSession(sessionId);
        LOG.info("WebSocket disconnected: sessionId={}", sessionId);
    }

    /**
     * Associates an XSRF token with a WebSocket session.
     * Called when we extract the XSRF token from the HTTP session during WebSocket handshake.
     *
     * @throws RuntimeException if xsrfToken is null (matching old Server.java behavior)
     */
    public void associateXsrfToken(String sessionId, String xsrfToken) {
        if (xsrfToken == null) {
            throw new RuntimeException(
                    "Session did not provide a valid XSRF token for sessionId: " + sessionId);
        }
        session2Xsrf.put(sessionId, xsrfToken);
        LOG.debug("Associated XSRF token with session: {}", sessionId);
    }

    // ==================== StateManager.RenderServer Implementation ====================

    @Override
    public void send(
            @Nonnull String sessionId,
            @Nullable String renderHtml,
            @Nullable String registrationHtml,
            @NotNull JtContainer container,
            @Nullable Integer index,
            boolean clearBefore) {

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

    @Override
    public void sendStatus(
            @Nonnull String sessionId,
            @NotNull StateManager.ExecutionStatus executionStatus,
            @Nullable Map<String, Integer> unusedComponents) {

        final Map<String, Object> message = new HashMap<>();
        message.put("type", "status");
        message.put("status", executionStatus);

        if (StateManager.isDeveloperSession(sessionId) && unusedComponents != null && !unusedComponents.isEmpty()) {
            message.put("toastDuration", 10);
            final List<String> unusedComponentsListItems = unusedComponents.entrySet().stream()
                    .map(e -> unusedComponentToMarkdownLi(e.getKey(), e.getValue()))
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

    // ==================== Private Helper Methods ====================

    private void handleMessage(String sessionId, FrontendMessage frontendMessage) {
        boolean doRerun = false;
        try {
            switch (frontendMessage.type()) {
                case "component_update" -> {
                    doRerun = StateManager.handleComponentUpdate(
                            sessionId,
                            frontendMessage.componentKey(),
                            frontendMessage.value()
                    );
                }
                case "reload" -> doRerun = true;
                case "path_update" -> {
                    final UrlContext urlContext = new UrlContext(
                            frontendMessage.path() != null ? frontendMessage.path() : "",
                            frontendMessage.queryParameters() != null ? frontendMessage.queryParameters() : Map.of()
                    );
                    StateManager.setUrlContext(sessionId, urlContext);
                    doRerun = true;
                }
                case "clear_cache" -> {
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
            LOG.error("Error handling client message", e);
            sendFullScreenModalError(
                    sessionId,
                    "Client message processing error",
                    "The server was not able to process the client message. Please reach out to support if this error is unexpected.",
                    e.getMessage(),
                    true
            );
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

    private void sendMessage(String sessionId, Map<String, Object> message) {
        final WebSocketSession ws = session2WsChannel.get(sessionId);
        if (ws != null) {
            try {
                String json = Shared.OBJECT_MAPPER.writeValueAsString(message);
                ws.sendText(json);
            } catch (IOException e) {
                LOG.error("Error sending message to session " + sessionId, e);
            }
        } else {
            LOG.error("Error sending message. Unknown sessionId: {}", sessionId);
        }
    }

    private void sendFullScreenModalError(
            String sessionId,
            String title,
            String paragraph,
            String error,
            boolean closable) {

        final Map<String, Object> message = new HashMap<>();
        message.put("type", "modal_error");
        message.put("title", title);
        message.put("paragraph", paragraph);
        message.put("error", error);
        message.put("closable", closable);
        sendMessage(sessionId, message);
    }

    private void sendCompilationError(String sessionId, String error) {
        sendFullScreenModalError(
                sessionId,
                "Compilation error",
                "Fix the compilation errors below and save the file to continue:",
                error,
                false
        );
    }

    private String getIndexHtml(String xsrfToken) {
        final StringWriter writer = new StringWriter();
        indexTemplate.execute(writer, Map.of(
                "MATERIAL_SYMBOLS_CDN", JtComponent.MATERIAL_SYMBOLS_CDN,
                "LIT_DEPENDENCY", JtComponent.LIT_DEPENDENCY,
                "customHeaders", customHeaders,
                "port", port,
                "XSRF_TOKEN", xsrfToken,
                "PRISM_SETUP_SNIPPET", JtComponent.PRISM_SETUP_SNIPPET,
                "PRISM_CSS", JtComponent.PRISM_CSS
        ));
        return writer.toString();
    }

    private static String unusedComponentToMarkdownLi(String name, Integer unusedCount) {
        final String userFriendlyName = name.substring(name.lastIndexOf(".") + 1).replace("Component", "");
        return "- " + userFriendlyName + " - _" + unusedCount + "_";
    }

    private static String generateSecureXsrfToken() {
        final SecureRandom random;
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        final byte[] bytes = new byte[32]; // 256-bit token
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Parses HTTP Range header and validates against If-Range conditions.
     */
    private static RangeResult parseRange(String rangeHeader, int contentLength, @Nullable String ifRangeHeader, String etag) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            return new RangeResult(200, 0, 0); // No range or invalid format
        }

        // Check If-Range condition
        if (ifRangeHeader != null && !ifRangeHeader.equals(etag)) {
            return new RangeResult(200, 0, 0); // If-Range mismatch, return full content
        }

        // Parse "bytes=start-end"
        final String rangeSpec = rangeHeader.substring(6);
        final String[] parts = rangeSpec.split("-");

        try {
            long start = 0;
            long end = contentLength - 1;

            if (parts.length == 1) {
                if (rangeSpec.startsWith("-")) {
                    // Suffix range: "-500" means last 500 bytes
                    long suffix = Long.parseLong(parts[0].substring(1));
                    start = Math.max(0, contentLength - suffix);
                } else {
                    // "500-" means from byte 500 to end
                    start = Long.parseLong(parts[0]);
                }
            } else if (parts.length == 2) {
                start = Long.parseLong(parts[0]);
                if (!parts[1].isEmpty()) {
                    end = Long.parseLong(parts[1]);
                }
            }

            // Validate range
            if (start < 0 || start >= contentLength || end < start || end >= contentLength) {
                return new RangeResult(416, 0, 0); // Range not satisfiable
            }

            return new RangeResult(206, start, end);

        } catch (NumberFormatException e) {
            return new RangeResult(200, 0, 0); // Invalid range format, return full content
        }
    }

    private record RangeResult(int statusCode, long start, long end) {}

    private record FrontendMessage(
            @Nonnull String type,
            @Nullable String componentKey,
            @Nullable Object value,
            @Nullable String path,
            @Nullable Map<String, List<String>> queryParameters
    ) {}

    // ==================== FileWatcher Methods ====================

    /**
     * Starts the file watcher to enable hot-reload during development.
     * Only watches if appPath is not null.
     */
    public void startFileWatcher() {
        if (appPath == null) {
            LOG.debug("No app path specified, file watching disabled");
            return;
        }

        if (directoryWatcher != null) {
            throw new IllegalStateException("FileWatcher is already running");
        }

        try {
            // Determine watch directory based on build system
            final Path directory;
            if (buildSystem == BuildSystem.FATJAR_AND_JBANG || buildSystem == BuildSystem.RUNTIME) {
                directory = appPath.toAbsolutePath().getParent();
            } else {
                // Maven/Gradle: watch project root
                directory = Paths.get("").toAbsolutePath();
            }

            final Path watchedFile = appPath.toAbsolutePath();

            directoryWatcher = DirectoryWatcher.builder()
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
                                default -> LOG.warn("File changed: {} but event type is not managed: {}.",
                                        changedFile,
                                        event.eventType());
                            }
                        }
                    })
                    .build();

            LOG.info("Initializing file watch in parent directory: {}", directory);

            // Initialize watcher with timeout protection (like old code)
            final CompletableFuture<CompletableFuture<Void>> watcherFutureWrapper =
                    CompletableFuture.supplyAsync(() -> directoryWatcher.watchAsync());

            try {
                watcherFuture = watcherFutureWrapper.get(10, TimeUnit.SECONDS);
                LOG.info("File watch started successfully");
            } catch (TimeoutException e) {
                throw new RuntimeException(
                        "Initializing file watch timed out after 10 seconds. Try to run the javelit app in a parent directory with less files. Also, do not run the javelit app in a parent directory that contains Cloud files (iCloud, Dropbox, etc...).",
                        e);
            } catch (InterruptedException e) {
                throw new RuntimeException("Initializing file watch was interrupted.", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Initializing file watch failed.", e);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to start file watcher", e);
        }
    }

    /**
     * Stops the file watcher if it's running.
     */
    public void stopFileWatcher() {
        if (directoryWatcher != null) {
            try {
                directoryWatcher.close();
                LOG.info("File watcher stopped");
            } catch (IOException e) {
                LOG.error("Error stopping file watcher", e);
            }
            directoryWatcher = null;
        }

        if (watcherFuture != null) {
            watcherFuture.cancel(true);
            watcherFuture = null;
        }
    }

    /**
     * Notifies all sessions to reload the app (triggered by file changes).
     */
    private void notifyReload() {
        // Reload the app and re-run for all sessions
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
}
