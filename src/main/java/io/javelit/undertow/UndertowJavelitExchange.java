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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import io.javelit.http.JavelitCookie;
import io.javelit.http.JavelitHttpExchange;
import io.javelit.http.JavelitMultiPart;
import io.javelit.http.JavelitSession;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.session.Session;
import io.undertow.util.HttpString;
import io.undertow.util.Sessions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static io.javelit.core.utils.LangUtils.optional;

class UndertowJavelitExchange implements JavelitHttpExchange {
  private static final FormParserFactory FORM_PARSER_FACTORY;

  private final HttpServerExchange delegate;

  static {
    final FormParserFactory.Builder parserBuilder = FormParserFactory.builder();
    parserBuilder.setDefaultCharset(StandardCharsets.UTF_8.name());
    FORM_PARSER_FACTORY = parserBuilder.build();
  }

  private UndertowJavelitExchange(final @Nonnull HttpServerExchange delegate) {
    this.delegate = delegate;
  }

  public static JavelitHttpExchange of(final @Nonnull HttpServerExchange exchange) {
    return new UndertowJavelitExchange(exchange);
  }

  @Override
  public String method() {
    return delegate.getRequestMethod().toString();
  }

  @Override
  public String path() {
    return delegate.getRequestPath();
  }

  @Override
  public Map<String, Deque<String>> queryParameters() {
    return delegate.getQueryParameters();
  }

  @Override
  public String firstHeader(final String name) {
    return delegate.getRequestHeaders().getFirst(name);
  }

  @Override
  public List<String> headers(final String name) {
    return delegate.getRequestHeaders().get(name);
  }

  @Override
  public @Nullable JavelitCookie cookie(final String name) {
    final var utCookie = delegate.getRequestCookie(name);
    if (utCookie == null) {
      return null;
    }
    return UndertowJavelitCookie.of(utCookie);
  }

  @Override
  public InputStream inputBody() throws IOException {
    return delegate.getInputStream();
  }

  @Override
  public String getRequestURL() {
    return delegate.getRequestURL();
  }

  @Override
  public @Nullable String getRemoteHostAddress() {
    return optional(delegate.getSourceAddress())
        .map(InetSocketAddress::getAddress)
        .map(InetAddress::getHostAddress)
        .orElse(null);
  }

  @Override
  public String getRelativePath() {
    return delegate.getRelativePath();
  }

  @Override
  public @Nullable JavelitMultiPart getMultiPartFormData() throws IOException {
    return UndertowJavelitMultiPart.of(delegate);
  }

  @Override
  public void setStatus(final int status) {
    delegate.setStatusCode(status);
  }

  @Override
  public void setHeader(final String name, final String value) {
    delegate.getResponseHeaders().put(HttpString.tryFromString(name), value);
  }

  @Override
  public void addHeader(final String name, final String value) {
    delegate.getResponseHeaders().add(HttpString.tryFromString(name), value);
  }

  @Override
  public void setCookie(final @Nonnull JavelitCookie cookie) {
    final Cookie undertowCookie = new CookieImpl(cookie.getName(), cookie.getValue())
        .setHttpOnly(cookie.isHttpOnly())
        .setSecure(cookie.isSecure())
        .setMaxAge(cookie.getMaxAge())
        .setPath(cookie.getPath())
        .setSameSite(cookie.isSameSite())
        .setComment(cookie.getComment())
        .setExpires(cookie.getExpires())
        .setVersion(cookie.getVersion())
        .setDomain(cookie.getDomain());
    if (cookie.getSameSiteMode() != null) {
      undertowCookie.setSameSiteMode(cookie.getSameSiteMode());
    }
    delegate.setResponseCookie(undertowCookie);
  }

  @Override
  public List<JavelitCookie> responseCookies() {
    final ArrayList<JavelitCookie> cookies = new ArrayList<>();
    for (final Cookie undertowCookie : delegate.responseCookies()) {
      cookies.add(UndertowJavelitCookie.of(undertowCookie));
    }
    return cookies;
  }

  @Override
  public OutputStream outputBody() throws IOException {
    return delegate.getOutputStream();
  }

  @Override
  public void write(final String data) {
    delegate.getResponseSender().send(data);
  }

  @Override
  public void write(final ByteBuffer buffer) {
    delegate.getResponseSender().send(buffer);
  }

  @Override
  public JavelitSession getOrCreateSession() {
    final Session s = Sessions.getOrCreateSession(delegate);
    return UndertowJavelitSession.of(s);
  }

  @Override
  public @Nullable JavelitSession getSession() {
    // FIXME ASAP CYRIL - check if this is equivalent to the current server logic
    final Session s = Sessions.getSession(delegate);
    return UndertowJavelitSession.of(s);
  }

}
