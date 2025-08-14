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
package tech.catheu.jeamlit.core;

/**
 * Exception thrown when two widgets have the same automatically generated or explicit key.
 * This mimics Streamlit's DuplicateWidgetID error behavior.
 */
class DuplicateWidgetIDException extends RuntimeException {

    private DuplicateWidgetIDException(final String message) {
        super(message);
    }

    protected static DuplicateWidgetIDException of(final JtComponent<?> component) {
        return new DuplicateWidgetIDException(String.format(
                "There are multiple identical %s widgets with the same key='%s'. " + "To fix this, please pass a unique key argument to each widget.",
                component.getClass().getName(),
                component.getKey()));
    }
}