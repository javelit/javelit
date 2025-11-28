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

import java.time.Duration;
import java.util.Date;

import io.javelit.http.JavelitCookie;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;

class SpringJavelitCookie implements JavelitCookie {
  private String name;
  private String value;
  private String path = "/";
  private String domain;
  private Integer maxAge;
  private boolean discard;
  private boolean secure;
  private int version;
  private boolean httpOnly;
  private Date expires;
  private String comment;
  private boolean sameSite;
  private String sameSiteMode;

  private SpringJavelitCookie(final @Nonnull HttpCookie cookie) {
    this.name = cookie.getName();
    this.value = cookie.getValue();
  }

  private SpringJavelitCookie(final @Nonnull ResponseCookie cookie) {
    this.name = cookie.getName();
    this.value = cookie.getValue();
    this.path = cookie.getPath();
    this.domain = cookie.getDomain();
    this.maxAge = (int) cookie.getMaxAge().getSeconds();
    this.secure = cookie.isSecure();
    this.httpOnly = cookie.isHttpOnly();
    this.sameSiteMode = cookie.getSameSite();
    this.sameSite = cookie.getSameSite() != null;
  }

  static JavelitCookie of(final @Nullable HttpCookie cookie) {
    if (cookie == null) {
      return null;
    }
    if (cookie instanceof ResponseCookie responseCookie) {
      return new SpringJavelitCookie(responseCookie);
    }
    return new SpringJavelitCookie(cookie);
  }

  static JavelitCookie of(final @Nullable ResponseCookie cookie) {
    if (cookie == null) {
      return null;
    }
    return new SpringJavelitCookie(cookie);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public JavelitCookie setValue(String value) {
    this.value = value;
    return this;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public JavelitCookie setPath(String path) {
    this.path = path;
    return this;
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public JavelitCookie setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  @Override
  public Integer getMaxAge() {
    return maxAge;
  }

  @Override
  public JavelitCookie setMaxAge(Integer maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  @Override
  public boolean isDiscard() {
    return discard;
  }

  @Override
  public JavelitCookie setDiscard(boolean discard) {
    this.discard = discard;
    return this;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public JavelitCookie setSecure(boolean secure) {
    this.secure = secure;
    return this;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public JavelitCookie setVersion(int version) {
    this.version = version;
    return this;
  }

  @Override
  public boolean isHttpOnly() {
    return httpOnly;
  }

  @Override
  public JavelitCookie setHttpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
    return this;
  }

  @Override
  public Date getExpires() {
    return expires;
  }

  @Override
  public JavelitCookie setExpires(Date expires) {
    this.expires = expires;
    return this;
  }

  @Override
  public String getComment() {
    return comment;
  }

  @Override
  public JavelitCookie setComment(String comment) {
    this.comment = comment;
    return this;
  }

  @Override
  public boolean isSameSite() {
    return sameSite;
  }

  @Override
  public JavelitCookie setSameSite(final boolean sameSite) {
    this.sameSite = sameSite;
    return this;
  }

  @Override
  public String getSameSiteMode() {
    return sameSiteMode;
  }

  @Override
  public JavelitCookie setSameSiteMode(String mode) {
    this.sameSiteMode = mode;
    return this;
  }
}
