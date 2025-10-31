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
package io.javelit.servlet;

import io.javelit.core.BuildSystem;
import io.javelit.core.JavelitHandler;
import io.javelit.core.JtRunnable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkState;

/**
 * Servlet implementation of Javelit that can be deployed to servlet containers
 * (Tomcat, Jetty, etc.) or embedded in frameworks (Spring Boot, Quarkus, etc.).
 * <p>
 * This servlet handles all HTTP requests and delegates to JavelitHandler for
 * framework-agnostic business logic.
 * <p>
 * Route mapping:
 * - / → Index page (HTML)
 * - /_/media/{hash}?sid={sessionId} → Media files with range request support
 * - /_/upload → File upload endpoint (PUT)
 * - /_/static/* → Static resources (CSS, JS, images)
 * <p>
 * WebSocket endpoint is handled separately by JavelitWebSocketEndpoint.
 * <p>
 * Usage example:
 * <pre>
 * {@code
 * // Create and configure servlet
 * JavelitServlet servlet = JavelitServlet.builder(appPath, port)
 *     .additionalClasspath(classpath)
 *     .headersFile(headersFile)
 *     .buildSystem(BuildSystem.MAVEN)
 *     .build();
 *
 * // Register in servlet container or framework
 * ServletHolder holder = new ServletHolder(servlet);
 * context.addServlet(holder, "/*");
 * }
 * </pre>
 */
@MultipartConfig(
    maxFileSize = 200 * 1024 * 1024,      // 200 MB
    maxRequestSize = 200 * 1024 * 1024,   // 200 MB
    fileSizeThreshold = 10 * 1024 * 1024  // 10 MB (files larger than this go to disk)
)
public class JavelitServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(JavelitServlet.class);

    private JavelitHandler handler;
    private final Builder builder;

    /**
     * Builder for configuring JavelitServlet with the same API as Server.Builder.
     */
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

        public JavelitServlet build() {
            if (buildSystem == null) {
                buildSystem = BuildSystem.inferBuildSystem();
            }
            return new JavelitServlet(this);
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

    private JavelitServlet(final Builder builder) {
        this.builder = builder;
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        try {
            // Initialize JavelitHandler with configuration
            handler = new JavelitHandler(
                builder.appPath,
                builder.appClass,
                builder.appRunnable,
                builder.classpath,
                builder.headersFile,
                builder.buildSystem,
                builder.port
            );

            // Start file watcher for hot-reload during development
            handler.startFileWatcher();

            LOG.info("JavelitServlet initialized successfully on port {}", builder.port);
        } catch (final Exception e) {
            LOG.error("Failed to initialize JavelitServlet", e);
            throw new ServletException("Failed to initialize JavelitServlet", e);
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {

        final String path = req.getPathInfo() != null ? req.getPathInfo() : "/";

        try {
            final ServletHttpRequest request = new ServletHttpRequest(req);
            final ServletHttpResponse response = new ServletHttpResponse(resp);

            // Route to appropriate handler
            if ("/".equals(path)) {
                // Index page
                handler.handleIndex(request, response);
            } else if (path.startsWith("/_/media/")) {
                // Media files
                handler.handleMedia(request, response);
            } else if (path.startsWith("/_/static/")) {
                // Static resources (internal framework resources)
                handler.handleStatic(request, response);
            } else if (path.startsWith("/app/static/")) {
                // Static resources (application-specific files)
                handler.handleStatic(request, response);
            } else {
                // Not found
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Path not found: " + path);
            }
        } catch (final Exception e) {
            LOG.error("Error handling GET request: {}", path, e);
            throw new ServletException("Error handling GET request", e);
        }
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {

        final String path = req.getPathInfo() != null ? req.getPathInfo() : "/";

        try {
            if ("/_/upload".equals(path)) {
                // File upload endpoint
                final ServletHttpRequest request = new ServletHttpRequest(req);
                final ServletHttpResponse response = new ServletHttpResponse(resp);

                // Validate XSRF token before processing upload
                if (!handler.validateXsrf(request, response)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "XSRF validation failed");
                    return;
                }

                handler.handleUpload(request, response);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Path not found: " + path);
            }
        } catch (final Exception e) {
            LOG.error("Error handling PUT request: {}", path, e);
            throw new ServletException("Error handling PUT request", e);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {

        final String path = req.getPathInfo() != null ? req.getPathInfo() : "/";

        try {
            // Validate XSRF for POST requests
            final ServletHttpRequest request = new ServletHttpRequest(req);
            final ServletHttpResponse response = new ServletHttpResponse(resp);

            if (!handler.validateXsrf(request, response)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "XSRF validation failed");
                return;
            }

            // Handle POST requests if needed
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST not supported for: " + path);
        } catch (final Exception e) {
            LOG.error("Error handling POST request: {}", path, e);
            throw new ServletException("Error handling POST request", e);
        }
    }

    @Override
    public void destroy() {
        LOG.info("JavelitServlet shutting down");

        // Stop file watcher
        if (handler != null) {
            handler.stopFileWatcher();
        }

        super.destroy();
    }

    /**
     * Returns the JavelitHandler instance for integration with JavelitWebSocketEndpoint.
     * Public for use by Server and JavelitWebSocketEndpoint.
     */
    public JavelitHandler getHandler() {
        return handler;
    }
}
