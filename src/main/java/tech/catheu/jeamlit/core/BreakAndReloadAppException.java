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
 * Exception used as a control flow.
 * This exception should be thrown to request to break the current app run and restart it from the beginning.
 * For instance, Jt.switchPage changes the current urlPath then throws this exception.
 * A runner upstream is expected to catch this exception and behave accordingly.
 * This exception should never be surfaced to users.
 *
 * Note: because this exception is used a control flow, it does not collect a stacktrace.
 * Should not be part of the public API. May be removed for a non-exception-based control flow any time.
 */
class BreakAndReloadAppException extends RuntimeException {
    
    BreakAndReloadAppException() {
        super("Requesting internal break—and-reload operation. If this exception is not caught and surfaced in the app, please reach out to support.");
    }

    // very slow operation not necessary here
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}