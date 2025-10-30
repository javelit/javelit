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
package io.javelit.core;

/**
 * Framework-agnostic representation of an HTTP cookie.
 */
public final class HttpCookie {
    private final String name;
    private final String value;
    private final String path;
    private final int maxAge;
    private final boolean httpOnly;
    private final boolean sameSite;

    public HttpCookie(String name, String value, String path, int maxAge, boolean httpOnly, boolean sameSite) {
        this.name = name;
        this.value = value;
        this.path = path;
        this.maxAge = maxAge;
        this.httpOnly = httpOnly;
        this.sameSite = sameSite;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getPath() {
        return path;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public boolean isSameSite() {
        return sameSite;
    }
}
