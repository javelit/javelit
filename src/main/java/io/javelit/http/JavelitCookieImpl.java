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
package io.javelit.http;

import java.util.Arrays;
import java.util.Date;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieSameSiteMode;

// copy of undertow CookieImpl
public class JavelitCookieImpl implements JavelitCookie {

  private final String name;
  private String value;
  private String path;
  private String domain;
  private Integer maxAge;
  private Date expires;
  private boolean discard;
  private boolean secure;
  private boolean httpOnly;
  private int version;
  private String comment;
  private boolean sameSite;
  private String sameSiteMode;

  public JavelitCookieImpl(final String name, final String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public JavelitCookieImpl setValue(final String value) {
    this.value = value;
    return this;
  }

  public String getPath() {
    return path;
  }

  public JavelitCookieImpl setPath(final String path) {
    this.path = path;
    return this;
  }

  public String getDomain() {
    return domain;
  }

  public JavelitCookieImpl setDomain(final String domain) {
    this.domain = domain;
    return this;
  }

  public Integer getMaxAge() {
    return maxAge;
  }

  public JavelitCookieImpl setMaxAge(final Integer maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  public boolean isDiscard() {
    return discard;
  }

  public JavelitCookieImpl setDiscard(final boolean discard) {
    this.discard = discard;
    return this;
  }

  public boolean isSecure() {
    return secure;
  }

  public JavelitCookieImpl setSecure(final boolean secure) {
    this.secure = secure;
    return this;
  }

  public int getVersion() {
    return version;
  }

  public JavelitCookieImpl setVersion(final int version) {
    this.version = version;
    return this;
  }

  public boolean isHttpOnly() {
    return httpOnly;
  }

  public JavelitCookieImpl setHttpOnly(final boolean httpOnly) {
    this.httpOnly = httpOnly;
    return this;
  }

  public Date getExpires() {
    return expires;
  }

  public JavelitCookieImpl setExpires(final Date expires) {
    this.expires = expires;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public JavelitCookieImpl setComment(final String comment) {
    this.comment = comment;
    return this;
  }

  @Override
  public boolean isSameSite() {
    return sameSite;
  }

  @Override
  public JavelitCookieImpl setSameSite(final boolean sameSite) {
    this.sameSite = sameSite;
    return this;
  }

  @Override
  public String getSameSiteMode() {
    return sameSiteMode;
  }

  @Override
  public JavelitCookieImpl setSameSiteMode(final String mode) {
    final String m = CookieSameSiteMode.lookupModeString(mode);
    if (m != null) {
      UndertowLogger.REQUEST_LOGGER.tracef("Setting SameSite mode to [%s] for cookie [%s]", m, this.name);
      this.sameSiteMode = m;
      this.setSameSite(true);
    } else {
      UndertowLogger.REQUEST_LOGGER.warnf(UndertowMessages.MESSAGES.invalidSameSiteMode(mode, Arrays.toString(CookieSameSiteMode.values())), "Ignoring specified SameSite mode [%s] for cookie [%s]", mode, this.name);
    }
    return this;
  }

  @Override
  public final int hashCode() {
    int result = 17;
    result = 37 * result + (getName() == null ? 0 : getName().hashCode());
    result = 37 * result + (getPath() == null ? 0 : getPath().hashCode());
    result = 37 * result + (getDomain() == null ? 0 : getDomain().hashCode());
    return result;
  }

  @Override
  public final boolean equals(final Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof Cookie)) {
      return false;
    }
    final Cookie o = (Cookie) other;
    // compare names
    if (getName() == null && o.getName() != null) {
      return false;
    }
    if (getName() != null && !getName().equals(o.getName())) {
      return false;
    }
    // compare paths
    if (getPath() == null && o.getPath() != null) {
      return false;
    }
    if (getPath() != null && !getPath().equals(o.getPath())) {
      return false;
    }
    // compare domains
    if (getDomain() == null && o.getDomain() != null) {
      return false;
    }
    if (getDomain() != null && !getDomain().equals(o.getDomain())) {
      return false;
    }
    // same cookie
    return true;
  }

  @Override
  public final String toString() {
    return "{JavelitCookieImpl@" + System.identityHashCode(this) + " name=" + getName() + " path=" + getPath() + " domain=" + getDomain() + "}";
  }
}
