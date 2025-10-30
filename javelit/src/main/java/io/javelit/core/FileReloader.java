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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.google.common.collect.Lists;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

// requires a JDK
class FileReloader extends Reloader {
    private static final Logger LOG = LoggerFactory.getLogger(FileReloader.class);
    protected static final Path COMPILATION_TARGET_DIR = Paths.get("target/javelit/classes")
            .toAbsolutePath();

    private final JavaCompiler compiler;
    private final @Nonnull Path javaFile;
    private final String providedClasspath;
    final BuildSystem buildSystem;
    private static @Nullable String lastDependencyClasspath;
    // this classloader contains dependencies - it will only be reloaded if dependencies change
    private static @Nullable URLClassLoader dependenciesClassloader;
    private @Nonnull List<HierarchicalClassLoader> appClassloaders = new ArrayList<>();

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
                    "System java compiler not available. Make sure you're running Javelit with a JDK, not a JRE, and that the java compiler is available.");
        }
        checkArgument(builder.appPath != null);
        this.javaFile = builder.appPath;
        this.buildSystem = builder.buildSystem;
    }

    @Override
    AppEntrypoint reload() {
        final String currentClasspath = buildClasspath(providedClasspath, javaFile);
        final @Nonnull List<JavaFileObject> classFiles = compileJavaFile(this.javaFile, currentClasspath);
        final @Nullable JavaFileObject mainClassFile = classFiles.stream()
                                                                 // inner classes may appear before the main class in the list
                                                                 .filter(e -> !e.getName().contains("$"))
                                                                 .findFirst()
                                                                 .orElse(null);
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
                LOG.info(
                        "App dependencies changed. Reloading the dependencies. Jt.cache() was cleared to avoid ClassLoader conflicts.");
            }
            LOG.info("Using classpath {}", currentClasspath);
            final URL[] dependencyUrls = createClassPathUrls(currentClasspath);
            dependenciesClassloader = new URLClassLoader(dependencyUrls, getClass().getClassLoader());
            clearAppClassloaders(0);
            lastDependencyClasspath = currentClasspath;
        }

        try {
            final List<List<JavaFileObject>> groupedClassFiles = getGroupedClassFiles(classFiles);
            boolean cacheMiss = groupedClassFiles.size() != appClassloaders.size();
            HierarchicalClassLoader hierarchicalClassLoader = null;
            for (int i = 0; i < groupedClassFiles.size(); i++) {
                final List<JavaFileObject> classFilesGroup = groupedClassFiles.get(i);
                final List<String> classNames = classFilesGroup.stream().map(FileReloader::classNameFor).toList();
                final List<byte[]> classesBytes = classFilesGroup.stream().map(FileReloader::loadClassBytes).toList();
                if (!cacheMiss) {
                    final HierarchicalClassLoader lastClassloader = appClassloaders.get(i);
                    if (lastClassloader.sourceEquals(classNames, classesBytes)) {
                        hierarchicalClassLoader = lastClassloader;
                        continue;
                    } else {
                        cacheMiss = true;
                        clearAppClassloaders(i);
                    }
                }
                hierarchicalClassLoader = new HierarchicalClassLoader(hierarchicalClassLoader != null ?
                                                                              hierarchicalClassLoader :
                                                                              dependenciesClassloader,
                                                                      classNames,
                                                                      classesBytes);
                appClassloaders.add(hierarchicalClassLoader);
            }
            final String name = classNameFor(mainClassFile);
            final Class<?> mainClass = hierarchicalClassLoader.loadClass(name);
            Method main = mainClass.getMethod("main", String[].class);
            return AppEntrypoint.of(main, hierarchicalClassLoader);
        } catch (NoSuchMethodException e) {
            throw new CompilationException(e);
        } catch (ClassNotFoundException e) {
            LOG.error("Implementation error. Please reach out to support.", e);
            throw new CompilationException(e);
        } catch (Exception e) {
            // classloader ops could fail for many reason - generic catching for the moment
            LOG.error("Unexpected error. If this happens too much, please reach out to support with the error details", e);
            throw new CompilationException(e);
        }
    }

    private static @NotNull List<List<JavaFileObject>> getGroupedClassFiles(@NotNull List<JavaFileObject> classFiles) {
        // group classFiles by parent outer class: inner classes must be defined in the same classloader as the parent (cl means classloader below)
        final LinkedHashMap<String, List<JavaFileObject>> clKeyToClassFiles = new LinkedHashMap<>();
        // run in inverse order --> will have entrypoint last, it's the file most likely to be changed
        for (final JavaFileObject javaFileObject : classFiles.reversed()) {
            final String clKey = clKey(javaFileObject);
            if (!clKeyToClassFiles.containsKey(clKey)) {
                clKeyToClassFiles.put(clKey, new ArrayList<>());
            }
            clKeyToClassFiles.get(clKey).add(javaFileObject);
        }
        // each group will have its own classloader
        return clKeyToClassFiles.values().stream().toList();
    }

    private void clearAppClassloaders(final int idx) {
        // close outdated classloaders from index then remove them
        final List<HierarchicalClassLoader> toRemove = appClassloaders.subList(idx, appClassloaders.size());
        toRemove.forEach(e -> closeClassLoader(e, "appClassloader"));
        toRemove.clear();
    }

    // NOTE: $ in classnames is supported but may result in suboptimal groups
    // eg: MyLib$Utils.java, MyLib$Helper.java --> both will end up in the same group MyLib
    private static String clKey(final JavaFileObject classFile) {
        // App, App$Message, App$1Class, etc... should have the same key
        final String name = classFile.getName().replace(".class", "");
        final int fileNameStart = name.lastIndexOf(File.separatorChar);
        final int dollarIndex = name.indexOf("$", Math.max(fileNameStart, 0));
        if (dollarIndex == -1) {
            return name;
        } else {
            return name.substring(0, dollarIndex);
        }
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
                return Lists.newArrayList(generated);
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

    private static @Nonnull byte[] loadClassBytes(final @Nonnull JavaFileObject classFile) {
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

    // visible for classloader ClassCast exception detection - see AppRunner
    static final class HierarchicalClassLoader extends URLClassLoader {

        private final List<String> definedNames = new ArrayList<>();
        private final List<byte[]> definedBytes = new ArrayList<>();

        private HierarchicalClassLoader(final ClassLoader parent,
                                        final List<String> classesNames,
                                        final List<byte[]> classesBytes) {
            super(new URL[]{}, parent);
            for (int i = 0; i < classesNames.size(); i++) {
                String className = classesNames.get(i);
                byte[] classBytes = classesBytes.get(i);
                defineClass(className, classBytes, 0, classBytes.length);
                definedNames.add(className);
                definedBytes.add(classBytes);
            }
        }

        public boolean sourceEquals(final List<String> otherNames, final List<byte[]> otherBytes) {
            if (otherNames.size() != definedNames.size()) {
                return false;
            }
            for (int i = 0; i < otherNames.size(); i++) {
                if (!definedNames.get(i).equals(otherNames.get(i))) {
                    return false;
                }
                // this comparison could be improved - it's taking the full class bytes, so it contains debugging bytes
                // because line numbers are part of the debugging, line changes can result in a cache miss
                // for instance, a comment on a new line will result in a cache miss
                // do not deactivate debugging info with "-g:none", it will break the debug support of run file in the IDEs
                // ignoring debugging bytes from the class bytes is a bit involved, will be done later (or never)
                if (!Arrays.equals(definedBytes.get(i), otherBytes.get(i))) {
                    return false;
                }
            }
            return true;
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
            throw new CompilationException("Failed to resolve %s dependencies: %s".formatted(buildSystem,
                                                                                             e.getMessage()), e);
        }
        return cp.toString();
    }

    /**
     * convert a JavaFileObject to a className
     * target/javelit/classes/com/example/MyClass.class -> com.example.MyClass
     * target/javelit/classes/MyClass.class --> MyClass
     * compatible with inner classes
     */
    private static String classNameFor(final @Nonnull JavaFileObject classFile) {
        return COMPILATION_TARGET_DIR.toUri().relativize(classFile.toUri()).toString()
                                     .replace(File.separatorChar, '.').replace(".class", "");
    }
}
