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
package io.javelit.undertow;

import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.javelit.core.CoreServer;
import io.javelit.core.JavelitServerConfig;
import io.javelit.http.JavelitHeaders;
import io.javelit.http.JavelitHttpExchange;
import io.javelit.http.JavelitServer;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndertowServer implements JavelitServer {

  private static final Logger LOG = LoggerFactory.getLogger(UndertowServer.class);

  @VisibleForTesting public final int port;
  private final CoreServer coreServer;
  private Undertow server;

  public UndertowServer(final @Nonnull CoreServer coreServer, final int port) {
    this.coreServer = coreServer;
    this.port = port;
  }

  public UndertowServer(final @Nonnull JavelitServerConfig serverConfig) {
    this.coreServer = new CoreServer(serverConfig);
    this.port = serverConfig.getPort();
  }

  public void start() {
    HttpHandler app = new PathHandler()
        // internal static files
        .addPrefixPath("/_/static", resource(new ClassPathResourceManager(getClass().getClassLoader(), "static")))
        .addExactPath("/_/ws",
                      Handlers.websocket(new WebSocketHandler()).addExtension(new PerMessageDeflateHandshake()))
        .addPrefixPath("/", new BlockingHandler(exchange -> {
          final JavelitHttpExchange jtExchange = UndertowJavelitExchange.of(exchange);
          coreServer.handleHttpRequest(jtExchange);
        }));
    final ResourceManager staticRm = buildStaticResourceManager();
    if (staticRm != null) {
      ((PathHandler) app).addPrefixPath("/app/static", resource(staticRm));
    }
    app = new BlockingHandler(new EmbeddedAttachmentHandler(app));
    app = new BlockingHandler(new XsrfAttachmentHandler(app));
    // attach a BROWSER session cookie - this is not the same as app state "session" used downstream
    app = new SessionAttachmentHandler(app,
                                       new InMemorySessionManager("javelit_session"),
                                       new SessionCookieConfig()
                                           .setCookieName(CoreServer.SESSION_ID_COOKIE_KEY)
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
              "Failed to launch the server. Port %s is already in use ? Try changing the getPort. In standalone mode, use --getPort <PORT>".formatted(
                  port),
              e);
        }
      }
      throw e;
    }
    coreServer.start();

    LOG.info("Javelit server started on http://localhost:{}", port);
  }

  public void stop() {
    coreServer.stop();
    if (server != null) {
      server.stop();
    }
  }

  @Override
  public int port() {
    return port;
  }

  private static HttpHandler resource(final @Nonnull ResourceManager resourceManager) {
    return new SetHeaderHandler(new ResourceHandler(resourceManager)
                                    .setDirectoryListingEnabled(false)
                                    .setCacheTime(3600), // 1 hour cache
                                JavelitHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff");
  }

  private @Nullable ResourceManager buildStaticResourceManager() {
    final Path appPath = coreServer.getAppPath();
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

  private class XsrfAttachmentHandler implements HttpHandler {
    private final HttpHandler next;

    public XsrfAttachmentHandler(final @Nonnull HttpHandler next) {
      this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
      coreServer.handleXsrf(UndertowJavelitExchange.of(exchange));
      next.handleRequest(exchange);
    }
  }

  private class EmbeddedAttachmentHandler implements HttpHandler {

    private final HttpHandler next;

    private EmbeddedAttachmentHandler(final @Nonnull HttpHandler next) {
      this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
      coreServer.handleEmbedded(UndertowJavelitExchange.of(exchange));
      next.handleRequest(exchange);
    }
  }

  private class WebSocketHandler implements WebSocketConnectionCallback {

    @Override
    public void onConnect(final WebSocketHttpExchange exchange, final WebSocketChannel channel) {
      final Session currentSession = getHttpSessionFromWebSocket(exchange);
      final String sessionId = coreServer.handleSocketConnect(UndertowJavelitSession.of(currentSession),
                                                              UndertowJavelitWebsocketChannel.of(channel));
      channel.getReceiveSetter().set(new AbstractReceiveListener() {
        @Override
        protected void onFullTextMessage(final WebSocketChannel channel, final BufferedTextMessage message) {
          coreServer.handleSocketFullTextMessage(sessionId, message.getData());
        }

        @Override
        protected void onCloseMessage(final CloseMessage cm, final WebSocketChannel channel) {
          coreServer.handleSocketCloseMessage(sessionId);
        }
      });
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

}
