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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.javelit.http.JavelitWebSocketChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static io.javelit.core.utils.LangUtils.optional;

class UndertowJavelitWebsocketChannel implements JavelitWebSocketChannel {

  private final WebSocketChannel delegate;

  private UndertowJavelitWebsocketChannel(WebSocketChannel channel) {
    this.delegate = channel;
  }

  static UndertowJavelitWebsocketChannel of(final @Nonnull WebSocketChannel channel) {
    return new UndertowJavelitWebsocketChannel(channel);
  }

  @Override
  public @Nullable String getRemoteHostAddress() {
    return optional(delegate.getSourceAddress())
        .map(InetSocketAddress::getAddress)
        .map(InetAddress::getHostAddress)
        .orElse(null);
  }

  @Override
  public void sendText(String json) {
    WebSockets.sendText(json, delegate, null);
  }
}
