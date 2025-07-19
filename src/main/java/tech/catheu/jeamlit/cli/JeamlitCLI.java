package tech.catheu.jeamlit.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tech.catheu.jeamlit.core.JeamlitAgent;
import tech.catheu.jeamlit.core.JeamlitServer;
import tech.catheu.jeamlit.watcher.FileWatcher;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;


@Command(name = "jeamlit", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Streamlit-like framework for Java")
public class JeamlitCLI implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(JeamlitCLI.class);

    @Command(name = "run", description = "Run a Jeamlit application")
    static class RunCommand implements Callable<Integer> {

        @SuppressWarnings("unused")
        @Parameters(index = "0", description = "The Jeamlit app Java file to run")
        private String appPath;

        @SuppressWarnings("unused")
        @Option(names = {"-p", "--port"}, description = "Port to run server on", defaultValue = "8080")
        private int port;

        @SuppressWarnings("unused")
        @Option(names = {"--no-browser"}, description = "Don't open browser automatically")
        private boolean noBrowser;

        @SuppressWarnings("unused")
        @Option(names = {"--classpath", "-cp"}, description = "Additional classpath entries")
        private String classpath;

        @SuppressWarnings("unused")
        @Option(names = {"--headers-file"}, description = "File containing additional HTML headers", defaultValue = "jt_headers.html")
        private String headersFile;

        @Override
        public Integer call() throws Exception {
            if (!parametersAreValid()) {
                return 1;
            }

            final Path javaFilePath = Paths.get(appPath);
            logger.info("Starting Jeamlit on file {}", javaFilePath.toAbsolutePath());
            if (!Files.exists(javaFilePath)) {
                logger.error("File not found: {}", javaFilePath.toAbsolutePath());
                return 1;
            }

            // Set up hot reloader
            final String fullClasspath = ClasspathUtils.buildClasspath(classpath, javaFilePath);
            logger.info("Using classpath {}", fullClasspath);
            final JeamlitAgent.HotReloader hotReloader = new JeamlitAgent.HotReloader(fullClasspath, javaFilePath);

            // CYRIL - NOTE - we could start a first compilation here to gain time but it's an optimization
            // could be done in another thread, the server may take some time to spin up

            // Create server
            final JeamlitServer server = new JeamlitServer(port, headersFile, hotReloader);

            // Set up file watcher
            final FileWatcher fileWatcher = new FileWatcher(appPath, server);

            // Start everything
            final String url = "http://localhost:" + port;
            try {
                server.start();
                fileWatcher.start();
                logger.info("Server started at {} ", url);
                logger.info("Press Ctrl+C to stop");
            } catch (Exception e) {
                logger.error("Error starting server", e);
                return 1;
            }
            if (!noBrowser) {
                openBrowser(url);
            }

            // wait for interruption
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                fileWatcher.stop();
                server.stop();
            }));
            Thread.currentThread().join();

            return 0;
        }

        private boolean parametersAreValid() {
            boolean parametersAreValid = true;
            if (!appPath.endsWith(".java")) {
                // note: I know a Java file could in theory not end with .java but I want to reduce other issues downstream
                logger.error("File {} does not look like a java file. File should end with .java",
                             appPath);
                parametersAreValid = false;
            }
            // perform other parameter checks here

            return parametersAreValid;
        }

        private void openBrowser(final String url) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    logger.warn("Desktop not supported, cannot open browser automatically");
                }
            } catch (Exception e) {
                logger.error("Could not open browser. Please open browser manually: " + e.getMessage());
            }
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