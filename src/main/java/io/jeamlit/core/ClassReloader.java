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

import static com.google.common.base.Preconditions.checkArgument;

// compatible with JRE only
public class ClassReloader extends Reloader {

    private final String appClassName;

    public ClassReloader(Server.Builder builder) {
        checkArgument(builder.buildSystem == BuildSystem.RUNTIME, "Class reloader only supports RUNTIME build systems.");
        checkArgument(builder.appClass != null);
        this.appClassName = builder.appClass.getName();
    }

    @Override
    Method reload() {
        try {
            // Resolve the class using the current context ClassLoader - should make the logic compatible with SpringBoot live reload
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final Class<?> appClass = Class.forName(appClassName, true, cl);
            return appClass.getMethod("main", String[].class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new PageRunException(e);
        }
    }
}
