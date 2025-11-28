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
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayDeque;
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
import io.javelit.http.JavelitCookie;
import io.javelit.http.JavelitCookieImpl;
import io.javelit.http.JavelitHeaders;
import io.javelit.http.JavelitHttpExchange;
import io.javelit.http.JavelitMultiPart;
import io.javelit.http.JavelitPart;
import io.javelit.http.JavelitSession;
import io.javelit.http.JavelitStatusCode;
import io.javelit.http.JavelitWebSocketChannel;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;
import io.undertow.util.ByteRange;
import io.undertow.util.StatusCodes;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static io.javelit.core.DeployUtils.generateRailwayDeployUrl;
import static io.javelit.core.utils.EmbedUtils.iframeHtml;
import static io.javelit.core.utils.LangUtils.optional;
import static io.javelit.http.JavelitHttpUtils.isLocalClient;

public class CoreServer implements StateManager.RenderServer {
  private static final String SESSION_XSRF_ATTRIBUTE = "XSRF_TOKEN";
  private static final String XSRF_COOKIE_KEY = "javelit-xsrf";
  public static final String SESSION_ID_COOKIE_KEY = "javelit-session-id";
  private static final String EMBED_QUERY_PARAM = "embed";

  // visible for StateManager
  static final String MEDIA_PATH = "/_/media/";
  static final String SESSION_ID_QUERY_PARAM = "sid";

  private static final Logger LOG = LoggerFactory.getLogger(CoreServer.class);

  private final @Nonnull AppRunner appRunner;
  private final @Nullable FileWatcher fileWatcher;
  private final @Nonnull BuildSystem buildSystem;

  @Nullable
  public Path getAppPath() {
    return appPath;
  }

  private final @Nullable Path appPath;
  private final boolean standaloneMode;
  private final @Nullable String originalUrl;
  private final @Nullable String basePath;
  private boolean ready;

  private final Map<String, JavelitWebSocketChannel> session2WsChannel = new ConcurrentHashMap<>();
  private final Map<String, String> session2Xsrf = new ConcurrentHashMap<>();
  private final String customHeaders;

  private static final Mustache indexTemplate;
  private static final Mustache SAFARI_WARNING_TEMPLATE;

  static {
    final MustacheFactory mf = new DefaultMustacheFactory();
    indexTemplate = mf.compile("index.html.mustache");
    SAFARI_WARNING_TEMPLATE = mf.compile("safari-embed-warning.html.mustache");
  }

  private String lastCompilationErrorMessage;

  public CoreServer(final JavelitServerConfig builder) {
    this.customHeaders = loadCustomHeaders(builder.getHeadersFile());
    this.appRunner = new AppRunner(builder, this);
    this.appPath = builder.getAppPath();
    this.standaloneMode = this.appPath != null;
    this.fileWatcher = this.standaloneMode ? new FileWatcher(builder.getAppPath()) : null;
    this.buildSystem = builder.getBuildSystem();
    this.ready = false;
    this.originalUrl = builder.getOriginalUrl();
    this.basePath = builder.getBasePath() == null ? null : cleanBasePath(builder.getBasePath());
  }

  private static @Nonnull String cleanBasePath(final @Nonnull String path) {
    String cleaned = path.trim();
    cleaned = cleaned.startsWith("/") ? cleaned : "/" + cleaned;
    cleaned = cleaned.endsWith("/") ? cleaned.substring(0, cleaned.length() - 1) : cleaned;
    return cleaned;
  }

