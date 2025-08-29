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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import dev.jbang.dependencies.DependencyResolver;
import dev.jbang.dependencies.ModularClassPath;
import dev.jbang.source.Project;
import dev.jbang.source.Source;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static tech.catheu.jeamlit.core.utils.JavaUtils.stackTraceString;
import static tech.catheu.jeamlit.core.utils.LangUtils.optional;
import static tech.catheu.jeamlit.core.utils.StringUtils.percentEncode;


class HotReloader {

    protected enum ReloadStrategy {
        ///  only reload the classes previous classpath will be used if it exists
        /// no maven/gradle build
        CLASS,
        BUILD_CLASSPATH_AND_CLASS
    }

    protected enum BuildSystem {
        GRADLE("build.gradle",
               IS_OS_WINDOWS ? "gradlew.bat" : "gradlew",
               "gradle",
               new String[]{"-q", "dependencies", "--configuration runtimeClasspath"},
               new String[]{"classes"}),
        MAVEN("pom.xml",
              IS_OS_WINDOWS ? "mvnw.cmd" : "mvnw",
              "mvn",
              IS_OS_WINDOWS ?
                      new String[]{"-q", "exec:exec", "-Dexec^.executable=cmd", "-Dexec^.args=\"/c echo %classpath\""} :
                      new String[]{"-q", "exec:exec", "-Dexec.executable=echo", "-Dexec.args=\"%classpath\""},
              new String[]{"compile"}),
        VANILLA("__NO_FILE__", "", "", new String[]{}, new String[]{});
        // jbang is not considered a build system, jbang commands can be combined with maven ones - may change later

        private final @Nonnull String buildSystemFile;
        private final @Nonnull String executable;
        private final @Nonnull String[] classpathCmdArgs;
        private final @Nonnull String[] compileCmdArgs;

        BuildSystem(@Nonnull String buildSystemFile, @Nonnull String wrapperFile, @Nonnull String executable, @Nonnull String[] classpathCmdArgs, @Nonnull String[] compileCmdArgs) {
            this.buildSystemFile = buildSystemFile;
            this.classpathCmdArgs = classpathCmdArgs;
            this.compileCmdArgs = compileCmdArgs;

            // use wrapper if found, else use basic executable - NOTE: may be slow
            final File wrapperInFS = lookForFile(wrapperFile, new File(""), 0);
            if (wrapperInFS != null) {
                this.executable = wrapperInFS.getAbsolutePath();
            } else {
                LOG.warn("{} wrapper not found. Will use `{}` directly.", this, executable);
                this.executable = executable;
            }
        }

        static BuildSystem inferBuildSystem() {
            for (BuildSystem buildSystem : BuildSystem.values()) {
                if (new File(buildSystem.buildSystemFile).exists()) {
                    return buildSystem;
                }
            }
            return BuildSystem.VANILLA;
        }

