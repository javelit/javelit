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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import dev.jbang.dependencies.DependencyResolver;
import dev.jbang.dependencies.ModularClassPath;
import dev.jbang.source.Project;
import dev.jbang.source.Source;
import jakarta.annotation.Nonnull;

import static io.jeamlit.core.utils.LangUtils.optional;

@SuppressWarnings("ImmutableEnumChecker")
// ignore the array field in this enum - see https://errorprone.info/bugpattern/ImmutableEnumChecker - this one is internal only - just ensure compileCmdArgs is not mutated
public enum BuildSystem {
    FATJAR_AND_JBANG() {

        private static final Method DEPENDENCY_COLLECT_REFLECTION;

        static {
            try {
                DEPENDENCY_COLLECT_REFLECTION = Source.class.getDeclaredMethod("collectBinaryDependencies");
                DEPENDENCY_COLLECT_REFLECTION.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }


        @Override
        boolean isUsed() {
            final String jeamlitLocation = BuildSystem.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            // Decode URL encoding (e.g., %20 for spaces)
            final String decodedPath = URLDecoder.decode(jeamlitLocation, StandardCharsets.UTF_8);
            return decodedPath.endsWith("-all.jar");
        }

        // FIXME CYRIL - need to improve the speed of this: cache if: content of the file has not changed
        //   cache dependency resolution if :dependencies has not changed --> return the result from last time
        @Override
        @Nonnull
        String obtainClasspath(@Nonnull Path javaFilePath) {
            final String jeamlitLocation = BuildSystem.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            // Decode URL encoding (e.g., %20 for spaces)
            final StringBuilder cp = new StringBuilder(URLDecoder.decode(jeamlitLocation, StandardCharsets.UTF_8));

            // add jbang style deps
            final Project jbangProject = Project.builder().build(javaFilePath);
            final Source mainSource = jbangProject.getMainSource();
            List<String> dependencies = getDependenciesFrom(mainSource);
            // remove the jeamlit dependencies - it's added to help the IDE plugins but it's not necessary, the FATJAR injects itself
            dependencies = dependencies.stream().filter(e -> !e.startsWith("io.jeamlit:jeamlit:")).toList();
            if (!dependencies.isEmpty()) {
                final DependencyResolver resolver = new DependencyResolver();
                resolver.addDependencies(dependencies);
                final ModularClassPath modularClasspath = resolver.resolve();
                cp.append(File.pathSeparator).append(modularClasspath.getClassPath());
            }

            return cp.toString();
        }

        private static List<String> getDependenciesFrom(final Source mainSource) {
            try {
                return (List<String>) DEPENDENCY_COLLECT_REFLECTION.invoke(mainSource);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    },
    RUNTIME() {
        @Override
        boolean isUsed() {
            return false;
        }

        @Override
        @Nonnull
        String obtainClasspath(@Nonnull Path javaFilePath) {
            return optional(System.getProperty("java.class.path")).orElse("");
        }
    };

    BuildSystem() {}

    abstract boolean isUsed();

    // NOTE: javaFilePath may be required by some build system, even if most won't use it
    @Nonnull
    abstract String obtainClasspath(@Nonnull Path javaFilePath) throws IOException, InterruptedException;

    static BuildSystem inferBuildSystem() {
        for (BuildSystem buildSystem : BuildSystem.values()) {
            if (buildSystem.isUsed()) {
                return buildSystem;
            }
        }

        return BuildSystem.RUNTIME;
    }
}
