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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.jeamlit.core.utils.Preconditions.checkArgument;

// requires a JDK
class FileReloader extends Reloader {
    private static final Logger LOG = LoggerFactory.getLogger(FileReloader.class);
    protected static final Path COMPILATION_TARGET_DIR = Paths.get("target/jeamlit/classes")
            .toAbsolutePath();

    private final JavaCompiler compiler;
    private final @Nonnull Path javaFile;
    private final String providedClasspath;
    final BuildSystem buildSystem;
    private static @Nullable String lastDependencyClasspath;
    // this classloader contains dependencies - it will only be reloaded if dependencies change
    private static @Nullable URLClassLoader dependenciesClassloader;
    // this classloader contains the app files - it is always reloaded on file change
    private @Nullable HotClassLoader appClassloader;

    /**
     * Recompile the app class. Returns the Class.
     *
     * @throws CompilationException for any compilation that should be reported to the user in the app
     *                              Note: not implemented yet: re-compile multiple classes, the dependencies of the app class
     */
    FileReloader(Server.Builder builder) {
        this.providedClasspath = builder.classpath;
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (this.compiler == null) {
            throw new RuntimeException(
                    "System java compiler not available. Make sure you're running Jeamlit with a JDK, not a JRE, and that the java compiler is available.");
        }
        checkArgument(builder.appPath != null);
        this.javaFile = builder.appPath;
        this.buildSystem = builder.buildSystem;
    }

    @Override
    Method reload() {
        final String currentClasspath = buildClasspath(providedClasspath, javaFile);
        final @Nonnull List<JavaFileObject> classFiles = compileJavaFile(this.javaFile, currentClasspath);
        final @Nullable JavaFileObject mainClassFile = classFiles.stream()
                // inner classes may appear before the main class in the list
                .filter(e -> !e.getName().contains("$")).findFirst().orElse(null);
        if (mainClassFile == null) {
            throw new CompilationException(
                    "Could not determine class name in file %s. File is empty, invalid or there are only inner classes?".formatted(
                            javaFile));
        }

        // reload dep classloader only if dependencies changed
        final boolean dependencyClasspathChanged = !Objects.equals(lastDependencyClasspath, currentClasspath);
        if (dependenciesClassloader == null || dependencyClasspathChanged) {
            // Need to reload persistent parent
            if (dependenciesClassloader != null) {
                // Clear cache since dependency classloader changed - an instance of the same class loaded by 2 different classloaders is not "the same class" and would result in ClassCastException
                // it's the responsibility of the app to manage cache init properly when the cache is cleared - this should be fine
                StateManager.getCache().clear();
                closeClassLoader(dependenciesClassloader, "dependencies");
                LOG.info("App dependencies changed. Reloading the dependencies. Jt.cache() was cleared to avoid ClassLoader conflicts.");
            }
            LOG.info("Using classpath {}", currentClasspath);
            final URL[] dependencyUrls = createClassPathUrls(currentClasspath);
            dependenciesClassloader = new URLClassLoader(dependencyUrls, getClass().getClassLoader());
            lastDependencyClasspath = currentClasspath;
        }

        // Always reload app code classloader
        closeClassLoader(appClassloader, "user code");
        Method mainMethod = null;
        try {
            appClassloader = new HotClassLoader(dependenciesClassloader);
            for (final JavaFileObject classFile : classFiles) {
                final String className = classNameFor(classFile);
                final byte[] classBytes = loadClassBytes(classFile);
                final Class<?> appClass = appClassloader.defineClass(className, classBytes);
                if (classFile == mainClassFile) {
                    mainMethod = appClass.getMethod("main", String[].class);
                }
            }
        } catch (NoSuchMethodException e) {
            throw new CompilationException(e);
        }
        return mainMethod;
    }

    private static void closeClassLoader(final URLClassLoader classLoader, final @Nullable String classLoaderName) {
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                LOG.warn("Failed to close previous {} classloader", classLoaderName, e);
            }
        }
    }

    // return the fully qualified classname of the compiled file
    private @Nonnull List<JavaFileObject> compileJavaFile(final @Nonnull Path javaFile, final @Nonnull String classpath) {
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null,
                                                                                   null,
                                                                                   null)) {
            final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(
                    List.of(javaFile));
            final List<@NotNull String> compilationOptions = List.of("-d",
                                                                     COMPILATION_TARGET_DIR.toString(),
                                                                     "-cp",
                                                                     classpath,
                                                                     "-sourcepath",
                                                                     javaFile.toAbsolutePath().getParent().toString(),
                                                                     "-proc:none");
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

    @SuppressWarnings("StringSplitter") // see https://errorprone.info/bugpattern/StringSplitter - checking for blank string should be enough here
    private static URL[] createClassPathUrls(final @Nonnull String classpath) {
        if (classpath.isBlank()) {
            return new URL[0];
        }
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

        private HotClassLoader(final ClassLoader parent) {
            super(new URL[]{}, parent);
        }

        private Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    /**
     * obtain and combine classpaths.
     */
    private String buildClasspath(final @Nullable String providedClasspath, final @Nonnull Path javaFilePath) {
        final StringBuilder cp = new StringBuilder();

        // add provided classpath
        if (providedClasspath != null && !providedClasspath.isEmpty()) {
            LOG.info("User-provided classpath added to the classpath successfully.");
            LOG.debug("Added from user-provided input: {}", providedClasspath);
            cp.append(providedClasspath);
        }

        try {
            LOG.info("Trying to add {} dependencies to the classpath...", buildSystem);
            final String classpath = buildSystem.obtainClasspath(javaFilePath);
            if (!classpath.isBlank()) {
                if (!cp.isEmpty()) {
                    cp.append(File.pathSeparator);
                }
                cp.append(classpath);
            }
            LOG.info("{} dependencies added to the classpath successfully.", buildSystem);
            LOG.debug("Added from {}: {}", buildSystem, classpath);
        } catch (Exception e) {
            if (e instanceof CompilationException ce) {
                throw ce;
            }
            throw new CompilationException("Failed to resolve %s dependencies: %s".formatted(buildSystem, e.getMessage()), e);
        }
        return cp.toString();
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


}
