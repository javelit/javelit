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

import jakarta.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkArgument;

// compatible with JRE only
// compatible with IDE hot-reload
public class RunnableReloader extends Reloader {

  private final JtRunnable runnable;

  public RunnableReloader(final @Nonnull JavelitServerConfig config) {
    checkArgument(config.getBuildSystem() == BuildSystem.RUNTIME,
                  "Class reloader only supports RUNTIME build systems.");
    checkArgument(config.getAppRunnable() != null);
    this.runnable = config.getAppRunnable();
  }

  @Override
  AppEntrypoint reload() {
    return new AppEntrypoint(
        runnable,
        Thread.currentThread().getContextClassLoader());
  }
}
