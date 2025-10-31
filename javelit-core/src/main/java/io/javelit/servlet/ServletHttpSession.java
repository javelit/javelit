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
package io.javelit.servlet;

import io.javelit.core.HttpSession;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Servlet implementation of HttpSession that wraps jakarta.servlet.http.HttpSession.
 */
public class ServletHttpSession implements HttpSession {

    private final jakarta.servlet.http.HttpSession servletSession;

    public ServletHttpSession(final jakarta.servlet.http.HttpSession servletSession) {
        this.servletSession = servletSession;
    }

    @Override
    public String getId() {
        return servletSession.getId();
    }

    @Override
    @Nullable
    public Object getAttribute(final String name) {
        return servletSession.getAttribute(name);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        servletSession.setAttribute(name, value);
    }

    @Override
    public Set<String> getAttributeNames() {
        return Set.copyOf(Collections.list(servletSession.getAttributeNames()));
    }

    /**
     * Returns the underlying servlet HttpSession for servlet-specific operations.
     */
    public jakarta.servlet.http.HttpSession getServletSession() {
        return servletSession;
    }
}
