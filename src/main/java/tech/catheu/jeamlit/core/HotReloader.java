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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static tech.catheu.jeamlit.core.utils.JavaUtils.stackTraceString;
import static tech.catheu.jeamlit.core.utils.StringUtils.percentEncode;


class HotReloader {
    private static final Logger LOG = LoggerFactory.getLogger(HotReloader.class);
    private static final Path COMPILATION_TARGET_DIR = Paths.get("target/jeamlit/classes");

    // used for gc control - see note close to usage
    private static final ConcurrentMap<String, Class<?>> LOADED_CLASSES = new ConcurrentHashMap<>();

    @Nullable
    URL[] classPathUrls = null;
    private final JavaCompiler compiler;
    private @Nullable List<String> compilationOptions;
    private final Path javaFile;
    private final AtomicReference<Method> mainMethod = new AtomicReference<>();
    private final String providedClasspath;

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
    }

    /**
     * Recompile the app class. Returns the Class.
     *
     * @throws CompilationException for any compilation that should be reported to the user in the app
     *                              Note: not implemented yet: re-compile multiple classes, the dependencies of the app class
     */
    protected void reloadFile() {
        final @Nullable String className = compileJavaFile(this.javaFile);
        if (className == null) {
            throw new CompilationException(
                    "Could not determine class name in file %s. File is empty or invalid ?".formatted(
                            javaFile));
        }
        final byte[] classBytes = loadClassBytes(className);
        try (final HotClassLoader loader = new HotClassLoader(this.classPathUrls,
                                                              getClass().getClassLoader())) {
            final Class<?> appClass = loader.defineClass(className, classBytes);
            mainMethod.set(appClass.getMethod("main", String[].class));
        } catch (IOException | NoSuchMethodException e) {
            throw new CompilationException(e);
        }
    }

    protected void runApp(final String sessionId) {
        if (mainMethod.get() == null) {
            // if there are edge cases where this could happen, simply call reloadFile instead of throwing
            // for the moment throwing to catch implementation bugs
            throw new RuntimeException(
                    "Implementation Error ? Trying to run app before compiling it. Please reach out to support");
        }
        List<JtComponent<?>> result;
        StateManager.beginExecution(sessionId);
        try {
            mainMethod.get().invoke(null, new Object[]{new String[]{}});
        } catch (Exception e) {
            if (!(e instanceof InvocationTargetException || e instanceof DuplicateWidgetIDException)) {
                LOG.error("Unexpected error type: {}", e.getClass(), e);
            }
            @Language("markdown") final String errorMessage = buildErrorMessage(e);
            // Send error as a component usage - its lifecycle is managed like all other components
            Jt.error(errorMessage).use();
        } finally {
            StateManager.endExecution();
        }
    }


    private static @Language("markdown") @NotNull String buildErrorMessage(Throwable error) {
        if (error instanceof InvocationTargetException) {
            error = error.getCause();
        }
        final String exceptionSimpleName = error.getClass().getSimpleName();
        final String errorMesssage = error.getMessage();
        final String stackTrace = stackTraceString(error);
        final String googleLink = "https://www.google.com/search?q=" + percentEncode(
                exceptionSimpleName + " " + errorMesssage);
        final String chatGptLink = "https://chatgpt.com/?q=" + percentEncode(String.join("\n",
                                                                                         List.of(
                                                                                                 "Help me fix the following issue. I use Java Jeamlit and got:",
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
    private String compileJavaFile(final Path javaFile) {
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null,
                                                                                   null,
                                                                                   null)) {
            final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(
                    List.of(javaFile));
            if (compilationOptions == null) {
                final String classpath = ClasspathUtils.buildClasspath(providedClasspath, javaFile);
                LOG.info("Using classpath {}", classpath);
                this.compilationOptions = List.of("-d",
                                                  COMPILATION_TARGET_DIR.toString(),
                                                  "-cp",
                                                  classpath,
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
            final CompilationUnitTree compilationUnitTree = l.iterator().next();
            final String className = getFullyQualifiedClassName(compilationUnitTree);
            task.analyze();    // semantic analysis, type checking
            task.generate();
            final boolean success = diagnosticsCollector.getDiagnostics().stream()
                    .noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
            if (success) {
                LOG.info("Successfully compiled {}", javaFile);
                return className;
            } else {
                final String errorMessage = diagnosticsCollector.getDiagnostics().stream()
                        .map(d -> String.format("[%s] %s:%d:%d %s",
                                                d.getKind(),
                                                // ERROR / WARNING / NOTE
                                                d.getSource() != null ? d.getSource()
                                                        .getName() : "<unknown source>",
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

    private @Nonnull byte[] loadClassBytes(final @Nonnull String className) {
        try {
            final String classFilePath = classFilePathFor(className);
            final Path classFile = COMPILATION_TARGET_DIR.resolve(classFilePath);
            if (!Files.exists(classFile)) {
                LOG.error("Class file not found: {} for original class name {}",
                          classFile,
                          className);
                throw new RuntimeException("Class file not found: %s for original class name %s".formatted(
                        classFile,
                        className));
            }
            return Files.readAllBytes(classFile);
        } catch (IOException e) {
            LOG.error(
                    "Error loading class bytes for {}. You may want to retry. If the error persists, please reach out to support.",
                    className,
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

    private static String getFullyQualifiedClassName(final CompilationUnitTree unit) {
        final ExpressionTree packageName = unit.getPackageName();
        for (final Tree typeDecl : unit.getTypeDecls()) {
            if (typeDecl instanceof ClassTree classTree) {
                final String className = classTree.getSimpleName().toString();
                return packageName == null ? className : packageName + "." + className;
            }
        }
        return null;
    }

    /**
     * convert a class name to a class file path
     * com.example.MyClass --> com/example/MyClass.class
     * MyClass --> MyClass.class
     * should be compatible with windows
     */
    private static String classFilePathFor(final String className) {
        return className.replace('.', File.separatorChar) + ".class";
    }


    private static final class HotClassLoader extends URLClassLoader {

        private HotClassLoader(final URL[] urls, final ClassLoader parent) {
            super(urls, parent);
        }

        private Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
