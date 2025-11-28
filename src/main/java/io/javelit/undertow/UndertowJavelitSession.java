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

import io.javelit.http.JavelitSession;
import io.undertow.server.session.Session;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

class UndertowJavelitSession implements JavelitSession {
  private final Session delegate;

  private UndertowJavelitSession(final @Nonnull Session delegate) {
    this.delegate = delegate;
  }

  static JavelitSession of(final @Nullable Session session) {
    return session == null ? null : new UndertowJavelitSession(session);
  }

  @Override
  public @Nullable Object getAttribute(@NotNull String name) {
    return delegate.getAttribute(name);
  }

  @Override
  public @Nullable Object setAttribute(@NotNull String name, @NotNull Object value) {
    return delegate.setAttribute(name, value);
  }
}
