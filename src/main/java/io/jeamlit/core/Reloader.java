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

import java.lang.reflect.Method;

// not using an interface to not expose this in the public API
abstract class Reloader {

    enum ReloadStrategy {
        /**
         * only reload the classes previous classpath will be used if it exists
         * no maven/gradle build
         */
        CLASS,
        BUILD_CLASSPATH_AND_CLASS
    }

    abstract Method reload();
}
