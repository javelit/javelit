package tech.catheu.jeamlit.cli;

import dev.jbang.dependencies.DependencyResolver;
import dev.jbang.dependencies.ModularClassPath;
import dev.jbang.source.Project;
import dev.jbang.source.ResourceRef;
import dev.jbang.source.Source;
import dev.jbang.source.sources.JavaSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tech.catheu.jeamlit.agent.JeamlitAgent;
import tech.catheu.jeamlit.server.JeamlitServer;
import tech.catheu.jeamlit.watcher.FileWatcher;

import java.awt.Desktop;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "jeamlit", mixinStandardHelpOptions = true, version = "1.0.0",
         description = "Streamlit-like framework for Java")
public class JeamlitCLI implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(JeamlitCLI.class);

    private static final Method DEPENDENCY_COLLECT_REFLECTION;

    static {
        try {
            DEPENDENCY_COLLECT_REFLECTION = Source.class.getDeclaredMethod("collectBinaryDependencies");
            DEPENDENCY_COLLECT_REFLECTION.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Command(name = "run", description = "Run a Jeamlit application")
    static class RunCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "Java file to run")
        private String javaFile;
        
        @Option(names = {"-p", "--port"}, description = "Port to run server on", defaultValue = "8080")
        private int port;
        
        @Option(names = {"--no-browser"}, description = "Don't open browser automatically")
        private boolean noBrowser;
        
        @Option(names = {"--classpath", "-cp"}, description = "Additional classpath entries")
        private String classpath;
        
        @Override
        public Integer call() throws Exception {
            final Path javaFilePath = Paths.get(javaFile);
            if (!Files.exists(javaFilePath)) {
                logger.error("File not found: {}", javaFile);
                return 1;
            }
            if (!javaFile.endsWith(".java")) {
                logger.error("File {} does not look like a java file. File should end with .java", javaFile);
                return 1;
            }
            
            logger.info("Starting Jeamlit with file {}", javaFilePath.toAbsolutePath());
            
            // Set up hot reloader
            final String fullClasspath = buildClasspath(classpath, javaFilePath);
            logger.info("Using classpath {}", fullClasspath);
            JeamlitAgent.HotReloader hotReloader = new JeamlitAgent.HotReloader(fullClasspath);
            
            // Initial compilation
            logger.info("Compiling " + javaFile + "...");
            if (!hotReloader.reloadFile(javaFilePath)) {
                System.err.println("Failed to compile " + javaFile);
                return 1;
            }
            logger.info("Compilation successful");
            
            // Create server
            JeamlitServer server = new JeamlitServer(port);
            
            // Create app runner that uses reflection to invoke main method
            server.setAppRunner(sessionId -> {
                try {
                    // Get the main class name from file path
                    String className = getClassName(javaFilePath);
                    System.out.println("Trying to load class: " + className);
                    
                    // Create a custom classloader that can see the classpath
                    // FIXME CYRIL - ENSURE DEPENDENCIES AR RELOADED HERE
                    java.net.URLClassLoader appClassLoader = createClassLoader(fullClasspath);
                    
                    // Load and run the class
                    Class<?> appClass = appClassLoader.loadClass(className);
                    System.out.println("Successfully loaded class: " + appClass.getName());
                    
                    java.lang.reflect.Method mainMethod = appClass.getMethod("main", String[].class);
                    mainMethod.invoke(null, new Object[]{new String[]{}});
                    
                } catch (Exception e) {
                    logger.error("Error running app", e);
                    System.err.println("Full error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Error running app: " + e.getMessage(), e);
                }
            });
            
            // Set up file watcher
            FileWatcher fileWatcher = new FileWatcher(javaFile, changedFile -> {
                logger.info("File changed: " + changedFile);
                logger.info("Reloading...");
                if (hotReloader.reloadFile(changedFile)) {
                    logger.info("Reloading successful");
                    server.notifyReload();
                } else {
                    // TODO CYRIL - if failure notify the server with an error message
                    logger.error("Reloading failed");
                }
            });
            
            // Start everything
            try {
                server.start();
                fileWatcher.start();
                
                String url = "http://localhost:" + port;
                logger.info("Server started at: " + url);
                
                // Open browser if requested
                if (!noBrowser) {
                    openBrowser(url);
                }

                logger.info("Press Ctrl+C to stop");
                
                // Add shutdown hook
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Shutting down...");
                    fileWatcher.stop();
                    server.stop();
                }));
                
                // Keep running
                Thread.currentThread().join();
                
            } catch (Exception e) {
                logger.error("Error starting server", e);
                return 1;
            }
            
            return 0;
        }

        // FIXME CYRIL CLEAN THIS
        private String buildClasspath(String additionalClasspath, Path javaFilePath) {
            final StringBuilder cp = new StringBuilder();
            // Add current directory (for standalone Java files)
            cp.append(".");
            // Add target/classes directory (for compiled Maven classes)
            cp.append(":target/classes");
            // TODO: Add proper Maven dependency resolution
            // add cli classpath
            if (additionalClasspath != null && !additionalClasspath.isEmpty()) {
                cp.append(":").append(additionalClasspath);
            }
            // add jbang style deps
            final Source source = JavaSource.forResourceRef(ResourceRef.forFile(javaFilePath), null);
            final var test = Project.builder().build(javaFilePath);
            final Source mainSource = test.getMainSource();
            final List<String> dependencies = getDependenciesFrom(mainSource);
            if (!dependencies.isEmpty()) {
                final DependencyResolver resolver = new DependencyResolver();
                resolver.addDependencies(dependencies);
                final ModularClassPath modularClasspath = resolver.resolve();
                cp.append(":").append(modularClasspath.getClassPath());
            }

            return cp.toString();
        }
        
        private String getClassName(Path javaFile) {
            String fileName = javaFile.toString();
            
            // Find the source root (src/main/java)
            int srcIndex = fileName.indexOf("src/main/java/");
            if (srcIndex != -1) {
                // Extract the part after src/main/java/
                String relativePath = fileName.substring(srcIndex + "src/main/java/".length());
                // Convert to class name
                return relativePath.replace('/', '.').replace(".java", "");
            }
            
            // Handle files in current directory or other locations
            String name = javaFile.getFileName().toString();
            return name.substring(0, name.lastIndexOf('.'));
        }
        
        private java.net.URLClassLoader createClassLoader(String classpath) throws Exception {
            String[] paths = classpath.split(":");
            java.net.URL[] urls = new java.net.URL[paths.length];
            
            for (int i = 0; i < paths.length; i++) {
                java.io.File file = new java.io.File(paths[i]);
                urls[i] = file.toURI().toURL();
            }
            
            return new java.net.URLClassLoader(urls, this.getClass().getClassLoader());
        }
        
        private void openBrowser(String url) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    System.out.println("Desktop not supported, cannot open browser automatically");
                }
            } catch (Exception e) {
                System.out.println("Could not open browser: " + e.getMessage());
            }
        }
    }

    private static List<String> getDependenciesFrom(final Source mainSource) {
        try {
            return (List<String>) DEPENDENCY_COLLECT_REFLECTION.invoke(mainSource);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new JeamlitCLI())
            .addSubcommand("run", new RunCommand())
            .execute(args);
        System.exit(exitCode);
    }
}