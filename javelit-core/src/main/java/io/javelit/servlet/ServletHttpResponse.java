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

import io.javelit.core.HttpCookie;
import io.javelit.core.HttpResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet implementation of HttpResponse that wraps HttpServletResponse.
 */
public class ServletHttpResponse implements HttpResponse {

    private final HttpServletResponse servletResponse;

    public ServletHttpResponse(final HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    @Override
    public void setStatus(final int statusCode) {
        servletResponse.setStatus(statusCode);
    }

    @Override
    public void setHeader(final String name, final String value) {
        servletResponse.setHeader(name, value);
    }

    @Override
    public void setCookie(final HttpCookie cookie) {
        final Cookie servletCookie = new Cookie(cookie.getName(), cookie.getValue());
        servletCookie.setPath(cookie.getPath());
        servletCookie.setMaxAge(cookie.getMaxAge());
        servletCookie.setHttpOnly(cookie.isHttpOnly());
        servletCookie.setSecure(false); // Match Server.java behavior

        // SameSite attribute handling
        if (cookie.isSameSite()) {
            // Jakarta Servlet 6.0+ supports SameSite via setAttribute
            servletCookie.setAttribute("SameSite", "Strict");
        }

        servletResponse.addCookie(servletCookie);
    }

    @Override
    public void sendText(final String body) throws IOException {
        servletResponse.setContentType("text/html; charset=UTF-8");
        servletResponse.setCharacterEncoding("UTF-8");
        servletResponse.getWriter().write(body);
        servletResponse.getWriter().flush();
    }

    @Override
    public void sendBytes(final byte[] body) throws IOException {
        servletResponse.getOutputStream().write(body);
        servletResponse.getOutputStream().flush();
    }

    @Override
    public void sendBytes(final byte[] body, final int offset, final int length) throws IOException {
        servletResponse.getOutputStream().write(body, offset, length);
        servletResponse.getOutputStream().flush();
    }

    /**
     * Returns the underlying HttpServletResponse for servlet-specific operations.
     */
    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }
}
