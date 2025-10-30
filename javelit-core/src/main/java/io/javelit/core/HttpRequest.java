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

import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Framework-agnostic abstraction for HTTP requests.
 * Can be implemented for both Undertow (HttpServerExchange) and Servlet (HttpServletRequest) APIs.
 */
public interface HttpRequest {

    /**
     * Returns the HTTP method (GET, POST, PUT, etc.)
     */
    String getMethod();

    /**
     * Returns the request path (e.g., "/", "/_/media/abc123")
     */
    String getPath();

    /**
     * Returns a single query parameter value, or null if not present
     */
    @Nullable String getQueryParameter(String name);

    /**
     * Returns all query parameters as a map of name to list of values
     */
    Map<String, List<String>> getQueryParameters();

    /**
     * Returns a single header value, or null if not present
     */
    @Nullable String getHeader(String name);

    /**
     * Returns a cookie value by name, or null if not present
     */
    @Nullable String getCookie(String name);

    /**
     * Returns the HTTP session, creating one if it doesn't exist
     */
    HttpSession getSession();

    /**
     * Parses multipart form data from the request body.
     * This is a blocking operation.
     *
     * @throws IOException if parsing fails
     */
    MultipartFormData parseMultipartForm() throws IOException;
}
