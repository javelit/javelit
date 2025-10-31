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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.javelit.core.JavelitHandler;
import io.javelit.core.Shared;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket endpoint for Javelit servlet implementation.
 * <p>
 * This endpoint handles WebSocket connections at the "/_/ws" path and delegates
 * message processing to JavelitHandler.
 * <p>
 * The endpoint is registered programmatically (not via annotation) to allow
 * constructor injection of the JavelitHandler instance.
 * <p>
 * Usage:
 * - Register this endpoint programmatically using ServerEndpointConfig.Builder
 * - Pass JavelitHandler via constructor using custom Configurator
 * - The servlet container will handle WebSocket upgrade requests
 * - Messages are routed to JavelitHandler for processing
 */
public class JavelitWebSocketEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(JavelitWebSocketEndpoint.class);
    private static final String SESSION_XSRF_ATTRIBUTE = "XSRF_TOKEN";

    private final JavelitHandler handler;

    // Maps: WebSocket session ID -> page session ID (UUID)
    private static final Map<String, String> websocketSessionToPageSession = new ConcurrentHashMap<>();

    /**
     * Creates a new WebSocket endpoint with the given handler.
     * This constructor is called by the custom ServerEndpointConfig.Configurator.
     */
    public JavelitWebSocketEndpoint(final JavelitHandler handler) {
        this.handler = handler;
    }

    @OnOpen
    public void onOpen(final Session session, final EndpointConfig config) {
        try {
            // Generate new UUID page session ID (one per WebSocket connection)
            final String pageSessionId = UUID.randomUUID().toString();

            // Store mapping from WebSocket session ID to page session ID
            websocketSessionToPageSession.put(session.getId(), pageSessionId);

            // Get HTTP session from user properties (set by configurator during handshake)
            final HttpSession httpSession = (HttpSession) session.getUserProperties().get("httpSession");

            // Extract and store XSRF token from HTTP session via handler
            if (httpSession != null) {
                final String xsrf = (String) httpSession.getAttribute(SESSION_XSRF_ATTRIBUTE);
                try {
                    handler.associateXsrfToken(pageSessionId, xsrf);
                } catch (RuntimeException e) {
                    LOG.error("Failed to associate XSRF token: {}", e.getMessage());
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "No XSRF token"));
                    return;
                }
            } else {
                LOG.error("No HTTP session found in WebSocket handshake");
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "No HTTP session"));
                return;
            }

            // Send session_init message to frontend immediately
            final Map<String, Object> sessionInitMessage = new HashMap<>();
            sessionInitMessage.put("type", "session_init");
            sessionInitMessage.put("sessionId", pageSessionId);
            session.getBasicRemote().sendText(Shared.OBJECT_MAPPER.writeValueAsString(sessionInitMessage));

            // Create wrapper and notify handler with page session ID
            final ServletWebSocketSession wsSession = new ServletWebSocketSession(session, pageSessionId);
            handler.handleWebSocketConnect(wsSession, pageSessionId);

            LOG.debug("WebSocket connection established: ws={}, page={}", session.getId(), pageSessionId);

        } catch (final Exception e) {
            LOG.error("Error handling WebSocket open", e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal error"));
            } catch (final Exception closeEx) {
                LOG.error("Error closing WebSocket after error", closeEx);
            }
        }
    }

    @OnMessage
    public void onMessage(final String message, final Session session) {
        try {
            // Get page session ID
            final String pageSessionId = websocketSessionToPageSession.get(session.getId());

            if (pageSessionId == null) {
                LOG.error("No page session ID for WebSocket: {}", session.getId());
                return;
            }

            // Delegate to handler
            handler.handleWebSocketMessage(pageSessionId, message);

        } catch (final Exception e) {
            LOG.error("Error handling WebSocket message", e);
        }
    }

    @OnClose
    public void onClose(final Session session, final CloseReason reason) {
        try {
            // Get and remove page session ID
            final String pageSessionId = websocketSessionToPageSession.remove(session.getId());

            if (pageSessionId != null) {
                // Notify handler (it will clean up XSRF mapping)
                handler.handleWebSocketClose(pageSessionId);
                LOG.debug("WebSocket connection closed: ws={}, page={}, reason={}",
                    session.getId(), pageSessionId, reason);
            } else {
                LOG.error("WebSocket closed but no page session mapping found: {} - potential orphaned session",
                        session.getId());
            }

        } catch (final Exception e) {
            LOG.error("Error handling WebSocket close", e);
        }
    }

    @OnError
    public void onError(final Session session, final Throwable error) {
        final String pageSessionId = websocketSessionToPageSession.get(session.getId());
        LOG.error("WebSocket error: ws={}, page={}", session.getId(), pageSessionId, error);

        // Clean up on error
        try {
            if (pageSessionId != null) {
                websocketSessionToPageSession.remove(session.getId());
                handler.handleWebSocketClose(pageSessionId);
            }
        } catch (final Exception e) {
            LOG.error("Error during WebSocket error cleanup", e);
        }
    }
}
