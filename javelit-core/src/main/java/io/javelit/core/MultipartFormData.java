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
import java.util.Set;

/**
 * Framework-agnostic abstraction for multipart form data.
 * Can be implemented for both Undertow (FormData) and Servlet (Part) APIs.
 */
public interface MultipartFormData {

    /**
     * Returns all field names in the form data
     */
    Set<String> getFieldNames();

    /**
     * Returns the first field value for a given field name, or null if not present
     */
    @Nullable FormField getFirst(String fieldName);

    /**
     * Represents a single field in multipart form data
     */
    interface FormField {

        /**
         * Returns true if this field is a file upload
         */
        boolean isFile();

        /**
         * Returns the filename if this is a file field, null otherwise
         */
        @Nullable String getFileName();

        /**
         * Returns the content type of the field
         */
        @Nullable String getContentType();

        /**
         * Reads the field data as a byte array.
         * This is a blocking operation.
         *
         * @throws IOException if reading fails
         */
        byte[] getBytes() throws IOException;
    }
}
