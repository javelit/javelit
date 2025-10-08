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
package io.jeamlit.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exception thrown when two widgets have the same automatically generated or explicit key.
 * Same as DuplicateWidgetID error in Streamlit.
 */
final class DuplicateWidgetIDException extends RuntimeException {

    private static final ObjectMapper NON_PRIVATE_MAPPER = new ObjectMapper();

    static {
        NON_PRIVATE_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NON_PRIVATE);
    }

    private DuplicateWidgetIDException(final String message) {
        super(message);
    }

    static DuplicateWidgetIDException of(final JtComponent<?> component) {
        String componentJson;
        try {
            componentJson = NON_PRIVATE_MAPPER.writeValueAsString(component);
        } catch (JsonProcessingException e) {
            componentJson = "<failed to serialize component - please reach out to support if need be>";
        }
        final String message = """
                There are multiple identical %s widgets with the same key='%s'.
                To fix this, please pass a unique key argument to each widget.
                Component: %s
                """.formatted(component.getClass().getName(),
                              component.getKey(), componentJson);
        return new DuplicateWidgetIDException(message);
    }
}
