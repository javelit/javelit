package tech.catheu.jeamlit.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JeamlitAgent {
    
    public static class HotReloader {
        private static final Logger logger = LoggerFactory.getLogger(HotReloader.class);
        private static final ConcurrentMap<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();
        
        private final String classpath;
        private final Path targetDir;
        
        public HotReloader(String classpath) {
            this.classpath = classpath;
            this.targetDir = Paths.get("target/classes");
        }
        
        public boolean reloadFile(final Path javaFile) {
            try {
                // step 1: compile
                final boolean compilationSuccess = compileJavaFile(javaFile);
                if (!compilationSuccess) {
                    logger.error("Failed to compile {}", javaFile);
                    return false;
                }
                // Step 2: load the compiled class
                final String className = getClassName(javaFile);
                final byte[] classBytes = loadClassBytes(className);
                if (classBytes == null) {
                    logger.error("Failed to load class bytes for {}", className);
                    return false;
                }
                // Step 3: Try to redefine the class
                return redefineClass(className, classBytes);
            } catch (Exception e) {
                logger.error("Error reloading file {}", javaFile, e);
                return false;
            }
        }
        
        private boolean compileJavaFile(Path javaFile) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                logger.error("No Java compiler available");
                return false;
            }
            
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
                
                // Set up compilation options
                List<String> options = Arrays.asList(
                    "-d", targetDir.toString(),
                    "-cp", classpath
                );
                
                // Get the Java file to compile
                Iterable<? extends JavaFileObject> compilationUnits = 
                    fileManager.getJavaFileObjectsFromFiles(List.of(javaFile.toFile()));
                
                // Compile
                JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, null, options, null, compilationUnits);
                
                boolean success = task.call();
                
                if (success) {
                    logger.info("Successfully compiled {}", javaFile);
                } else {
                    logger.error("Compilation failed for {}", javaFile);
                }
                
                return success;
                
            } catch (IOException e) {
                logger.error("Error compiling file {}", javaFile, e);
                return false;
            }
        }
        
        private String getClassName(Path javaFile) {
            // Convert file path to class name
            // e.g., src/main/java/com/example/MyClass.java -> com.example.MyClass
            
            String fileName = javaFile.toString();
            
            // Find the source root (src/main/java)
            int srcIndex = fileName.indexOf("src/main/java/");
            if (srcIndex == -1) {
                // Fallback: assume it's just the filename without package
                String name = javaFile.getFileName().toString();
                return name.substring(0, name.lastIndexOf('.'));
            }
            
            // Extract the part after src/main/java/
            String relativePath = fileName.substring(srcIndex + "src/main/java/".length());
            
            // Convert to class name
            return relativePath.replace('/', '.').replace(".java", "");
        }
        
        private byte[] loadClassBytes(String className) {
            try {
                // Convert class name to file path
                String classFilePath = className.replace('.', '/') + ".class";
                Path classFile = targetDir.resolve(classFilePath);
                
                if (!Files.exists(classFile)) {
                    logger.error("Class file not found: {}", classFile);
                    return null;
                }
                
                return Files.readAllBytes(classFile);
                
            } catch (IOException e) {
                logger.error("Error loading class bytes for {}", className, e);
                return null;
            }
        }
        
        private boolean redefineClass(String className, byte[] classBytes) {
            try {
                // Try to find the existing class
                Class<?> existingClass = loadedClasses.get(className);
                
                if (existingClass == null) {
                    // Class hasn't been loaded yet, try to load it
                    try {
                        existingClass = Class.forName(className);
                        loadedClasses.put(className, existingClass);
                    } catch (ClassNotFoundException e) {
                        logger.info("Class {} not yet loaded, will be loaded on next execution", className);
                        return true; // Not an error, just means fresh load
                    }
                }

                logger.warn("No instrumentation available, falling back to class loader approach");
                return reloadWithClassLoader(className, classBytes);
            } catch (Exception e) {
                logger.error("Error redefining class {}", className, e);
                return false;
            }
        }
        
        private boolean reloadWithClassLoader(String className, byte[] classBytes) {
            // Fallback approach: create a new class loader
            logger.info("Using class loader approach for {}", className);
            
            try {
                // Create a custom class loader that can define classes from bytes
                CustomClassLoader loader = new CustomClassLoader(getClass().getClassLoader());
                Class<?> newClass = loader.defineClass(className, classBytes);
                loadedClasses.put(className, newClass);
                
                logger.info("Successfully loaded class with custom loader: {}", className);
                return true;
                
            } catch (Exception e) {
                logger.error("Error loading class with custom loader: {}", className, e);
                return false;
            }
        }
    }


    private static class CustomClassLoader extends URLClassLoader {
        public CustomClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}