        private String obtainClasspath() throws IOException, InterruptedException {
            final String[] cmd = ArrayUtils.addAll(new String[]{this.executable},
                                                   this.classpathCmdArgs);
            final CmdRunResult cmdResult = runCmd(cmd);
            if (cmdResult.exitCode() != 0) {
                throw new RuntimeException(("""
                        Failed to obtain %s classpath.
                        %s command finished with exit code %s.
                        %s command: %s.
                        Error: %s""").formatted(this,
                                                this,
                                                cmdResult.exitCode,
                                                this,
                                                cmd,
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

        private void compile() throws IOException, InterruptedException {
            if (this == VANILLA) {
                return;
            }
            final String[] cmd = ArrayUtils.addAll(new String[]{this.executable},
                                                   this.compileCmdArgs);
            final CmdRunResult cmdResult = runCmd(cmd);
            if (cmdResult.exitCode() != 0) {
                throw new RuntimeException(("""
                        Failed to compile with maven toolchain.
                        Maven command finished with exit code %s.
                        Maven command: %s.
                        Error: %s""").formatted(cmdResult.exitCode,
                                                cmd,
                                                String.join("\n", cmdResult.outputLines())));
            }
        }

        /**
         * Look for a file. If not found, look into the parent.
         */
        private static File lookForFile(final @Nonnull String filename, final @Nonnull File startDirectory, final int depthLimit) {
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
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                final List<String> outputLines = reader.lines().toList();
                return new CmdRunResult(exitCode, outputLines);
            }
        }
    }

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


    private static final Logger LOG = LoggerFactory.getLogger(HotReloader.class);
    protected static final Path COMPILATION_TARGET_DIR = Paths.get("target/jeamlit/classes")
            .toAbsolutePath();

    private @Nullable URL[] classPathUrls;
    private final JavaCompiler compiler;
    private @Nullable List<String> compilationOptions;
    private final Path javaFile;
    private final AtomicReference<Method> mainMethod = new AtomicReference<>();
    private final Semaphore reloadAvailable = new Semaphore(1, true);
    private final String providedClasspath;
    final BuildSystem buildSystem;

    /**
     * @param providedClasspath java classpath to use. If found, classpath resolved by jbang/maven/gradle will be appended to this classpath.
     * @param javaFile          the Jeamlit app file. If found, file dependencies resolved by jbang will be managed.
     *                          Note: maven, gradle and multi-file is not implemented yet.
     */
    protected HotReloader(final @Nullable String providedClasspath, final Path javaFile) {
        LOG.info("Using provided classpath {}", providedClasspath);
        this.providedClasspath = providedClasspath;

        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (this.compiler == null) {
            throw new RuntimeException(
                    "System java compiler not available. Make sure you're running Jeamlit with a JDK, not a JRE, and that the java compiler is available.");
        }
        this.javaFile = javaFile;
        this.buildSystem = BuildSystem.inferBuildSystem();
    }

    /**
     * Recompile the app class. Returns the Class.
     *
     * @throws CompilationException for any compilation that should be reported to the user in the app
     *                              Note: not implemented yet: re-compile multiple classes, the dependencies of the app class
     */
    protected void reloadFile(final @Nonnull ReloadStrategy reloadStrategy) {
        switch (reloadStrategy) {
            case BUILD_CLASSPATH_AND_CLASS:
                try {
                    buildSystem.compile();
                } catch (Exception e) {
                    throw new CompilationException(e);
                }
                // this will force the recomputation of the classpath
                compilationOptions = null;
                break;
            case CLASS:
                break;
            default:
                throw new RuntimeException("ReloadStrategy not implemented: " + reloadStrategy);
        }

        final @Nonnull List<JavaFileObject> classFiles = compileJavaFile(this.javaFile);
        final @Nullable JavaFileObject mainClassFile = classFiles.stream()
                // inner classes may appear before the main class in the list
                .filter(e -> !e.getName().contains("$")).findFirst().orElse(null);
        if (mainClassFile == null) {
            throw new CompilationException(
                    "Could not determine class name in file %s. File is empty, invalid or there are only inner classes?".formatted(
                            javaFile));
        }
        try (final HotClassLoader loader = new HotClassLoader(this.classPathUrls,
                                                              getClass().getClassLoader())) {
            for (final JavaFileObject classFile : classFiles) {
                final String className = classNameFor(classFile);
                final byte[] classBytes = loadClassBytes(classFile);
                final Class<?> appClass = loader.defineClass(className, classBytes);
                if (classFile == mainClassFile) {
                    mainMethod.set(appClass.getMethod("main", String[].class));
                }
            }
        } catch (IOException | NoSuchMethodException e) {
            throw new CompilationException(e);
        }
    }

    /**
     * convert a JavaFileObject to a className
     * target/jeamlit/classes/com/example/MyClass.class -> com.example.MyClass
     * target/jeamlit/classes/MyClass.class --> MyClass
     * compatible with inner classes
     */
    private static String classNameFor(final @Nonnull JavaFileObject classFile) {
        return COMPILATION_TARGET_DIR.toUri().relativize(classFile.toUri()).toString()
                .replace(File.separatorChar, '.').replace(".class", "");
    }

    /// @throws CompilationException if it is called for the first time, the files have never been compiled and and the compilation failed
    protected void runApp(final String sessionId) {
        StateManager.beginExecution(sessionId);
        // if necessary: load the app for the first time
        if (mainMethod.get() == null) {
            try {
                reloadAvailable.acquire();
                if (mainMethod.get() == null) {
                    LOG.warn("Compiling the app for the first time.");
                    reloadFile(HotReloader.ReloadStrategy.BUILD_CLASSPATH_AND_CLASS);
                }
            } catch (InterruptedException e) {
                Jt.error("Compilation interrupted.").use();
            } catch (Exception e) {
                if (!(e instanceof CompilationException)) {
                    LOG.error("Unknown error type: {}", e.getClass(), e);
                }
                throw e;
            } finally {
                reloadAvailable.release();
            }
        }

        boolean doRerun = false;
        Consumer<String> runAfterBreak = null;
        try {
            mainMethod.get().invoke(null, new Object[]{new String[]{}});
        } catch (Exception e) {
            if (!(e instanceof InvocationTargetException || e instanceof DuplicateWidgetIDException || e instanceof PageRunException)) {
                LOG.error("Unexpected error type: {}", e.getClass(), e);
            }
            if (e.getCause() instanceof BreakAndReloadAppException u) {
                runAfterBreak = u.runAfterBreak;
                doRerun = true;
            } else if (e.getCause() != null && e.getCause()
                    .getCause() instanceof BreakAndReloadAppException u) {
                runAfterBreak = u.runAfterBreak;
                doRerun = true;
            } else {
                @Language("markdown") final String errorMessage = buildErrorMessage(e);
                // Send error as a component usage - its lifecycle is managed like all other components
                Jt.error(errorMessage).use();
            }
        } finally {
            StateManager.endExecution();
        }

        if (doRerun) {
            if (runAfterBreak != null) {
                runAfterBreak.accept(sessionId);
            }
            runApp(sessionId);
        }
    }


    private static @Language("markdown") @NotNull String buildErrorMessage(Throwable error) {
        if (error instanceof PageRunException) {
            error = error.getCause();
        }
        if (error instanceof InvocationTargetException) {
            error = error.getCause();
        }
        final String exceptionSimpleName = error.getClass().getSimpleName();
        final String errorMesssage = optional(error.getMessage()).orElse("[ no error message ]");
        final String stackTrace = stackTraceString(error);
        final String googleLink = "https://www.google.com/search?q=" + percentEncode(
                exceptionSimpleName + " " + errorMesssage);
        final String chatGptLink = "https://chatgpt.com/?q=" + percentEncode(String.join("\n",
                                                                                         List.of("Help me fix the following issue. I use Java Jeamlit and got:",
                                                                                                 exceptionSimpleName,
                                                                                                 errorMesssage,
                                                                                                 stackTrace)));

        return """
                **%s**: %s
                
                **Stacktrace**:
                ```
                %s
                ```
                [Ask Google](%s) • [Ask ChatGPT](%s)
                """.formatted(exceptionSimpleName,
                              errorMesssage,
                              stackTrace,
                              googleLink,
                              chatGptLink);
    }

    // return the fully qualified classname of the compiled file
    private @Nonnull List<JavaFileObject> compileJavaFile(final Path javaFile) {
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null,
                                                                                   null,
                                                                                   null)) {
            final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(
                    List.of(javaFile));
            if (compilationOptions == null) {
                final String classpath = buildClasspath(providedClasspath, javaFile);
                LOG.info("Using classpath {}", classpath);
                this.compilationOptions = List.of("-d",
                                                  COMPILATION_TARGET_DIR.toString(),
                                                  "-cp",
                                                  classpath,
                                                  // NOTE: reconsider this for a maven-backed project
                                                  "-sourcepath",
                                                  javaFile.toAbsolutePath().getParent().toString(),
                                                  "-proc:none");
                this.classPathUrls = createClassPathUrls(classpath);
            }
            // Compile
            final var task = (JavacTask) compiler.getTask(null,
                                                          fileManager,
                                                          diagnosticsCollector,
                                                          compilationOptions,
                                                          null,
                                                          compilationUnits);
            final Iterable<? extends CompilationUnitTree> l = task.parse();
            if (!l.iterator().hasNext()) {
                throw new CompilationException(
                        "Failed to compile Java file: %s. Reading of the file failed. This could be caused by file I/O errors,unsupported file formats, invalid encoding, etc...".formatted(
                                javaFile));
            }
            task.analyze();
            final Iterable<JavaFileObject> generated = (Iterable<JavaFileObject>) task.generate();
            final boolean success = diagnosticsCollector.getDiagnostics().stream()
                    .noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
            if (success) {
                LOG.info("Successfully compiled {}", javaFile);
                return StreamSupport.stream(generated.spliterator(), false).toList();
            } else {
                final String errorMessage = diagnosticsCollector.getDiagnostics().stream()
                        .map(d -> String.format("[%s] %s:%d:%d %s",
                                                d.getKind(),
                                                // ERROR / WARNING / NOTE
                                                d.getSource() != null ?
                                                        d.getSource().getName() :
                                                        "<unknown source>",
                                                d.getLineNumber(),
                                                d.getColumnNumber(),
                                                d.getMessage(null)))
                        .collect(Collectors.joining("\n\n"));


                LOG.error("Compilation failed for {}: \n{}", javaFile, errorMessage);
                throw new CompilationException(errorMessage);
            }
        } catch (IOException | IllegalStateException e) {
            LOG.error("Error compiling file {}", javaFile, e);
            throw new CompilationException(e);
        }
    }

