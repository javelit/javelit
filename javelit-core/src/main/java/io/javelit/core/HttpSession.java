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

import java.util.Set;

/**
 * Framework-agnostic abstraction for HTTP sessions.
 * Can be implemented for both Undertow (Session) and Servlet (HttpSession) APIs.
 */
public interface HttpSession {

    /**
     * Returns the unique session identifier
     */
    String getId();

    /**
     * Returns a session attribute value, or null if not present
     */
    @Nullable Object getAttribute(String name);

    /**
     * Sets a session attribute
     */
    void setAttribute(String name, Object value);

    /**
     * Returns all attribute names in the session
     */
    Set<String> getAttributeNames();
}
