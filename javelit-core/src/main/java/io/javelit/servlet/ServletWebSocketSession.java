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

import io.javelit.core.WebSocketSession;
import jakarta.annotation.Nullable;
import jakarta.websocket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Servlet implementation of WebSocketSession that wraps jakarta.websocket.Session.
 */
public class ServletWebSocketSession implements WebSocketSession {

    private final Session websocketSession;
    private final String httpSessionId;

    public ServletWebSocketSession(final Session websocketSession, final String httpSessionId) {
        this.websocketSession = websocketSession;
        this.httpSessionId = httpSessionId;
    }

    @Override
    public void sendText(final String message) throws IOException {
        websocketSession.getBasicRemote().sendText(message);
    }

    @Override
    public String getSessionId() {
        return httpSessionId;
    }

    @Override
    @Nullable
    public InetSocketAddress getRemoteAddress() {
        // Jakarta WebSocket API doesn't provide direct access to remote address
        // We can try to extract it from the request URI if available
        try {
            final URI requestURI = websocketSession.getRequestURI();
            if (requestURI != null && requestURI.getHost() != null) {
                final int port = requestURI.getPort() != -1 ? requestURI.getPort() : 80;
                return new InetSocketAddress(requestURI.getHost(), port);
            }
        } catch (final Exception e) {
            // If we can't get the address, return null
        }
        return null;
    }

    /**
     * Returns the underlying Jakarta WebSocket Session for servlet-specific operations.
     */
    public Session getWebsocketSession() {
        return websocketSession;
    }
}