    private @Nonnull byte[] loadClassBytes(final @Nonnull JavaFileObject classFile) {
        try {
            final Path classFilePath = Paths.get(classFile.toUri());
            if (!Files.exists(classFilePath)) {
                LOG.error("Class file not found: {} for original class object {}",
                          classFile,
                          classFile.getName());
                throw new RuntimeException("Class file not found: %s for original class name %s".formatted(
                        classFile,
                        classFile.getName()));
            }
            return Files.readAllBytes(classFilePath);
        } catch (IOException e) {
            LOG.error(
                    "Error loading class bytes for {}. You may want to retry. If the error persists, please reach out to support.",
                    classFile.getName(),
                    e);
            // note: this is not a CompilationException. This is most likely to be an implementation error.
            throw new CompilationException(e);
        }
    }

    private static URL[] createClassPathUrls(final String classpath) {
        final String[] paths = classpath.split(File.pathSeparator);
        final URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            final File file = new File(paths[i]);
            try {
                urls[i] = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        return urls;
    }


    private static final class HotClassLoader extends URLClassLoader {

        private HotClassLoader(final URL[] urls, final ClassLoader parent) {
            super(urls, parent);
        }

        private Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    /**
     * combine all required classpaths.
     */
    private String buildClasspath(final @Nullable String providedClasspath, final @Nonnull Path javaFilePath) {
        final StringBuilder cp = new StringBuilder();

        // add provided classpath
        if (providedClasspath != null && !providedClasspath.isEmpty()) {
            LOG.info("User-provided classpath added to the classpath successfully.");
            LOG.debug("Added from user-provided input: {}", providedClasspath);
            cp.append(File.pathSeparator).append(providedClasspath);
        }

        if (buildSystem != BuildSystem.VANILLA) {
            try {
                LOG.info("Found a {} file. Trying to add {} dependencies to the classpath...",
                         buildSystem.buildSystemFile,
                         buildSystem);
                final String classpath = buildSystem.obtainClasspath();
                cp.append(File.pathSeparator).append(classpath);
                LOG.info("{} dependencies added to the classpath successfully.", buildSystem);
                LOG.debug("Added from {}: {}", buildSystem, classpath);
                // assume classpath contains the jeamlit dependencies
                // NOTE: this is also the codepath that is reached when developing on jeamlit
            } catch (IOException | InterruptedException e) {
                LOG.error(
                        "Failed resolving {} dependencies from {}. {} classpath not injected in app. Please reach out to support with this error if need be.",
                        buildSystem,
                        buildSystem.buildSystemFile,
                        buildSystem,
                        e);
            }
        } else {
            // Add Jeamlit itself to the classpath if it runs from a Jar
            final String jeamlitLocation = HotReloader.class.getProtectionDomain().getCodeSource()
                    .getLocation().getPath();
            // Decode URL encoding (e.g., %20 for spaces)
            final String decodedPath = URLDecoder.decode(jeamlitLocation, StandardCharsets.UTF_8);
            if (decodedPath.endsWith(".jar")) {
                // Running from JAR - add entire JAR (includes all dependencies, assuming it's the -all version)
                LOG.info("Injecting Jeamlit dependencies in classpath: {}", decodedPath);
                cp.append(decodedPath);
            } else {
                LOG.warn(
                        "Jeamlit is not running from its jar, nor running from maven. Support may be limited. Please reach out to support if you encounter classpath issues.");
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

        return cp.toString();
    }

    private static List<String> getDependenciesFrom(final Source mainSource) {
        try {
            return (List<String>) DEPENDENCY_COLLECT_REFLECTION.invoke(mainSource);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
