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
package io.javelit.spring;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.javelit.core.CoreServer;
import io.javelit.core.JavelitServerConfig;
import io.javelit.http.JavelitHeaders;
import io.javelit.http.JavelitHttpExchange;
import io.javelit.http.JavelitServer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class SpringServer implements JavelitServer {

  private static final Logger LOG = LoggerFactory.getLogger(SpringServer.class);

  @VisibleForTesting
  public final int port;
  private final CoreServer coreServer;
  private DisposableServer disposableServer;

  public SpringServer(final @Nonnull CoreServer coreServer, final int port) {
    this.coreServer = coreServer;
    this.port = port;
  }

  public SpringServer(final @Nonnull JavelitServerConfig serverConfig) {
    this.coreServer = new CoreServer(serverConfig);
    this.port = serverConfig.getPort();
  }

  public void start() {
    // Build WebSocket handler mapping
    final Map<String, WebSocketHandler> wsHandlers = new HashMap<>();
    wsHandlers.put("/_/ws", new SpringWebSocketHandler(coreServer));

    final SimpleUrlHandlerMapping wsHandlerMapping = new SimpleUrlHandlerMapping();
    wsHandlerMapping.setUrlMap(wsHandlers);
    wsHandlerMapping.setOrder(1);

    final WebSocketHandlerAdapter wsAdapter = new WebSocketHandlerAdapter();

    // Build HTTP router
    final RouterFunction<ServerResponse> routes = buildRoutes();

    // Build filters
    final WebFilter xsrfFilter = (exchange, chain) -> {
      final JavelitHttpExchange jtExchange = SpringJavelitExchange.of(exchange);
      coreServer.handleXsrf(jtExchange);
      return chain.filter(exchange);
    };

    final WebFilter embeddedFilter = (exchange, chain) -> {
      final JavelitHttpExchange jtExchange = SpringJavelitExchange.of(exchange);
      coreServer.handleEmbedded(jtExchange);
      return chain.filter(exchange);
    };

    final WebFilter sessionFilter = (exchange, chain) -> {
      // Ensure session is created with proper config
      return exchange.getSession()
          .defaultIfEmpty(exchange.getSession().block())
          .flatMap(session -> {
            // Store session in attributes for WebSocket handler
            exchange.getAttributes().put("javelit-session", SpringJavelitSession.of(session));
            return chain.filter(exchange);
          });
    };

    // Build WebHttpHandler
    final HttpHandler httpHandler = WebHttpHandlerBuilder.webHandler(RouterFunctions.toWebHandler(routes))
        .filter(sessionFilter)
        .filter(embeddedFilter)
        .filter(xsrfFilter)
        .exceptionHandler((exchange, ex) -> {
          LOG.error("Unhandled exception in request", ex);
          exchange.getResponse().setRawStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
          return Mono.empty();
        })
        .build();

    // Create Reactor Netty HTTP server
    final ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

    try {
      disposableServer = HttpServer.create()
          .host("0.0.0.0")
          .port(port)
          .handle(adapter)
          .bindNow();

      coreServer.start();
      LOG.info("Javelit server started on http://localhost:{}", port);
    } catch (Exception e) {
      if (e.getCause() instanceof BindException) {
        throw new RuntimeException(
            "Failed to launch the server. Port %s is already in use ? Try changing the port. In standalone mode, use --port <PORT>".formatted(
                port),
            e);
      }
      throw new RuntimeException("Failed to start Spring server", e);
    }
  }

  private RouterFunction<ServerResponse> buildRoutes() {
    // Internal static files
    final RouterFunction<ServerResponse> internalStatic = nest(
        path("/_/static"),
        route(GET("/**"), this::handleInternalStatic)
    );

    // App static files
    RouterFunction<ServerResponse> appStatic = null;
    final Path staticPath = getStaticPath();
    if (staticPath != null) {
      appStatic = nest(
          path("/app/static"),
          route(GET("/**"), request -> handleAppStatic(request, staticPath))
      );
    }

    // Main HTTP handler for all other requests
    final RouterFunction<ServerResponse> mainHandler = route()
        .GET("/**", this::handleHttpRequest)
        .POST("/**", this::handleHttpRequest)
        .PUT("/**", this::handleHttpRequest)
        .DELETE("/**", this::handleHttpRequest)
        .PATCH("/**", this::handleHttpRequest)
        .build();

    // Combine routes: internal static -> app static -> main handler
    RouterFunction<ServerResponse> combined = internalStatic;
    if (appStatic != null) {
      combined = combined.and(appStatic);
    }
    combined = combined.and(mainHandler);

    return combined;
  }

  private Mono<ServerResponse> handleInternalStatic(ServerRequest request) {
    final String resourcePath = request.path().substring("/_/static/".length());
    final Resource resource = new ClassPathResource("static/" + resourcePath);

    if (!resource.exists()) {
      return ServerResponse.notFound().build();
    }

    return ServerResponse.ok()
        .header(JavelitHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff")
        .header(JavelitHeaders.CACHE_CONTROL, "max-age=3600")
        .bodyValue(resource);
  }

  private Mono<ServerResponse> handleAppStatic(ServerRequest request, Path staticBasePath) {
    final String resourcePath = request.path().substring("/app/static/".length());
    final Path filePath = staticBasePath.resolve(resourcePath).normalize();

    // Security check: ensure the resolved path is still within staticBasePath
    if (!filePath.startsWith(staticBasePath)) {
      return ServerResponse.notFound().build();
    }

    if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
      return ServerResponse.notFound().build();
    }

    final Resource resource = new FileSystemResource(filePath);

    return ServerResponse.ok()
        .header(JavelitHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff")
        .header(JavelitHeaders.CACHE_CONTROL, "max-age=3600")
        .bodyValue(resource);
  }

  private Mono<ServerResponse> handleHttpRequest(ServerRequest request) {
    return request.exchange().getSession()
        .flatMap(session -> {
          final JavelitHttpExchange jtExchange = SpringJavelitExchange.of(request.exchange());
          try {
            coreServer.handleHttpRequest(jtExchange);
            // Response already written by CoreServer
            return Mono.empty();
          } catch (Exception e) {
            LOG.error("Error handling HTTP request", e);
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
          }
        })
        .then(Mono.empty());
  }

  private @Nullable Path getStaticPath() {
    final Path appPath = coreServer.getAppPath();
    if (appPath != null) {
      final Path staticPath = appPath.toAbsolutePath().getParent().resolve("static");
      if (Files.exists(staticPath) && Files.isDirectory(staticPath)) {
        LOG.info("Serving static files from: {}", staticPath.toAbsolutePath());
        return staticPath;
      }
    } else {
      LOG.info("Serving static files from resources static folder");
      // Will use classpath resources instead
    }
    return null;
  }

  public void stop() {
    coreServer.stop();
    if (disposableServer != null) {
      disposableServer.disposeNow();
    }
  }

  @Override
  public int port() {
    return port;
  }
}
