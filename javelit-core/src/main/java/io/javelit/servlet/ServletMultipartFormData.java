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

import io.javelit.core.MultipartFormData;
import jakarta.annotation.Nullable;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servlet implementation of MultipartFormData that wraps Jakarta Servlet Part API.
 */
public class ServletMultipartFormData implements MultipartFormData {

    private final HttpServletRequest servletRequest;

    public ServletMultipartFormData(final HttpServletRequest servletRequest) throws ServletException, IOException {
        this.servletRequest = servletRequest;

        // Parse parts to ensure multipart data is available
        // This triggers the parsing if not already done
        servletRequest.getParts();
    }

    @Override
    public Set<String> getFieldNames() {
        try {
            final Collection<Part> parts = servletRequest.getParts();
            return parts.stream()
                .map(Part::getName)
                .collect(Collectors.toSet());
        } catch (final IOException | ServletException e) {
            throw new RuntimeException("Failed to get field names from multipart form data", e);
        }
    }

    @Override
    @Nullable
    public FormField getFirst(final String fieldName) {
        try {
            final Part part = servletRequest.getPart(fieldName);
            if (part == null) {
                return null;
            }
            return new ServletFormField(part);
        } catch (final IOException | ServletException e) {
            throw new RuntimeException("Failed to get field: " + fieldName, e);
        }
    }

    /**
     * Servlet implementation of FormField that wraps Jakarta Servlet Part.
     */
    private static class ServletFormField implements FormField {

        private final Part part;

        ServletFormField(final Part part) {
            this.part = part;
        }

        @Override
        public boolean isFile() {
            // A Part is considered a file if it has a filename
            final String filename = part.getSubmittedFileName();
            return filename != null && !filename.isEmpty();
        }

        @Override
        @Nullable
        public String getFileName() {
            return part.getSubmittedFileName();
        }

        @Override
        @Nullable
        public String getContentType() {
            return part.getContentType();
        }

        @Override
        public byte[] getBytes() throws IOException {
            try (final InputStream inputStream = part.getInputStream()) {
                return inputStream.readAllBytes();
            }
        }
    }
}
