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

import io.javelit.http.JavelitSession;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.server.WebSession;

class SpringJavelitSession implements JavelitSession {
  private final WebSession delegate;

  private SpringJavelitSession(final @Nonnull WebSession delegate) {
    this.delegate = delegate;
  }

  static JavelitSession of(final @Nullable WebSession session) {
    return session == null ? null : new SpringJavelitSession(session);
  }

  @Override
  public @Nullable Object getAttribute(@NotNull String name) {
    return delegate.getAttribute(name);
  }

  @Override
  public @Nullable Object setAttribute(@NotNull String name, @NotNull Object value) {
    return delegate.getAttributes().put(name, value);
  }
}
