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

import java.util.concurrent.atomic.AtomicReference;

import io.javelit.core.CoreServer;
import io.javelit.http.JavelitSession;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

class SpringWebSocketHandler implements WebSocketHandler {
  private static final Logger LOG = LoggerFactory.getLogger(SpringWebSocketHandler.class);

  private final CoreServer coreServer;

  SpringWebSocketHandler(final @Nonnull CoreServer coreServer) {
    this.coreServer = coreServer;
  }

  @Override
  public Mono<Void> handle(final WebSocketSession session) {
    final AtomicReference<String> sessionIdRef = new AtomicReference<>();

    // Get HTTP session from attributes (set by SpringServer during handshake)
    final JavelitSession httpSession = session.getAttributes()
        .containsKey("javelit-session") ?
        (JavelitSession) session.getAttributes().get("javelit-session") : null;

    // Handle connection
    final String sessionId = coreServer.handleSocketConnect(
        httpSession,
        SpringJavelitWebSocketChannel.of(session)
    );
    sessionIdRef.set(sessionId);

    // Handle incoming messages and close events
    return session.receive()
        .doOnNext(message -> {
          if (message.getType() == WebSocketMessage.Type.TEXT) {
            try {
              coreServer.handleSocketFullTextMessage(sessionId, message.getPayloadAsText());
            } catch (Exception e) {
              LOG.error("Error handling WebSocket message for session {}", sessionId, e);
            }
          }
        })
        .doOnComplete(() -> {
          coreServer.handleSocketCloseMessage(sessionId);
        })
        .doOnError(error -> {
          LOG.error("WebSocket error for session {}", sessionId, error);
          coreServer.handleSocketCloseMessage(sessionId);
        })
        .then();
  }
}
