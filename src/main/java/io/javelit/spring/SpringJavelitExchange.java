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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.javelit.http.JavelitCookie;
import io.javelit.http.JavelitHttpExchange;
import io.javelit.http.JavelitMultiPart;
import io.javelit.http.JavelitSession;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static io.javelit.core.utils.LangUtils.optional;

class SpringJavelitExchange implements JavelitHttpExchange {
  private final ServerWebExchange delegate;
  private final ServerHttpRequest request;
  private final ServerHttpResponse response;
  private final AtomicReference<InputStream> inputStreamRef = new AtomicReference<>();
  private final AtomicReference<OutputStream> outputStreamRef = new AtomicReference<>();

  private SpringJavelitExchange(final @Nonnull ServerWebExchange delegate) {
    this.delegate = delegate;
    this.request = delegate.getRequest();
    this.response = delegate.getResponse();
  }

  public static JavelitHttpExchange of(final @Nonnull ServerWebExchange exchange) {
    return new SpringJavelitExchange(exchange);
  }

  @Override
  public String method() {
    return request.getMethod().name();
  }

  @Override
  public String path() {
    return request.getPath().value();
  }

  @Override
  public Map<String, Deque<String>> queryParameters() {
    return request.getQueryParams().entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> new LinkedList<>(e.getValue())
        ));
  }

  @Override
  public String firstHeader(final String name) {
    return request.getHeaders().getFirst(name);
  }

  @Override
  public List<String> headers(final String name) {
    final List<String> values = request.getHeaders().get(name);
    return values != null ? values : List.of();
  }

  @Override
  public @Nullable JavelitCookie cookie(final String name) {
    final HttpCookie springCookie = request.getCookies().getFirst(name);
    if (springCookie == null) {
      return null;
    }
    return SpringJavelitCookie.of(springCookie);
  }

  @Override
  public InputStream inputBody() throws IOException {
    InputStream existing = inputStreamRef.get();
    if (existing != null) {
      return existing;
    }

    final PipedInputStream pipedInput = new PipedInputStream();
    final PipedOutputStream pipedOutput = new PipedOutputStream(pipedInput);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();

    request.getBody()
        .doOnNext(dataBuffer -> {
          try {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            pipedOutput.write(bytes);
            DataBufferUtils.release(dataBuffer);
          } catch (IOException e) {
            errorRef.set(e);
          }
        })
        .doOnComplete(() -> {
          try {
            pipedOutput.close();
          } catch (IOException e) {
            errorRef.set(e);
          } finally {
            latch.countDown();
          }
        })
        .doOnError(error -> {
          errorRef.set(error);
          latch.countDown();
        })
        .subscribe();

    inputStreamRef.set(pipedInput);
    return pipedInput;
  }

  @Override
  public String getRequestURL() {
    return request.getURI().toString();
  }

  @Override
  public @Nullable String getRemoteHostAddress() {
    return optional(request.getRemoteAddress())
        .map(addr -> addr.getAddress().getHostAddress())
        .orElse(null);
  }

  @Override
  public String getRelativePath() {
    return request.getPath().pathWithinApplication().value();
  }

  @Override
  public @Nullable JavelitMultiPart getMultiPartFormData() throws IOException {
    return SpringJavelitMultiPart.of(delegate);
  }

  @Override
  public void setStatus(final int status) {
    response.setRawStatusCode(status);
  }

  @Override
  public void setHeader(final String name, final String value) {
    response.getHeaders().set(name, value);
  }

  @Override
  public void addHeader(final String name, final String value) {
    response.getHeaders().add(name, value);
  }

  @Override
  public void setCookie(final @Nonnull JavelitCookie cookie) {
    final ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
        .from(cookie.getName(), cookie.getValue())
        .httpOnly(cookie.isHttpOnly())
        .secure(cookie.isSecure())
        .path(cookie.getPath() != null ? cookie.getPath() : "/");

    if (cookie.getMaxAge() != null) {
      builder.maxAge(cookie.getMaxAge());
    }
    if (cookie.getDomain() != null) {
      builder.domain(cookie.getDomain());
    }
    if (cookie.getSameSiteMode() != null) {
      builder.sameSite(cookie.getSameSiteMode());
    }

    response.addCookie(builder.build());
  }

  @Override
  public List<JavelitCookie> responseCookies() {
    final ArrayList<JavelitCookie> cookies = new ArrayList<>();
    for (final ResponseCookie springCookie : response.getCookies().values().stream()
        .flatMap(List::stream)
        .toList()) {
      cookies.add(SpringJavelitCookie.of(springCookie));
    }
    return cookies;
  }

  @Override
  public OutputStream outputBody() throws IOException {
    OutputStream existing = outputStreamRef.get();
    if (existing != null) {
      return existing;
    }

    final PipedOutputStream pipedOutput = new PipedOutputStream();
    final PipedInputStream pipedInput = new PipedInputStream(pipedOutput);

    response.writeWith(
        DataBufferUtils.readInputStream(
            () -> pipedInput,
            response.bufferFactory(),
            4096
        )
    ).subscribe();

    outputStreamRef.set(pipedOutput);
    return pipedOutput;
  }

  @Override
  public void write(final String data) {
    final byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
    final DataBuffer buffer = response.bufferFactory().wrap(bytes);
    response.writeWith(Mono.just(buffer)).subscribe();
  }

  @Override
  public void write(final ByteBuffer buffer) {
    final DataBuffer dataBuffer = response.bufferFactory().wrap(buffer);
    response.writeWith(Mono.just(dataBuffer)).subscribe();
  }

  @Override
  public JavelitSession getOrCreateSession() {
    return delegate.getSession()
        .map(SpringJavelitSession::of)
        .block();
  }

  @Override
  public @Nullable JavelitSession getSession() {
    return delegate.getSession()
        .mapNotNull(session -> session.isStarted() ? SpringJavelitSession.of(session) : null)
        .block();
  }
}
