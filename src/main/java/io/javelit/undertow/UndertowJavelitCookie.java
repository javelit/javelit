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

import java.util.Date;

import io.javelit.http.JavelitCookie;
import io.undertow.server.handlers.Cookie;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class UndertowJavelitCookie implements JavelitCookie {
  private final Cookie delegate;

  private UndertowJavelitCookie(final @Nonnull Cookie delegate) {
    this.delegate = delegate;
  }

  static JavelitCookie of(final @Nullable Cookie delegate) {
    if (delegate == null) {
      return null;
    }
    return new UndertowJavelitCookie(delegate);
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public String getValue() {
    return delegate.getValue();
  }

  @Override
  public JavelitCookie setValue(String value) {
    delegate.setValue(value);
    return this;
  }

  @Override
  public String getPath() {
    return delegate.getPath();
  }

  @Override
  public JavelitCookie setPath(String path) {
    delegate.setPath(path);
    return this;
  }

  @Override
  public String getDomain() {
    return delegate.getDomain();
  }

  @Override
  public JavelitCookie setDomain(String domain) {
    delegate.setDomain(domain);
    return this;
  }

  @Override
  public Integer getMaxAge() {
    return delegate.getMaxAge();
  }

  @Override
  public JavelitCookie setMaxAge(Integer maxAge) {
    delegate.setMaxAge(maxAge);
    return this;
  }

  @Override
  public boolean isDiscard() {
    return delegate.isDiscard();
  }

  @Override
  public JavelitCookie setDiscard(boolean discard) {
    delegate.setDiscard(discard);
    return this;
  }

  @Override
  public boolean isSecure() {
    return delegate.isSecure();
  }

  @Override
  public JavelitCookie setSecure(boolean secure) {
    delegate.setSecure(secure);
    return this;
  }

  @Override
  public int getVersion() {
    return delegate.getVersion();
  }

  @Override
  public JavelitCookie setVersion(int version) {
    delegate.setVersion(version);
    return this;
  }

  @Override
  public boolean isHttpOnly() {
    return delegate.isHttpOnly();
  }

  @Override
  public JavelitCookie setHttpOnly(boolean httpOnly) {
    delegate.setHttpOnly(httpOnly);
    return this;
  }

  @Override
  public Date getExpires() {
    return delegate.getExpires();
  }

  @Override
  public JavelitCookie setExpires(Date expires) {
    delegate.setExpires(expires);
    return this;
  }

  @Override
  public String getComment() {
    return delegate.getComment();
  }

  @Override
  public JavelitCookie setComment(String comment) {
    delegate.setComment(comment);
    return this;
  }

  @Override
  public boolean isSameSite() {
    return delegate.isSameSite();
  }

  @Override
  public JavelitCookie setSameSite(final boolean sameSite) {
    delegate.setSameSite(sameSite);
    return this;
  }

  @Override
  public String getSameSiteMode() {
    return delegate.getSameSiteMode();
  }

  @Override
  public JavelitCookie setSameSiteMode(String mode) {
    delegate.setSameSiteMode(mode);
    return this;
  }
}
