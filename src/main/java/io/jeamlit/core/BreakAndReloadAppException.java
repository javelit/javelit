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

import java.util.function.Consumer;

import jakarta.annotation.Nullable;

/**
 * Exception used as a control flow.
 * This exception should be thrown to request to break the current app run and restart it from the beginning.
 * A function that takes in input the sessionId can be provided. It will be run after the current run is breaked, after StateManager.endExecution().
 * This exception should never be surfaced to users.
 *
 * Note: because this exception is used a control flow, it does not collect a stacktrace.
 * Should not be part of the public API. May be removed for a non-exception-based control flow any time.
 */
class BreakAndReloadAppException extends RuntimeException {

    // takes a sessionId and run - this is run after break
    protected final Consumer<String> runAfterBreak;

    BreakAndReloadAppException(final @Nullable Consumer<String> runAfterBreak) {
        super("Requesting internal break—and-reload operation. If this exception is not caught and surfaced in the app, please reach out to support.");
        this.runAfterBreak = runAfterBreak;
    }

    // very slow operation that is not necessary here - override to do nothing
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
