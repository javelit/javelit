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

// note: not a fan of these custom exception but I'll start with this for the moment
// previously been working go style with records results containing success bool and error message but it was not much better

/**
 * Exception thrown when compilation of the app fails OR when launching the main method fails
 */
class CompilationException extends RuntimeException {
    protected CompilationException(final Exception e) {
        super(e.getMessage(), e.getCause());
    }

    protected CompilationException(final String errorMessage) {
        super(errorMessage);
    }
}