  public void start() {
    if (fileWatcher != null) {
      try {
        fileWatcher.start();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    ready = true;
  }

  public void stop() {
    if (fileWatcher != null) {
      fileWatcher.stop();
    }
  }

  public void handleHttpRequest(final @Nonnull JavelitHttpExchange exchange) {
    // session --> done by the adapter interface
    // xsrf --> done by the adapter calling handleXsrf
    // embedded support (iframe) --> done by the adapter calling handleEmbedded
    // INTERNAL RESOURCES --> done by the adapter
    // user resources --> done by the adapter

    switch (exchange.path()) {
      case "/_/health" -> handleHealth(exchange);
      case "/_/ready" -> handleReady(exchange);
      case "/_/oembed" -> handleOembed(exchange);
      case "/_/ws" -> throw new RuntimeException("Implementation error, please reach out to support");
      case "/_/upload" -> handleUpload(exchange);
      default -> {
        if (exchange.path().startsWith(MEDIA_PATH)) {
          handleMedia(exchange);
        } else {
          handleIndex(exchange);
        }
      }
    }
  }

  // returns the socket session id (javelit session, not http session, id)
  public String handleSocketConnect(final @Nullable JavelitSession session, final JavelitWebSocketChannel channel) {
    final String sessionId = UUID.randomUUID().toString();
    if (session == null) {
      throw new RuntimeException("No http session found for javelit channel sessionId: " + sessionId);
    }
    session2WsChannel.put(sessionId, channel);
    if (isLocalClient(channel.getRemoteHostAddress())) {
      StateManager.registerDeveloperSession(sessionId);
    }
    // Send session ID to frontend immediately
    final Map<String, Object> sessionInitMessage = new HashMap<>();
    sessionInitMessage.put("type", "session_init");
    sessionInitMessage.put("sessionId", sessionId);
    sendMessage(channel, sessionInitMessage);
    final @Nullable String xsrf = (String) session.getAttribute(SESSION_XSRF_ATTRIBUTE);
    if (xsrf == null) {
      throw new RuntimeException("Session did not provide a valid xsrf token: " + sessionId);
    }
    session2Xsrf.put(sessionId, xsrf);
    return sessionId;
  }

  public void handleSocketFullTextMessage(final @Nonnull String sessionId, final @Nonnull String text) {
    try {
      FrontendMessage msg = Shared.OBJECT_MAPPER.readValue(text, FrontendMessage.class);
      handleMessage(sessionId, msg);
    } catch (Exception e) {
      LOG.error("Error handling message", e);
    }
  }

  public void handleSocketCloseMessage(final @Nonnull String sessionId) {
    session2WsChannel.remove(sessionId);
    session2Xsrf.remove(sessionId);
    StateManager.clearSession(sessionId);
  }

  private void handleIndex(final @Nonnull JavelitHttpExchange exchange) {
    // get or create session, then generate and attach XSRF token cookie
    final JavelitSession currentSession = exchange.getOrCreateSession();
    final String xsrfToken = (String) currentSession.getAttribute(SESSION_XSRF_ATTRIBUTE);
    exchange.addHeader(JavelitHeaders.CONTENT_TYPE, "text/html");
    final boolean devMode = isLocalClient(exchange.getRemoteHostAddress());
    final String currentUrl = getCurrentUrl(exchange);
    final String basePath = extractBasePath(exchange);

    exchange.write(getIndexHtml(xsrfToken, devMode, currentUrl, basePath));
  }

  private String getIndexHtml(final String xsrfToken,
                              boolean devMode,
                              final String encodedCurrentUrl,
                              final String basePath) {
    final StringWriter writer = new StringWriter();
    indexTemplate.execute(writer,
                          Map.ofEntries(Map.entry("MATERIAL_SYMBOLS_CDN", JtComponent.MATERIAL_SYMBOLS_CDN),
                                        Map.entry("LIT_DEPENDENCY", JtComponent.LIT_DEPENDENCY),
                                        Map.entry("customHeaders", customHeaders),
                                        Map.entry("XSRF_TOKEN", xsrfToken),
                                        Map.entry("PRISM_SETUP_SNIPPET", JtComponent.PRISM_SETUP_SNIPPET),
                                        Map.entry("PRISM_CSS", JtComponent.PRISM_CSS),
                                        Map.entry("DEV_MODE", devMode),
                                        Map.entry("STANDALONE_MODE", standaloneMode),
                                        Map.entry("RAILWAY_DEPLOY_APP_URL",
                                                  devMode && standaloneMode ?
                                                      generateRailwayDeployUrl(appPath, originalUrl) :
                                                      ""),
                                        Map.entry("ENCODED_CURRENT_URL", encodedCurrentUrl),
                                        Map.entry("BASE_URL_PATH", basePath)));
    return writer.toString();
  }

  private void handleMedia(final @Nonnull JavelitHttpExchange exchange) {
    if ("GET".equalsIgnoreCase(exchange.method())) {
      handleMediaGets(exchange);
    } else {
      // invalid endpoint / method
      exchange.setStatus(StatusCodes.BAD_REQUEST);
      exchange.write("bad request.");
    }
  }

  private void handleMediaGets(final @Nonnull JavelitHttpExchange exchange) {
    // security checks - it's not possible to read a media from a different xsrf token
    final String sessionId = optional(exchange.queryParameters().get(SESSION_ID_QUERY_PARAM))
        .map(Deque::getFirst)
        .orElse(null);
    if (sessionId == null) {
      exchange.setStatus(StatusCodes.BAD_REQUEST);
      exchange.write("Missing session ID");
      return;
    }
    final String cookieProvidedToken = optional(exchange.cookie(XSRF_COOKIE_KEY))
        .map(JavelitCookie::getValue)
        .orElse(null);

    final String sessionXsrf = session2Xsrf.get(sessionId);
    if (sessionXsrf == null || !sessionXsrf.equals(cookieProvidedToken)) {
      exchange.setStatus(StatusCodes.FORBIDDEN);
      exchange.write("XSRF token mismatch");
      return;
    }

    final String hash = exchange.getRelativePath().substring(MEDIA_PATH.length());
    final MediaEntry media = StateManager.getMedia(sessionId, hash);
    writeResponse(exchange, media, hash);
  }

  private void writeResponse(final @Nonnull JavelitHttpExchange exchange,
                             final @Nullable MediaEntry media,
                             final @Nonnull String hash) {
    if (media == null || hash.isBlank()) {
      exchange.setStatus(StatusCodes.NOT_FOUND);
      return;
    }
    // X-Frame-Options is NONE when embed=true is not set (the most common case)
    // we allow iframe for media because the component pdf uses iframe
    exchange.addHeader(JavelitHeaders.X_FRAME_OPTIONS, "SAMEORIGIN");
    final String ifNoneMatch = exchange.firstHeader(JavelitHeaders.IF_NONE_MATCH);
    if (ifNoneMatch != null && ifNoneMatch.equals(hash)) {
      exchange.setStatus(StatusCodes.NOT_MODIFIED);
      return;
    }
    exchange.addHeader(JavelitHeaders.CONTENT_TYPE, media.format());
    exchange.addHeader(JavelitHeaders.ACCEPT_RANGES, "bytes");
    exchange.addHeader(JavelitHeaders.ETAG, hash);
    exchange.addHeader(JavelitHeaders.CACHE_CONTROL, "private, max-age=3600, must-revalidate");
    final byte[] data = media.bytes();
    final String rangeHeader = exchange.firstHeader(JavelitHeaders.RANGE);
    final ByteRange range = ByteRange.parse(rangeHeader);
    if (range == null) {
      // no range header, or invalid / unsupported range format (e.g., multi-range)
      writeFullContent(exchange, data);
      return;
    }
    final ByteRange.RangeResponseResult result = range.getResponseResult(data.length,
                                                                         exchange.firstHeader(JavelitHeaders.IF_RANGE),
                                                                         null,
                                                                         // lastModified
                                                                         hash);
    if (result.getStatusCode() == StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE) {
      exchange.setStatus(StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE);
      exchange.addHeader(JavelitHeaders.CONTENT_RANGE, "bytes */" + data.length);
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
    exchange.setStatus(StatusCodes.PARTIAL_CONTENT);
    exchange.addHeader(JavelitHeaders.CONTENT_RANGE, result.getContentRange());
    exchange.addHeader(JavelitHeaders.CONTENT_LENGTH, length);
    exchange.write(ByteBuffer.wrap(data, (int) start, length));
  }

  private void writeFullContent(final @Nonnull JavelitHttpExchange exchange, final byte[] data) {
    exchange.addHeader(JavelitHeaders.CONTENT_LENGTH, data.length);
    exchange.write(ByteBuffer.wrap(data));
  }

  private void handleUpload(final @Nonnull JavelitHttpExchange exchange) {
    switch (exchange.method()) {
      case "PUT" -> {
        handleUploadPuts(exchange);
      }
      case null, default -> {
        // invalid endpoint / method
        exchange.setStatus(StatusCodes.BAD_REQUEST);
        exchange.write("bad request.");
      }
    }
  }

  private void handleUploadPuts(final @Nonnull JavelitHttpExchange exchange) {
    try (final JavelitMultiPart multiparts = exchange.getMultiPartFormData()) {
      if (multiparts == null) {
        exchange.setStatus(StatusCodes.BAD_REQUEST);
        exchange.write("Request is not multipart/form-data");
        return;
      }
      // either an element is a file, has fileName and fileItem, either it has value set.
      final List<JtUploadedFile> uploadedFiles = new ArrayList<>();
      for (final JavelitPart part : multiparts.parts()) {
        checkArgument(part.isFile(), "Upload form data is not a file item: %s", part.name());
        final byte[] content;
        try {
          content = part.bytes();
        } catch (IOException e) {
          exchange.setStatus(StatusCodes.INTERNAL_SERVER_ERROR);
          exchange.write("Failed to read uploaded file: " + part.filename());
          return;
        }
        final JtUploadedFile f = new JtUploadedFile(part.filename(), part.contentType(), content);
        uploadedFiles.add(f);
      }

      // TODO NEED TO GET THE SESSION ID PROPERLY
      final String sessionId = exchange.firstHeader("X-Session-ID");
      final String componentKey = exchange.firstHeader("X-Component-Key");
      FrontendMessage componentUpdate = new FrontendMessage("component_update",
                                                            componentKey,
                                                            uploadedFiles,
                                                            null,
                                                            null);
      handleMessage(sessionId, componentUpdate);
      exchange.setStatus(StatusCodes.OK);
    } catch (Exception e) {
      LOG.error("Error processing file upload", e);
      exchange.setStatus(StatusCodes.INTERNAL_SERVER_ERROR);
      exchange.write("Upload failed:  " + e.getMessage());
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
          final UrlContext urlContext = new UrlContext(optional(frontendMessage.path()).orElse(""),
                                                       optional(frontendMessage.queryParameters()).orElse(Map.of()));
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
    sendFullScreenModalError(sessionId,
                             "Compilation error",
                             "Fix the compilation errors below and save the file to continue:",
                             error,
                             false);
  }

  private void sendMessage(final String sessionId, final Map<String, Object> message) {
    final JavelitWebSocketChannel channel = session2WsChannel.get(sessionId);
    if (channel != null) {
      sendMessage(channel, message);
    } else {
      LOG.error("Error sending message. Unknown sessionId: {}", sessionId);
    }
  }

  private static void sendMessage(final @Nonnull JavelitWebSocketChannel channel, final Map<String, Object> message) {
    try {
      String json = Shared.OBJECT_MAPPER.writeValueAsString(message);
      channel.sendText(json);
    } catch (Exception e) {
      LOG.error("Error sending message", e);
    }
  }

  /**
   * Handler for oEmbed endpoint that returns embed information for the app.
   * Implements the oEmbed specification: https://oembed.com/
   */
  private void handleOembed(final @Nonnull JavelitHttpExchange exchange) {
    exchange.addHeader(JavelitHeaders.CONTENT_TYPE, "application/json; charset=utf-8");

    // Parse query parameters
    final String url = exchange.queryParameters().getOrDefault("url", new ArrayDeque<>()).peek();
    final String format = exchange.queryParameters().getOrDefault("format", new ArrayDeque<>(List.of("json"))).peek();

    // Validate URL parameter
    if (url == null || url.isEmpty()) {
      exchange.setStatus(StatusCodes.BAD_REQUEST);
      exchange.write("{\"error\":\"Missing required parameter: url\"}");
      return;
    }

    // Only support JSON format (XML is rarely used)
    if (!"json".equalsIgnoreCase(format)) {
      exchange.setStatus(StatusCodes.NOT_IMPLEMENTED);
      exchange.write("{\"error\":\"Only JSON format is supported\"}");
      return;
    }

    final String iframeHtml = iframeHtml(url, 600);

    // Build oEmbed JSON response
    final String oembedJson = String.format("""
                                                {
                                                  "title": "Javelit",
                                                  "version": "1.0",
                                                  "type": "rich",
                                                  "provider_name": "Javelit",
                                                  "provider_url": "https://javelit.io",
                                                  "html": "%s"
                                                }""", iframeHtml.replace("\"", "\\\"") // Escape double quotes for JSON
    );

    exchange.setStatus(StatusCodes.OK);
    exchange.write(oembedJson);

  }

  private void handleHealth(final @Nonnull JavelitHttpExchange exchange) {
    exchange.setStatus(StatusCodes.OK);
    exchange.addHeader(JavelitHeaders.CONTENT_TYPE, "text/plain");
    exchange.write("OK");
  }

  private void handleReady(final @Nonnull JavelitHttpExchange exchange) {
    exchange.addHeader(JavelitHeaders.CONTENT_TYPE, "text/plain");
    if (ready) {
      exchange.setStatus(JavelitStatusCode.OK);
      exchange.write("OK");
    } else {
      exchange.setStatus(JavelitStatusCode.SERVICE_UNAVAILABLE);
      exchange.write("Service Unavailable");
    }

  }

  // FIXME need to return whether should terminate or not
  public void handleEmbedded(final @Nonnull JavelitHttpExchange exchange) {
    final boolean embedMode = isEmbedMode(exchange);
    if (embedMode && isSafari(exchange)) {
      // return a static html page - embedded is not supported in safari
      // Remove ?embed=true from URL for the "open in new tab" link
      final String currentUrl = getCurrentUrl(exchange);
      final String appUrl = currentUrl.replaceAll("[?&]embed=true", "").replaceAll("\\?&", "?").replaceAll("\\?$", "");
      exchange.write(getSafariWarningHtml(appUrl));
      return;
    } else if (embedMode) {
      final Iterable<JavelitCookie> sessionCookies = exchange.responseCookies();
      for (JavelitCookie cookie : sessionCookies) {
        if (Set.of(XSRF_COOKIE_KEY, SESSION_ID_COOKIE_KEY).contains(cookie.getName())) {
          cookie.setSameSiteMode("None").setSecure(true);
        }
      }
    } else {
      // prevent iframe explicitly
      exchange.setHeader("X-Frame-Options", "DENY");
    }
  }

  // FIXME - this method is incorrect anyway behind proxy 
  private static String getCurrentUrl(final @Nonnull JavelitHttpExchange exchange) {
    String currentUrl = exchange.getRequestURL();
    // handle reverse proxy HTTPS forwarding to not lose https
    // FIXME need to take all other forwarded fields, not only the protocol - domain needs to be fixed too
    final String forwardedProto = exchange.firstHeader("X-Forwarded-Proto");
    if ("https".equalsIgnoreCase(forwardedProto) && currentUrl.startsWith("http://")) {
      currentUrl = "https://" + currentUrl.substring(7);
    }
    return currentUrl;
  }

  private String extractBasePath(final @Nonnull JavelitHttpExchange exchange) {
    final boolean isIgnoreBasePath = isLocalClient(exchange.getRemoteHostAddress()) && exchange
        .queryParameters()
        .containsKey("ignoreBasePath");
    if (basePath != null && !isIgnoreBasePath) {
      return basePath;
    }

    String basePath = exchange.firstHeader("X-Forwarded-Prefix");
    if (basePath == null || basePath.isEmpty()) {
      return "";
    }
    return cleanBasePath(basePath);
  }

  private static boolean isEmbedMode(final @Nonnull JavelitHttpExchange exchange) {
    return optional(exchange.queryParameters())
        .map(e -> e.get(EMBED_QUERY_PARAM))
        .map(Deque::peek)
        .map("true"::equalsIgnoreCase)
        .orElse(false);
  }


  private static boolean isSafari(final @Nonnull JavelitHttpExchange exchange) {
    final String userAgent = exchange.firstHeader("User-Agent");
    return userAgent != null && userAgent.contains("Safari") && !userAgent.contains("Chrome") && !userAgent.contains(
        "Chromium");
  }

  private static String getSafariWarningHtml(final @Nonnull String appUrl) {
    final Map<String, Object> context = new HashMap<>();
    context.put("APP_URL", appUrl);
    final StringWriter writer = new StringWriter();
    SAFARI_WARNING_TEMPLATE.execute(writer, context);
    return writer.toString();
  }

  // FIXME need to return whether should terminate or not
  public void handleXsrf(JavelitHttpExchange exchange) {
    final boolean requiresXsrfValidation = Set.of("POST", "PUT", "PATCH", "DELETE").contains(exchange.method());
    final JavelitSession currentSession = exchange.getOrCreateSession();
    if (requiresXsrfValidation) {
      // validate Xsrf Token
      final String expectedToken = (String) currentSession.getAttribute(SESSION_XSRF_ATTRIBUTE);
      if (expectedToken == null) {
        // the session just got created - this should not happen
        exchange.setStatus(JavelitStatusCode.FORBIDDEN);
        exchange.write("Request coming from invalid session.");
        return;
      }
      // perform XSRF validation
      final String providedToken = exchange.firstHeader("X-XSRF-TOKEN");
      final String cookieProvidedToken = optional(exchange.cookie(XSRF_COOKIE_KEY))
          .map(JavelitCookie::getValue)
          .orElse(null);
      if (providedToken == null || !providedToken.equals(cookieProvidedToken) || !providedToken.equals(expectedToken)) {
        exchange.setStatus(StatusCodes.FORBIDDEN);
        exchange.write("Invalid XSRF token");
        return;
      }

      final String pageSessionId = exchange.firstHeader("X-Session-ID");
      if (pageSessionId == null || !session2WsChannel.containsKey(pageSessionId)) {
        exchange.setStatus(StatusCodes.UNAUTHORIZED);
        exchange.write("Invalid session");
        return;
      }
      // Verify XSRF token matches the one stored for this WebSocket session
      final String sessionXsrf = session2Xsrf.get(pageSessionId);
      if (sessionXsrf == null || !sessionXsrf.equals(providedToken)) {
        exchange.setStatus(StatusCodes.FORBIDDEN);
        exchange.write("XSRF token mismatch");
        return;
      }
    } else {
      // attempt to create one if need be
      if (currentSession.getAttribute(SESSION_XSRF_ATTRIBUTE) == null) {
        final String xsrfToken = generateSecureXsrfToken();
        currentSession.setAttribute(SESSION_XSRF_ATTRIBUTE, xsrfToken);
        exchange.setCookie(new JavelitCookieImpl(XSRF_COOKIE_KEY, xsrfToken)
                               .setHttpOnly(false)
                               .setSameSite(true)
                               .setPath("/")
                               .setMaxAge(86400 * 7));
      }
    }
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
      session2WsChannel.keySet().forEach(sessionId -> sendCompilationError(sessionId, lastCompilationErrorMessage));
      return;
    }

    for (final String sessionId : session2WsChannel.keySet()) {
      appRunner.runApp(sessionId);
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
  public void sendStatus(final @Nonnull String sessionId,
                         @NotNull StateManager.ExecutionStatus executionStatus,
                         final @Nullable Map<String, Integer> unusedComponents) {
    final Map<String, Object> message = new HashMap<>();
    message.put("type", "status");
    message.put("status", executionStatus);
    if (StateManager.isDeveloperSession(sessionId) && unusedComponents != null && !unusedComponents.isEmpty()) {
      message.put("toastDuration", 10);
      final List<String> unusedComponentsListItems = unusedComponents
          .entrySet()
          .stream()
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

  private static String unusedComponentToMarkdownLi(final String name, final Integer unusedCount) {
    final String userFriendlyName = name.substring(name.lastIndexOf(".") + 1).replace("Component", "");
    return "- " + userFriendlyName + " - _" + unusedCount + "_";

  }

  // TODO MAKE STATIC
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

      watcher = DirectoryWatcher.builder().path(directory).fileHasher(FileHasher.LAST_MODIFIED_TIME).listener(event -> {
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
                session2WsChannel.keySet().forEach(id -> sendCompilationError(id, "App file was deleted."));
              }
            }
            case CREATE -> {
              if (changedFile.equals(watchedFile)) {
                LOG.warn("App file {} recreated. Attempting to reload from the new file.", watchedFile);
                notifyReload();
              }
            }
            case OVERFLOW -> {
              LOG.warn(
                  "Too many file events. Some events may have been skipped or lost. If the app is not up to date, you may want to perform another edit to trigger a reload.");
            }
            case null, default ->
                LOG.warn("File changed: {} but event type is not managed: {}.", changedFile, event.eventType());
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
