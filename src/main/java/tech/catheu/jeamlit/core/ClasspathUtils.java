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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import dev.jbang.dependencies.DependencyResolver;
import dev.jbang.dependencies.ModularClassPath;
import dev.jbang.source.Project;
import dev.jbang.source.Source;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

final class ClasspathUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathUtils.class);

    private static final String GRADLE_PROJECT_FILE = "build.gradle";
    private static final String MAVEN_PROJECT_FILE = "pom.xml";
    private static final String MAVEN_WRAPPER_FILE =
            IS_OS_WINDOWS ? "mvnw.cmd" : "mvnw";
    private static final String[] MAVEN_CP_COMMAND = new String[]{"-q", "exec:exec", "-Dexec.executable=echo", "-Dexec.args=\"%classpath\""};
    private static final String[] MAVEN_CP_COMMAND_WINDOWS = new String[]{"-q", "exec:exec", "-Dexec^.executable=cmd", "-Dexec^.args=\"/c echo %classpath\""};
    private static final String[] MAVEN_COMPILE_COMMAND = new String[]{"compile"};

    private static Boolean isGradleProject = null;
    private static Boolean isMavenProject = null;

    private static final Method DEPENDENCY_COLLECT_REFLECTION;

    static {
        try {
            DEPENDENCY_COLLECT_REFLECTION = Source.class.getDeclaredMethod(
                    "collectBinaryDependencies");
            DEPENDENCY_COLLECT_REFLECTION.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private ClasspathUtils() {
    }

    /**
     * Detect and combine all required classpaths.
     * Note: maven and gradle support not implemented yet.
     */
    static String buildClasspath(final @Nullable String providedClasspath, final @Nonnull Path javaFilePath) {
        final StringBuilder cp = new StringBuilder();

        // add provided classpath
        if (providedClasspath != null && !providedClasspath.isEmpty()) {
            LOG.info("User-provided classpath added to the classpath successfully.");
            LOG.debug("Added from user-provided input: {}", providedClasspath);
            cp.append(File.pathSeparator).append(providedClasspath);
        }

        boolean addJeamlitClasspath = true;
        if (isMavenProject()) {
            try {
                LOG.info(
                        "Found a pom.xml file. Trying to add maven dependencies to the classpath...");
                final String mavenClasspath = computeMavenClasspath();
                cp.append(File.pathSeparator).append(mavenClasspath);
                LOG.info("Maven dependencies added to the classpath successfully.");
                LOG.debug("Added from maven: {}", mavenClasspath);
                // assume maven classpath contains the jeamlit dependencies and even respect overrides
                // NOTE: this is also the codepath that is reached when developing jeamlit
                addJeamlitClasspath = false;
            } catch (IOException | InterruptedException e) {
                LOG.error("Failed resolving maven dependencies in pom.xml. Maven classpath not injected in app. Please reach out to support with this error if need be.", e);
            }
        } else if (isGradleProject()) {
            LOG.warn(
                    "Automatic inclusion of classpath with gradle is not implemented. Use --classpath argument to pass manually.");
            // logic will be similar to maven
        }
        if (addJeamlitClasspath) {
            // Add Jeamlit itself to classpath if it runs from a Jar
            final String jeamlitLocation = ClasspathUtils.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            // Decode URL encoding (e.g., %20 for spaces)
            final String decodedPath = java.net.URLDecoder.decode(jeamlitLocation,
                                                                  StandardCharsets.UTF_8);
            if (decodedPath.endsWith(".jar")) {
                // Running from JAR - add entire JAR (includes all dependencies)
                LOG.info("Injecting Jeamlit dependencies in classpath: {}", decodedPath);
                cp.append(decodedPath);
            } else {
                LOG.warn("Jeamlit is not running from its jar, nor running from maven. Support may be limited. Please reach out to support if you encounter classpath issues.");
            }
        }

        // add jbang style deps
        final Project jbangProject = Project.builder().build(javaFilePath);
        final Source mainSource = jbangProject.getMainSource();
        final List<String> dependencies = getDependenciesFrom(mainSource);
        if (!dependencies.isEmpty()) {
            final DependencyResolver resolver = new DependencyResolver();
            resolver.addDependencies(dependencies);
            final ModularClassPath modularClasspath = resolver.resolve();
            cp.append(File.pathSeparator).append(modularClasspath.getClassPath());
        }
        // note: multi file support of jbang not implemented

        return cp.toString();
    }

    static boolean isGradleProject() {
        if (isGradleProject == null) {
            isGradleProject = new File(GRADLE_PROJECT_FILE).exists();
        }
        return  isGradleProject;
    }

    static boolean isMavenProject() {
        if (isMavenProject == null) {
            isMavenProject = new File(MAVEN_PROJECT_FILE).exists();
        }
        return isMavenProject;
    }

    static void rebuild() throws IOException, InterruptedException {
        if (isMavenProject()) {
            compileMaven();
        } else if (isGradleProject()) {
            LOG.warn("Auto rebuild from Gradle projects is not supported yet. Reach out to support for help if need be.");
        } else {
            // no rebuild command to trigger
        }
    }

    private static void compileMaven() throws IOException, InterruptedException {
        final String mavenExecutable = getMavenExecutable();
        final String[] cmd = ArrayUtils.addAll(new String[]{mavenExecutable}, MAVEN_COMPILE_COMMAND);
        final CmdRunResult cmdResult = runCmd(cmd);
        if (cmdResult.exitCode() != 0) {
            throw new RuntimeException(("""
                        Failed to compile with maven toolchain.
                        Maven command finished with exit code %s.
                        Maven command: %s.
                        Error: %s""").formatted(
                    cmdResult.exitCode,
                    cmd, String.join("\n", cmdResult.outputLines())));
        }
    }

    private static List<String> getDependenciesFrom(final Source mainSource) {
        try {
            return (List<String>) DEPENDENCY_COLLECT_REFLECTION.invoke(mainSource);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static String computeMavenClasspath() throws IOException, InterruptedException {
        final String mavenExecutable = getMavenExecutable();
        final String[] mavenCommand = IS_OS_WINDOWS ? MAVEN_CP_COMMAND_WINDOWS : MAVEN_CP_COMMAND;
        final String[] cmd = ArrayUtils.addAll(new String[]{mavenExecutable}, mavenCommand);
        final CmdRunResult cmdResult = runCmd(cmd);
        if (cmdResult.exitCode() != 0) {
            throw new RuntimeException(("""
                        Failed to add maven dependencies to the classpath.
                        Maven command finished with exit code %s.
                        Maven command: %s.
                        Error: %s""").formatted(
                    cmdResult.exitCode,
                    cmd, String.join("\n", cmdResult.outputLines())));
        }
        if (cmdResult.outputLines().isEmpty()) {
            LOG.warn("Maven dependencies command ran successfully, but classpath is empty");
            return "";
        } else if (cmdResult.outputLines().size() == 1) {
            return cmdResult.outputLines().getFirst();
        } else {
            LOG.warn(
                    "Maven dependencies command ran successfully, but multiple classpath were returned. This can happen with multi-modules projects. Combining all classpath.");
            return String.join(":", cmdResult.outputLines());
        }
    }

    private record CmdRunResult(int exitCode, List<String> outputLines) {}

    private static CmdRunResult runCmd(final @Nonnull String[] cmd) throws IOException, InterruptedException {
        final Runtime run = Runtime.getRuntime();
        final Process pr = run.exec(cmd);
        final int exitCode = pr.waitFor();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            final List<String> outputLines = reader.lines().toList();
            return new CmdRunResult(exitCode, outputLines);
        }
    }

    private static @NotNull String getMavenExecutable() {
        final File mavenWrapper = lookForFile(MAVEN_WRAPPER_FILE, new File(""), 0);
        final String mavenExecutable;
        if (mavenWrapper != null) {
            mavenExecutable = mavenWrapper.getAbsolutePath();
        } else {
            LOG.warn("Maven wrapper not found. Trying to use `mvn` directly.");
            mavenExecutable = "mvn";
        }
        return mavenExecutable;
    }

    /**
     * Look for a file. If not found, look into the parent.
     */
    private static File lookForFile(final String filename, final File startDirectory,
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
}
