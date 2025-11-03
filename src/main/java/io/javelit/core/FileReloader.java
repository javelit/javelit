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
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
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
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.DefaultTokenWriter;
import spoon.support.compiler.FileSystemFile;
import spoon.support.reflect.code.CtBlockImpl;
import spoon.support.reflect.declaration.CtMethodImpl;

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
            // FIXME SHOULD BE A PARAM OF COURSE
            final boolean notebook = true;

            final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
            final List<@NotNull String> compilationOptions = List.of("-d",
                                                                     COMPILATION_TARGET_DIR.toString(),
                                                                     "-cp",
                                                                     classpath,
                                                                     "-sourcepath",
                                                                     javaFile.toAbsolutePath().getParent().toString(),
                                                                     "-proc:none");

            final JavacTask task;
            if (notebook) {
                // Generate modified source in-memory with notebook enhancements
                final String modifiedSource = genMainMethod(javaFile);
                final String className = extractClassName(javaFile);

                // Create in-memory source object
                final JavaFileObject inMemorySource = new StringSourceJavaFileObject(className, modifiedSource);

                // Create hybrid file manager that checks in-memory sources first
                final Map<String, JavaFileObject> inMemorySources = Map.of(className, inMemorySource);
                final HybridJavaFileManager hybridManager = new HybridJavaFileManager(fileManager, inMemorySources);

                // Compile from in-memory source
                task = (JavacTask) compiler.getTask(null,
                                                   hybridManager,
                                                   diagnosticsCollector,
                                                   compilationOptions,
                                                   null,
                                                   List.of(inMemorySource));
            } else {
                // Normal mode: compile from disk
                final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(
                        List.of(javaFile));
                task = (JavacTask) compiler.getTask(null,
                                                   fileManager,
                                                   diagnosticsCollector,
                                                   compilationOptions,
                                                   null,
                                                   compilationUnits);
            }

            // Continue with compilation
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

    private String genMainMethod(final Path javaFile) {
        final Launcher spoon = new Launcher();
        spoon.getEnvironment().setAutoImports(true);
        spoon.addInputResource(new FileSystemFile(new File(javaFile.toUri())));
        spoon.getFactory().getEnvironment().setPrettyPrinterCreator(() -> {
            DefaultJavaPrettyPrinter defaultJavaPrettyPrinter =
                    new DefaultJavaPrettyPrinter(spoon.getFactory().getEnvironment());
            defaultJavaPrettyPrinter.setIgnoreImplicit(false);
            defaultJavaPrettyPrinter.setPrinterTokenWriter(new DefaultTokenWriter() {
                @Override
                public DefaultTokenWriter writeComment(CtComment comment) {
                    // FIXME P1 CYRIL maybe just don't write comments for top level field impl, main because they are written manually as markdown
                    //  is it possible to get the top level? most likely
                    // don't write comments
                    return this;
                }
            });
            return defaultJavaPrettyPrinter;
        });
        final CtModel model = spoon.buildModel();
        final CtClass<?> classAst = (CtClass<?>) model.getAllTypes().iterator().next();
        final List<CtTypeMember> members = classAst.getTypeMembers();
        final StringBuilder newMain = new StringBuilder();
        newMain.append("public static void main(String[] args) { \n");
        CtMethodImpl<?> mainMethod = null;
        for (int i = 1; i < members.size(); i++) {
            final CtTypeMember elem = members.get(i);
            if (elem instanceof CtMethodImpl<?> method) {
                if ("main".equals(method.getSimpleName())) {
                    mainMethod = method;
                }
            }
        }
        if (mainMethod == null) {
            throw new CompilationException("Could not find main method");
        }
        genMainBody(mainMethod, newMain);
        newMain.append("}");
        final String originalClassCode;
        try {
            originalClassCode = Files.readString(javaFile);
        } catch (IOException e) {
            throw new CompilationException(e);
        }
        return "import io.javelit.core.Jt;\n" + originalClassCode.substring(0,
                                           mainMethod.getPosition().getSourceStart())
               + newMain
               + originalClassCode.substring(mainMethod.getPosition().getSourceEnd() + 1);
    }

    private void genMainBody(CtMethodImpl<?> notebookMethod, StringBuilder newMain) {
        final CtBlockImpl<?> block = (CtBlockImpl<?>) notebookMethod.getBody();
        List<CtStatement> statements = block.getStatements();
        final List<String> codeElems = new ArrayList<>();
        final List<String> orphanComments = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            CtStatement s = statements.get(i);
            var sPos = s.getPosition();
            var endLine = sPos.getEndLine();
            newMain.append(commentPrint(s));
            if (s instanceof CtComment comment) {
                orphanComments.add(comment.getContent());
                continue;
            }
            final String statement = s + ";";
            codeElems.add(statement);
            boolean showResult = i + 1 == statements.size() // end of file
                                 || statements.get(i + 1) instanceof CtComment // next is a comment
                                 || statements.get(i + 1)
                                              .getPosition()
                                              .getLine() > endLine + 1; // there is an empty line --> corresponds to a notebook block end
            if (showResult) {
                final String codeSnippet = String.join("\n", codeElems);
                newMain.append("Jt.code(\"\"\"\n").append(codeSnippet).append("\n\"\"\").use();\n");
                newMain.append(codeSnippet).append("\n");
                codeElems.clear();
            }
        }
        if (!orphanComments.isEmpty()) {
            final String commentSnippet = String.join("\n", orphanComments);
            final CommentTemplate template = processCommentTemplate(commentSnippet);
            if (template.variables.isEmpty()) {
                newMain.append("Jt.markdown(\"\"\"\n").append(template.template).append("\n\"\"\").use();\n");
            } else {
                final String varsJoined = String.join(", ", template.variables);
                newMain.append("Jt.markdown(\"\"\"\n").append(template.template).append("\n\"\"\".formatted(").append(varsJoined).append(")).use();\n");
            }
        }
    }

    /**
     * Result of processing a comment template with variable interpolation
     */
    private static class CommentTemplate {
        final String template;  // With %s placeholders
        final List<String> variables;  // Variable names in order

        CommentTemplate(final String template, final List<String> variables) {
            this.template = template;
            this.variables = variables;
        }
    }

    /**
     * Process comment string to extract ${expression} patterns and replace with %s placeholders
     * Supports any Java expression: ${x}, ${x*x}, ${foo.bar()}, etc.
     * @param comment Original comment string with ${...} patterns
     * @return CommentTemplate with %s placeholders and list of expressions
     */
    private static CommentTemplate processCommentTemplate(final String comment) {
        // Match ${...} where ... can be any Java expression (non-greedy to handle multiple ${} in one line)
        final Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        final Matcher matcher = pattern.matcher(comment);
        final List<String> variables = new ArrayList<>();
        final StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            variables.add(matcher.group(1).trim());  // Extract expression and trim whitespace
            matcher.appendReplacement(result, "%s");  // Replace with %s
        }
        matcher.appendTail(result);

        return new CommentTemplate(result.toString(), variables);
    }

    private static String commentPrint(CtStatement elem) {
        if (elem.getComments().isEmpty()) {
            return "";
        }
        final String commentsString = elem.getComments()
                                          .stream()
                                          .map(CtComment::getContent)
                                          .collect(Collectors.joining("\n"));

        final CommentTemplate template = processCommentTemplate(commentsString);

        if (template.variables.isEmpty()) {
            // No interpolation needed
            return "Jt.markdown(\"\"\"\n" + template.template + "\n\"\"\").use();\n";
        } else {
            // Add .formatted(var1, var2, ...)
            final String varsJoined = String.join(", ", template.variables);
            return "Jt.markdown(\"\"\"\n" + template.template + "\n\"\"\".formatted(" + varsJoined + ")).use();\n";
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
     * JavaFileObject that holds source code in memory for in-memory compilation
     */
    static class StringSourceJavaFileObject extends SimpleJavaFileObject {
        private final String sourceCode;

        StringSourceJavaFileObject(final String className, final String sourceCode) {
            super(URI.create("string:///" + className.replace('.', '/') + ".java"), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return sourceCode;
        }
    }

    /**
     * File manager that checks in-memory sources first, then falls back to disk
     */
    static class HybridJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, JavaFileObject> inMemorySources;

        HybridJavaFileManager(final StandardJavaFileManager fileManager,
                             final Map<String, JavaFileObject> inMemorySources) {
            super(fileManager);
            this.inMemorySources = inMemorySources;
        }

        @Override
        public JavaFileObject getJavaFileForInput(final Location location,
                                                 final String className,
                                                 final JavaFileObject.Kind kind) throws IOException {
            // Check in-memory sources first
            if (kind == JavaFileObject.Kind.SOURCE && inMemorySources.containsKey(className)) {
                return inMemorySources.get(className);
            }
            // Fall back to disk
            return super.getJavaFileForInput(location, className, kind);
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
     * Extract class name from Java file path
     * examples/demo/DemoApp.java -> DemoApp
     */
    private static String extractClassName(final Path javaFile) {
        final String fileName = javaFile.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf('.'));
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
