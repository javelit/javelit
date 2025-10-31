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

import java.net.BindException;
import java.nio.file.Path;

import io.javelit.servlet.JavelitServlet;
import io.javelit.servlet.JavelitWebSocketEndpoint;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.MultipartConfigElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

/**
 * Embedded Undertow servlet server for running Javelit applications.
 * Uses JavelitServlet and JavelitWebSocketEndpoint for request handling.
 */
public final class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    public final int port;
    private Undertow server;
    private final JavelitServlet servlet;

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

    @Deprecated(forRemoval = true)
    public static Builder builder(final @Nonnull Class<?> appClass, final int port) {
        return new Builder(appClass, port);
    }

    public static Builder builder(final @Nonnull JtRunnable app, final int port) {
        return new Builder(app, port);
    }

    private Server(final Builder builder) {
        this.port = builder.port;

        // Create JavelitServlet using the same builder pattern
        final JavelitServlet.Builder servletBuilder;
        if (builder.appPath != null) {
            servletBuilder = JavelitServlet.builder(builder.appPath, builder.port);
        } else if (builder.appClass != null) {
            servletBuilder = JavelitServlet.builder(builder.appClass, builder.port);
        } else if (builder.appRunnable != null) {
            servletBuilder = JavelitServlet.builder(builder.appRunnable, builder.port);
        } else {
            throw new IllegalArgumentException("Either appPath, appClass, or appRunnable must be provided");
        }

        // Only set buildSystem if appClass is not used (to avoid IllegalStateException)
        if (builder.appClass == null && builder.buildSystem != null) {
            servletBuilder.buildSystem(builder.buildSystem);
        }

        this.servlet = servletBuilder
                .additionalClasspath(builder.classpath)
                .headersFile(builder.headersFile)
                .build();
    }

    public void start() {
        try {
            // Configure WebSocket support with programmatic endpoint registration and compression
            final WebSocketDeploymentInfo webSockets = new WebSocketDeploymentInfo()
                    .addExtension(new PerMessageDeflateHandshake())  // Enable WebSocket compression
                    .addEndpoint(jakarta.websocket.server.ServerEndpointConfig.Builder
                            .create(JavelitWebSocketEndpoint.class, "/_/ws")
                            .configurator(new jakarta.websocket.server.ServerEndpointConfig.Configurator() {
                                @Override
                                public <T> T getEndpointInstance(Class<T> endpointClass) {
                                    // Inject handler via constructor
                                    return (T) new JavelitWebSocketEndpoint(servlet.getHandler());
                                }

                                @Override
                                public void modifyHandshake(jakarta.websocket.server.ServerEndpointConfig config,
                                                            jakarta.websocket.server.HandshakeRequest request,
                                                            jakarta.websocket.HandshakeResponse response) {
                                    // Try to get HTTP session from standard WebSocket API
                                    final Object httpSessionObj = request.getHttpSession();
                                    if (httpSessionObj instanceof jakarta.servlet.http.HttpSession httpSession) {
                                        config.getUserProperties().put("httpSession", httpSession);
                                        LOG.debug("Associated WebSocket with HTTP session: {}", httpSession.getId());
                                    } else {
                                        // Fallback: Parse javelit-session-id cookie manually
                                        // This is needed because some servlet containers don't provide
                                        // the HTTP session in WebSocket handshakes
                                        final java.util.List<String> cookieHeaders = request.getHeaders().get("Cookie");
                                        if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
                                            final String cookieHeader = cookieHeaders.get(0);
                                            if (cookieHeader != null && !cookieHeader.isBlank()) {
                                                final String[] cookies = cookieHeader.split(";");
                                                for (String cookie : cookies) {
                                                    cookie = cookie.trim();
                                                    if (cookie.startsWith("javelit-session-id=")) {
                                                        final String sessionId = cookie.substring("javelit-session-id=".length());
                                                        config.getUserProperties().put("httpSessionId", sessionId);
                                                        LOG.debug("Stored HTTP session ID from javelit-session-id cookie: {}", sessionId);
                                                        break;
                                                    }
                                                }
                                            }
                                        }

                                        if (!config.getUserProperties().containsKey("httpSessionId")) {
                                            LOG.warn("No HTTP session found in WebSocket handshake - XSRF token may not be available");
                                        }
                                    }
                                }
                            })
                            .build());

            // Create deployment info with session management
            final DeploymentInfo deploymentInfo = Servlets.deployment()
                    .setClassLoader(Server.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName("javelit.war")
                    .setServletSessionConfig(new io.undertow.servlet.api.ServletSessionConfig()
                            .setName("javelit-session-id")
                            .setHttpOnly(true)
                            .setMaxAge(86400 * 7) // 7 days
                            .setPath("/")
                    )
                    .addServlet(
                            Servlets.servlet("JavelitServlet", JavelitServlet.class,
                                    () -> new io.undertow.servlet.api.InstanceHandle<>() {
                                        @Override
                                        public JavelitServlet getInstance() {
                                            return servlet;
                                        }

                                        @Override
                                        public void release() {
                                            // No-op: servlet is managed by Server
                                        }
                                    })
                                    .addMapping("/*")
                                    .setLoadOnStartup(1)
                                    .setMultipartConfig(new MultipartConfigElement(
                                            null,             // use default temp dir
                                            200 * 1024 * 1024,        // Per-file limit (200 MB)
                                            200 * 1024 * 1024,        // Total request limit (200 MB)
                                            10 * 1024 * 1024          // Memory vs. disk cutoff (10 MB)
                                    ))
                    )
                    .addServletContextAttribute(
                            WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                            webSockets
                    );

            // Deploy servlet
            final DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
            manager.deploy();

            // Build and start Undertow server with optimized static resource handling
            final PathHandler pathHandler = new PathHandler(manager.start());

            // Add optimized static resource handlers (bypass servlet for better performance)
            // Internal framework resources from classpath
            pathHandler.addPrefixPath("/_/static",
                    createResourceHandler(new ClassPathResourceManager(
                            Server.class.getClassLoader(), "static")));

            // Application-specific resources from filesystem (if they exist)
            final ResourceManager appStaticRm = buildStaticResourceManager();
            if (appStaticRm != null) {
                pathHandler.addPrefixPath("/app/static", createResourceHandler(appStaticRm));
            }

            server = Undertow.builder()
                    .addHttpListener(port, "0.0.0.0")
                    .setHandler(pathHandler)
                    .build();

            server.start();

            LOG.info("Javelit server started on http://localhost:{}", port);

        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof BindException) {
                if (e.getMessage().contains("Address already in use")) {
                    throw new RuntimeException(
                            "Failed to launch the server. Port %s is already in use? Try changing the port. In standalone mode, use --port <PORT>".formatted(
                                    port),
                            e);
                }
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Javelit server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop();
            LOG.info("Javelit server stopped");
        }
    }

    /**
     * Builds a ResourceManager for application-specific static files.
     * Returns null if no static directory exists.
     */
    private @Nullable ResourceManager buildStaticResourceManager() {
        final Path appStaticPath = servlet.getHandler().getAppStaticPath();
        if (appStaticPath != null) {
            LOG.info("Serving application static files from: {}", appStaticPath.toAbsolutePath());
            return new PathResourceManager(appStaticPath, 100);
        }
        return null;
    }

    /**
     * Creates an optimized ResourceHandler with security headers and caching.
     */
    private static HttpHandler createResourceHandler(final @Nonnull ResourceManager resourceManager) {
        return new SetHeaderHandler(
                new ResourceHandler(resourceManager)
                        .setDirectoryListingEnabled(false)
                        .setCacheTime(3600), // 1 hour cache
                "X-Content-Type-Options",
                "nosniff"
        );
    }
}
