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

import jakarta.annotation.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Framework-agnostic abstraction for WebSocket sessions.
 * Can be implemented for both Undertow (WebSocketChannel) and Servlet (javax.websocket.Session) APIs.
 */
public interface WebSocketSession {

    /**
     * Sends a text message to the client
     *
     * @throws IOException if sending fails
     */
    void sendText(String message) throws IOException;

    /**
     * Returns the unique session identifier for this WebSocket connection
     */
    String getSessionId();

    /**
     * Returns the remote address of the WebSocket client, or null if not available
     */
    @Nullable InetSocketAddress getRemoteAddress();
}
