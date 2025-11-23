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

import java.util.Date;

// copy of Undertow cookie
public interface JavelitCookie {

    String getName();

    String getValue();

    JavelitCookie setValue(String value);

    String getPath();

    JavelitCookie setPath(String path);

    String getDomain();

    JavelitCookie setDomain(String domain);

    Integer getMaxAge();

    JavelitCookie setMaxAge(Integer maxAge);

    boolean isDiscard();

    JavelitCookie setDiscard(boolean discard);

    boolean isSecure();

    JavelitCookie setSecure(boolean secure);

    int getVersion();

    JavelitCookie setVersion(int version);

    boolean isHttpOnly();

    JavelitCookie setHttpOnly(boolean httpOnly);

    Date getExpires();

    JavelitCookie setExpires(Date expires);

    String getComment();

    JavelitCookie setComment(String comment);

    boolean isSameSite();

    JavelitCookie setSameSite(final boolean sameSite);

    String getSameSiteMode();

    JavelitCookie setSameSiteMode(final String mode);
}
