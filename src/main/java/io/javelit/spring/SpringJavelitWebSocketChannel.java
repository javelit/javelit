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

import io.javelit.http.JavelitWebSocketChannel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import static io.javelit.core.utils.LangUtils.optional;

class SpringJavelitWebSocketChannel implements JavelitWebSocketChannel {

  private final WebSocketSession delegate;

  private SpringJavelitWebSocketChannel(WebSocketSession session) {
    this.delegate = session;
  }

  static SpringJavelitWebSocketChannel of(final @Nonnull WebSocketSession session) {
    return new SpringJavelitWebSocketChannel(session);
  }

  @Override
  public @Nullable String getRemoteHostAddress() {
    return optional(delegate.getHandshakeInfo().getRemoteAddress())
        .map(addr -> addr.getAddress().getHostAddress())
        .orElse(null);
  }

  @Override
  public void sendText(String json) {
    delegate.send(Mono.just(delegate.textMessage(json))).subscribe();
  }
}
