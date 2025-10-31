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

import io.javelit.core.HttpRequest;
import io.javelit.core.HttpSession;
import io.javelit.core.MultipartFormData;
import jakarta.annotation.Nullable;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servlet implementation of HttpRequest that wraps HttpServletRequest.
 */
public class ServletHttpRequest implements HttpRequest {

    private final HttpServletRequest servletRequest;
    private final ServletHttpSession session;

    public ServletHttpRequest(final HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
        this.session = new ServletHttpSession(servletRequest.getSession(true));
    }

    @Override
    public String getMethod() {
        return servletRequest.getMethod();
    }

    @Override
    public String getPath() {
        final String contextPath = servletRequest.getContextPath();
        final String servletPath = servletRequest.getServletPath();
        final String pathInfo = servletRequest.getPathInfo();

        // Build full path: context + servlet + pathInfo
        final StringBuilder path = new StringBuilder();
        if (contextPath != null && !contextPath.isEmpty()) {
            path.append(contextPath);
        }
        if (servletPath != null && !servletPath.isEmpty()) {
            path.append(servletPath);
        }
        if (pathInfo != null && !pathInfo.isEmpty()) {
            path.append(pathInfo);
        }

        // Return "/" if nothing was accumulated
        return path.isEmpty() ? "/" : path.toString();
    }

    @Override
    @Nullable
    public String getQueryParameter(final String name) {
        return servletRequest.getParameter(name);
    }

    @Override
    public Map<String, List<String>> getQueryParameters() {
        final Map<String, List<String>> result = new HashMap<>();
        final Map<String, String[]> parameterMap = servletRequest.getParameterMap();

        for (final Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            result.put(entry.getKey(), Arrays.asList(entry.getValue()));
        }

        return result;
    }

    @Override
    @Nullable
    public String getHeader(final String name) {
        return servletRequest.getHeader(name);
    }

    @Override
    @Nullable
    public String getCookie(final String name) {
        final Cookie[] cookies = servletRequest.getCookies();
        if (cookies == null) {
            return null;
        }

        final Optional<Cookie> cookie = Arrays.stream(cookies)
            .filter(c -> c.getName().equals(name))
            .findFirst();

        return cookie.map(Cookie::getValue).orElse(null);
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    @Override
    public MultipartFormData parseMultipartForm() throws IOException {
        try {
            return new ServletMultipartFormData(servletRequest);
        } catch (final ServletException e) {
            throw new IOException("Failed to parse multipart form data", e);
        }
    }

    /**
     * Returns the underlying HttpServletRequest for servlet-specific operations.
     */
    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }
}
