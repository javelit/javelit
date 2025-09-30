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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import dev.jbang.dependencies.DependencyResolver;
import dev.jbang.dependencies.ModularClassPath;
import dev.jbang.source.Project;
import dev.jbang.source.Source;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.jeamlit.core.utils.LangUtils.optional;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

@SuppressWarnings("ImmutableEnumChecker") // ignore the array field in this enum - see https://errorprone.info/bugpattern/ImmutableEnumChecker - this one is internal only - just ensure compileCmdArgs is not mutated
public enum BuildSystem {
    GRADLE("build.gradle",
           IS_OS_WINDOWS ? "gradlew.bat" : "gradlew",
           "gradle",
           new String[]{"-q", "dependencies", "--configuration runtimeClasspath"},
           new String[]{"classes"}) {
        @Override
        boolean isUsed() {
            return new File(this.buildSystemFile).exists();
        }
    },
    MAVEN("pom.xml",
          IS_OS_WINDOWS ? "mvnw.cmd" : "mvnw",
          "mvn",
          IS_OS_WINDOWS ?
                  new String[]{"-q", "exec:exec", "-Dexec^.executable=cmd", "-Dexec^.args=\"/c echo %classpath\""} :
                  new String[]{"-q", "exec:exec", "-Dexec.executable=echo", "-Dexec.args=\"%classpath\""},
          new String[]{"compile"}) {
        @Override
        boolean isUsed() {
            return new File(this.buildSystemFile).exists();
        }
    },
    FATJAR_AND_JBANG("java app file", "", "", new String[]{}, new String[]{}) {

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

        @Override
        @Nonnull
        String obtainClasspath(@Nonnull Path javaFilePath, final @Nullable String[] customClasspathCmdArgs) {
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

        @Override
        void compile(@org.jetbrains.annotations.Nullable String[] customCompileCmdArgs) {
            // do nothing
        }
    },
    RUNTIME("java runtime", "", "", new String[]{}, new String[]{}) {
        @Override
        boolean isUsed() {
            return false;
        }

        @Override
        @Nonnull
        String obtainClasspath(@Nonnull Path javaFilePath, final @Nullable String[] customClasspathCmdArgs) {
            return optional(System.getProperty("java.class.path")).orElse("");
        }

        @Override
        void compile(@org.jetbrains.annotations.Nullable String[] customCompileCmdArgs) {
            // do nothing
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(BuildSystem.class);

    final @Nonnull String buildSystemFile;
    // do not use directly - use getExecutable
    private final @Nonnull String baseExecutable;
    // do not use directly - use getExecutable
    private final @Nonnull String wrapperFile;
    private final @Nonnull String[] classpathCmdArgs;
    private final @Nonnull String[] compileCmdArgs;

    private @Nullable String executable;

    BuildSystem(@Nonnull String buildSystemFile,
                @Nonnull String wrapperFile,
                @Nonnull String baseExecutable,
                @Nonnull String[] classpathCmdArgs,
                @Nonnull String[] compileCmdArgs) {
        this.buildSystemFile = buildSystemFile;
        this.baseExecutable = baseExecutable;
        this.wrapperFile = wrapperFile;
        this.classpathCmdArgs = classpathCmdArgs;
        this.compileCmdArgs = compileCmdArgs;
    }

    abstract boolean isUsed();

    // lazily find whether the base executable of the wrapper should be used and return it
    private @Nonnull String getExecutable() {
        if (executable == null) {
            // use wrapper if found, else use basic executable
            final File wrapperInFS = lookForFile(wrapperFile, new File(""), 0);
            if (wrapperInFS != null) {
                this.executable = wrapperInFS.getAbsolutePath();
            } else {
                this.executable = baseExecutable;
            }
        }
        return executable;
    }

    static BuildSystem inferBuildSystem() {
        for (BuildSystem buildSystem : BuildSystem.values()) {
            if (buildSystem.isUsed()) {
                return buildSystem;
            }
        }

        return BuildSystem.RUNTIME;
    }

    // NOTE: javaFilePath may be required by some build system, even if most won't use it
    @Nonnull
    String obtainClasspath(@Nonnull Path javaFilePath,
                           final @Nullable String[] customClasspathCmdArgs) throws IOException, InterruptedException {
        final String[] cmd = ArrayUtils.addAll(new String[]{this.getExecutable()},
                                               customClasspathCmdArgs != null ?
                                                       customClasspathCmdArgs :
                                                       this.classpathCmdArgs);
        final CmdRunResult cmdResult = runCmd(cmd);
        if (cmdResult.exitCode() != 0) {
            throw new RuntimeException("""
                                               Failed to obtain %s classpath.
                                               %s command finished with exit code %s.
                                               %s command: %s.
                                               Error: %s""".formatted(this,
                                                                      this,
                                                                      cmdResult.exitCode,
                                                                      this,
                                                                      Arrays.toString(cmd),
                                                                      String.join("\n", cmdResult.outputLines())));
        }
        if (cmdResult.outputLines().isEmpty()) {
            LOG.warn("{} dependencies command ran successfully, but classpath is empty", this);
            return "";
        } else if (cmdResult.outputLines().size() == 1) {
            return cmdResult.outputLines().getFirst();
        } else {
            LOG.warn(
                    "{} dependencies command ran successfully, but multiple classpath were returned. This can happen with multi-modules projects. Combining all classpath.",
                    this);
            return String.join(File.pathSeparator, cmdResult.outputLines());
        }
    }

    void compile(final @Nullable String[] customCompileCmdArgs) throws IOException, InterruptedException {
        final String[] cmd = ArrayUtils.addAll(new String[]{this.getExecutable()},
                                               customCompileCmdArgs != null ?
                                                       customCompileCmdArgs :
                                                       this.compileCmdArgs);
        final CmdRunResult cmdResult = runCmd(cmd);
        if (cmdResult.exitCode() != 0) {
            throw new RuntimeException("""
                                               Failed to compile with %s toolchain.
                                               %s command finished with exit code %s.
                                               %s command: %s.
                                               Error: %s""".formatted(this,
                                                                      this,
                                                                      cmdResult.exitCode,
                                                                      this,
                                                                      Arrays.toString(cmd),
                                                                      String.join("\n", cmdResult.outputLines())));
        }
    }

    /**
     * Look for a file. If not found, look into the parent.
     */
    private static File lookForFile(final @Nonnull String filename,
                                    final @Nonnull File startDirectory,
                                    final int depthLimit) {
        final File absoluteDirectory = startDirectory.getAbsoluteFile();
        if (new File(absoluteDirectory, filename).exists()) {
            return new File(absoluteDirectory, filename);
        } else {
            File parentDirectory = absoluteDirectory.getParentFile();
            if (parentDirectory != null && depthLimit > 0) {
                return lookForFile(filename, parentDirectory, depthLimit - 1);
            } else {
                return null;
            }
        }
    }

    private record CmdRunResult(int exitCode, List<String> outputLines) {
    }

    private static CmdRunResult runCmd(final @Nonnull String[] cmd) throws IOException, InterruptedException {
        final Runtime run = Runtime.getRuntime();
        final Process pr = run.exec(cmd);
        final int exitCode = pr.waitFor();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream(),
                                                                                    StandardCharsets.UTF_8))) {
            final List<String> outputLines = reader.lines().toList();
            return new CmdRunResult(exitCode, outputLines);
        }
    }
}
