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
package io.jeamlit.cli;

import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import ch.qos.logback.classic.Level;
import io.jeamlit.core.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static io.jeamlit.core.utils.LangUtils.optional;


@Command(name = "jeamlit", mixinStandardHelpOptions = true,
        versionProvider = Cli.JeamlitVersionProvider.class,
        description = "Jeamlit, a Streamlit-like framework for Java")
public class Cli implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(Cli.class);

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
        @Option(names = {"--headers-file"}, description = "File containing additional HTML headers")
        private String headersFile;

        @SuppressWarnings("unused")
        @Option(names = {"-cca", "--custom-compile-args"},
                description = "Custom arguments for compile command. To avoid breaking spaces, separated by :: (e.g., clean::compile::-DargLine=\"-Xmx1024m -XX:MaxPermSize=256m\"). Alternatively, specify multiple times (e.g., -cca clean -cca compile -cca \"-DargLine=-Xmx1024m -XX:MaxPermSize=256m\"). Overrides default Maven/Gradle compile arguments.",
                split = "::")
        private String[] customCompileCmdArgs;

        @SuppressWarnings("unused")
        @Option(names = {"-ccpa", "--custom-classpath-args"},
                description = "Custom arguments for classpath command, separated by :: (e.g., dependency:build-classpath::-DincludeScope=test). Alternatively, specify multiple times (e.g., -ccpa dependency:build-classpath -ccpa -DincludeScope=test). Overrides default Maven/Gradle classpath arguments.",
                split = "::")
        private String[] customClasspathCmdArgs;

        @SuppressWarnings("unused")
        @Option(names = {"--log-level"}, 
                description = "Set log level (TRACE, DEBUG, INFO, WARN, ERROR). Default: INFO. If the value is not recognized, fallbacks to DEBUG.",
                defaultValue = "INFO")
        private String logLevel;

        @Override
        public Integer call() throws Exception {
            final Level logLevel = Level.valueOf(this.logLevel);
            setLoggingLevel(logLevel);

            if (!parametersAreValid()) {
                return 1;
            }

            final Path javaFilePath = Paths.get(appPath);
            logger.info("Starting Jeamlit on file {}", javaFilePath.toAbsolutePath());
            if (!Files.exists(javaFilePath)) {
                logger.error("File not found: {}", javaFilePath.toAbsolutePath());
                return 1;
            }
            // Create server
            final Server server = Server.builder(javaFilePath, port).additionalClasspath(classpath)
                    .headersFile(headersFile)
                    .customCompileCmdArgs(customCompileCmdArgs)
                    .customClasspathCmdArgs(customClasspathCmdArgs)
                    .build();

            // Start everything
            final String url = "http://localhost:" + port;
            try {
                server.start();
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
    }

    @Command(name = "hello", description = "Run the Hello World app.")
    static class HelloWorldCommand implements Callable<Integer> {
        @SuppressWarnings("unused")
        @Option(names = {"-p", "--port"}, description = "Port to run server on", defaultValue = "8080")
        private int port;

        @Override
        public Integer call() throws Exception {
            final Level logLevel = Level.valueOf("INFO");
            setLoggingLevel(logLevel);
            logger.info("Starting the Hello World application");
            final Server server = Server.builder(HelloWorld.class, port).build();
            // Start everything
            final String url = "http://localhost:" + port;
            try {
                server.start();
                logger.info("Press Ctrl+C to stop");
            } catch (Exception e) {
                logger.error("Error starting server", e);
                return 1;
            }
            openBrowser(url);

            // wait for interruption
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                server.stop();
            }));
            Thread.currentThread().join();

            return 0;
        }
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        final int exitCode = new CommandLine(new Cli())
                .addSubcommand("run", new RunCommand())
                .addSubcommand("hello", new HelloWorldCommand())
                .execute(args);
        System.exit(exitCode);
    }

    public static void setLoggingLevel(Level level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);
    }

    public static class JeamlitVersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{
                    optional(Cli.class.getPackage())
                            .map(Package::getImplementationTitle)
                            .orElse("unknown project name") + " "
                    + optional(Cli.class.getPackage())
                    .map(Package::getImplementationVersion)
                    .orElse("unknown version"),};
        }
    }

    private static void openBrowser(final String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                logger.warn("Desktop not supported, cannot open browser automatically");
            }
        } catch (Exception e) {
            logger.error("Could not open browser. Please open browser manually. ", e);
        }
    }
}
