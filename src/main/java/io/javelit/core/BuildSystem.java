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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.jbang.dependencies.DependencyUtil;
import dev.jbang.dependencies.ModularClassPath;
import dev.jbang.source.Project;
import dev.jbang.source.Source;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import static io.javelit.core.utils.LangUtils.optional;

@SuppressWarnings("ImmutableEnumChecker")
// ignore the array field in this enum - see https://errorprone.info/bugpattern/ImmutableEnumChecker - this one is internal only - just ensure compileCmdArgs is not mutated
public enum BuildSystem {
    FATJAR_AND_JBANG() {

        private static final String JAVELIT_DEP = "io.javelit:javelit:";
        private static final Method DEPENDENCY_COLLECT_REFLECTION;

        private static final String VERSION = optional(BuildSystem.class.getPackage())
                .map(Package::getImplementationVersion).orElse(null);

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
            final String javelitLocation = BuildSystem.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            // Decode URL encoding (e.g., %20 for spaces)
            final String decodedPath = URLDecoder.decode(javelitLocation, StandardCharsets.UTF_8);
            return decodedPath.endsWith("-all.jar");
        }

        // FIXME CYRIL - need to improve the speed of this: cache if: content of the file has not changed
        //   cache dependency resolution if :dependencies has not changed --> return the result from last time
        @Override
        @Nonnull
        String obtainClasspath(@Nonnull Path javaFilePath) {
            final String javelitLocation = BuildSystem.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            // Decode URL encoding (e.g., %20 for spaces)
            final StringBuilder cp = new StringBuilder(URLDecoder.decode(javelitLocation, StandardCharsets.UTF_8));

            // add jbang style deps
            final Project jbangProject = Project.builder().build(javaFilePath);
            final Source mainSource = jbangProject.getMainSource();
            List<String> dependencies = getDependenciesFrom(mainSource);
            final List<String> cleanedDependencies = cleanedDependencies(dependencies);
            if (!cleanedDependencies.isEmpty()) {
                final ModularClassPath modularClasspath = DependencyUtil.resolveDependencies(cleanedDependencies,
                                                   // default to maven central
                                                   List.of(),
                                                   false,
                                                   false,
                                                   false,
                                                   true,
                                                   false);
                cp.append(File.pathSeparator).append(modularClasspath.getClassPath());
            }

            return cp.toString();
        }

        private @NotNull List<String> cleanedDependencies(List<String> dependencies) {
            final List<String> cleanedDependencies = new ArrayList<>(dependencies.size());
            for (final String dep : dependencies) {
                if (!dep.startsWith(JAVELIT_DEP)) {
                    // filter the javelit dependencies - it's added to help the IDE plugins but it's not necessary, the FATJAR injects itself
                    cleanedDependencies.add(dep);
                } else if (VERSION != null) {
                    // if running in CLI, ensure the CLI version is greater than the
                    final String requestedVersion = extractJavelitVersion(dep);
                    if (isVersionGreater(requestedVersion, VERSION)) {
                        throw new CompilationException(versionMismatchText(requestedVersion));
                    }
                }
            }
            return cleanedDependencies;
        }

        private @NotNull String versionMismatchText(String requestedVersion) {
            return """
                    Incompatible Javelit versions: version of the CLI is smaller than the version declared in //DEPS.
                    Javelit version of the CLI is %s
                    Javelit version declared in //DEPS is %s
                    
                    Please upgrade the CLI with:
                      jbang app install --fresh --force javelit@javelit
                    or install a specific version with:
                      jbang app install --force io.javelit:javelit:%s:all@fatjar
                    
                    If you are not using jbang to install the CLI, download a more recent Jar with
                      curl -L -o javelit.jar https://repo1.maven.org/maven2/io/javelit/javelit/%s/javelit-%s-all.jar
                    
                    Or consider downgrading the version declared in //DEPS to %s.
                    """.formatted(VERSION, requestedVersion, requestedVersion, requestedVersion, requestedVersion, VERSION);
        }

        private static List<String> getDependenciesFrom(final Source mainSource) {
            try {
                return (List<String>) DEPENDENCY_COLLECT_REFLECTION.invoke(mainSource);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        private static @NotNull String extractJavelitVersion(final @Nonnull String dep) {
            String requestedVersion = dep.substring(JAVELIT_DEP.length());
            // Remove classifier/type if present (e.g., ":all@fatjar" or ":classifier")
            int colonIndex = requestedVersion.indexOf(':');
            if (colonIndex != -1) {
                requestedVersion = requestedVersion.substring(0, colonIndex);
            }
            return requestedVersion;
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

    BuildSystem() {
    }

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

    /**
     * Compare two semantic version strings.
     * @param v1 First version string (e.g., "1.0.0" or "1.0.0-SNAPSHOT")
     * @param v2 Second version string (e.g., "0.58.0" or "0.58.0-SNAPSHOT")
     * @return true if v1 is greater than v2, false otherwise
     */
    @VisibleForTesting
    static boolean isVersionGreater(String v1, String v2) {
        // Remove qualifiers like -SNAPSHOT for comparison
        String[] parts1 = v1.split("-")[0].split("\\.");
        String[] parts2 = v2.split("-")[0].split("\\.");

        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            int num1 = Integer.parseInt(parts1[i]);
            int num2 = Integer.parseInt(parts2[i]);
            if (num1 > num2) {
                return true;
            }
            if (num1 < num2) {
                return false;
            }
        }

        // If equal up to this point, longer version is greater
        return parts1.length > parts2.length;
    }
}
