package tech.catheu.jeamlit.core;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.exception.AppRunException;
import tech.catheu.jeamlit.exception.CompilationException;
import tech.catheu.jeamlit.spi.JtComponent;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
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

import static tech.catheu.jeamlit.core.FileNameUtils.classFilePathFor;
import static tech.catheu.jeamlit.core.FileNameUtils.classNameFor;

public class JeamlitAgent {

    public static class HotReloader {
        private static final Logger LOG = LoggerFactory.getLogger(HotReloader.class);
        private static final Path COMPILATION_TARGET_DIR = Paths.get("target/jeamlit/classes");

        // used for gc control - see note close to usage
        private static final ConcurrentMap<String, Class<?>> LOADED_CLASSES = new ConcurrentHashMap<>();

        final URL[] classPathUrls;
        private final JavaCompiler compiler;
        private final List<String> compilationOptions;
        private final Path javaFile;
        private AtomicReference<Method> mainMethod = new AtomicReference<>();

        public HotReloader(final String classpath, final Path javaFile) {
            this.compiler = ToolProvider.getSystemJavaCompiler();
            if (this.compiler == null) {
                throw new RuntimeException(
                        "System java compiler not available. Make sure you're running Jeamlit with a JDK, not a JRE, and that the java compiler is available.");
            }
            this.compilationOptions = List.of("-d",
                                              COMPILATION_TARGET_DIR.toString(),
                                              "-cp",
                                              classpath,
                                              "-proc:none");
            this.classPathUrls = JeamlitAgent.createClassPathUrls(classpath);
            this.javaFile = javaFile;
        }

        /**
         * Recompile the app class. Returns the Class.
         *
         * @throws CompilationException for any compilation that should be reported to the user in the app
         *                              Note: not implemented yet: re-compile multiple classes, the dependencies of the app class
         */
        public void reloadFile() {
            compileJavaFile(this.javaFile);
            final String className = classNameFor(this.javaFile);
            final byte[] classBytes = loadClassBytes(className);
            try (final HotClassLoader loader = new HotClassLoader(this.classPathUrls,
                                                                  getClass().getClassLoader())) {
                final Class<?> appClass = loader.defineClass(className, classBytes);
                mainMethod.set(appClass.getMethod("main", String[].class));
            } catch (IOException | NoSuchMethodException e) {
                throw new CompilationException(e.getMessage());
            }
        }

        /**
         * @return the JtComponents after the end of the execution of the app
         * @throws CompilationException: if the main method was not defined as expected
         * @throws AppRunException :      if the main method call raised an exception
         */
        public List<JtComponent<?>> runApp(final String sessionId) {
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
            } catch (IllegalAccessException e) {
                throw new CompilationException(
                        "The main method of the app may not be public. Error: " + e.getMessage());
            } catch (InvocationTargetException e) {
                throw new AppRunException(e);
            } finally {
                result = StateManager.endExecution();
            }
            return result;

        }

        private void compileJavaFile(final Path javaFile) {
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null,
                                                                                       null,
                                                                                       null)) {
                final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
                final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(
                        List.of(javaFile));
                // Compile
                final JavaCompiler.CompilationTask task = compiler.getTask(null,
                                                                           fileManager,
                                                                           diagnosticsCollector,
                                                                           this.compilationOptions,
                                                                           null,
                                                                           compilationUnits);
                boolean success = task.call();
                if (success) {
                    LOG.info("Successfully compiled {}", javaFile);
                } else {
                    final String errorMessage = diagnosticsCollector.getDiagnostics().stream().map(d -> d.getMessage(
                            null)).collect(Collectors.joining("\n\n"));
                    LOG.error("Compilation failed for {}: \n{}", javaFile, errorMessage);
                    throw new CompilationException(errorMessage);
                }
            } catch (IOException e) {
                LOG.error("Error compiling file {}", javaFile, e);
                throw new CompilationException(e.getMessage());
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
                throw new CompilationException(e.getMessage());
            }
        }

    }

    public static URL[] createClassPathUrls(final String classpath) {
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


    private static class HotClassLoader extends URLClassLoader {

        public HotClassLoader(final URL[] urls, final ClassLoader parent) {
            super(urls, parent);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}