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
package io.javelit.cli;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import io.javelit.core.Server;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static com.google.common.base.Preconditions.checkArgument;
import static io.javelit.core.utils.LangUtils.optional;


@Command(name = "javelit",
    mixinStandardHelpOptions = true,
    versionProvider = Cli.JavelitVersionProvider.class,
    description = "Javelit, a Streamlit-like framework for Java")
public class Cli implements Callable<Integer> {

  private static final Logger LOG = LoggerFactory.getLogger(Cli.class);

  @Command(name = "run", description = "Run a Javelit application")
  static class RunCommand implements Callable<Integer> {

    @SuppressWarnings("unused") @Parameters(index = "0", description = """
        The Javelit app Java file to run. Supported:
        (0) A local file path
        (1) A direct link to a text/plain .java file
          e.g. https://raw.githubusercontent.com/user/repo/branch/file.java
        (2) A GitHub repository URL
          e.g. https://github.com/user/repo
        (3) A GitHub folder tree URL
          e.g. github.com/user/repo/tree/branch/path/to/folder
        (4) A GitHub file blob URL 
          e.g. github.com/javelit/javelit/blob/main/examples/Callbacks.java
        """) private String appPath;

    @SuppressWarnings("unused") @Option(names = {"-p", "--port"},
        description = "Port to run server on",
        defaultValue = "8080") private int port;

    @SuppressWarnings("unused") @Option(names = {"--no-browser"},
        description = "Don't open browser automatically") private boolean noBrowser;

    @SuppressWarnings("unused") @Option(names = {"--watch-remote"},
        description = "Applies when running a remote file/folder. The remote file/folder is refreshed every N seconds. Default is -1 (no refresh). The minimum positive value is 60 seconds to prevent overloading webservers.",
        defaultValue = "-1") @ApiStatus.Experimental private int watchRemoteSeconds;

    @SuppressWarnings("unused") @Option(names = {"--classpath", "-cp"},
        description = "Additional classpath entries") private String classpath;

    @SuppressWarnings("unused") @Option(names = {"--headers-file"},
        description = "File containing additional HTML headers") private String headersFile;

    @SuppressWarnings("unused") @Option(names = {"--log-level"},
        description = "Set log level (TRACE, DEBUG, INFO, WARN, ERROR). Default: INFO. If the value is not recognized, fallbacks to DEBUG.",
        defaultValue = "INFO") private String logLevel;

    @SuppressWarnings("unused")
    @Option(names = {"--base-path"},
        description = """
            URL path prefix where the Javelit app is served. By default, Javelit expects to be served at the root "/".
            For instance, if Javelit is served behind a proxy at example.com/behind/proxy, use "--base-path=/behind/proxy".
            This setting is not necessary if the proxy sets the X-Forwarded-Prefix header.
            When using --base-path, if you need to access the app on localhost directly (not behind the proxy), pass the ?ignoreBasePath=true query parameter. Eg: localhost:8080/?ignoreBasePath=true
            In dev mode, the browser automatically opens with ?ignoreBasePath=true.
            """)
    private String basePath;

    @Override
    public Integer call() throws Exception {
      final Level logLevel = Level.valueOf(this.logLevel);
      setLoggingLevel(logLevel);

      if (!parametersAreValid()) {
        return 1;
      }

      // Resolve appPath - could be local file or URL

      final boolean isUrl = isUrl(appPath);
      final Path javaFilePath;
      if (isUrl) {
        try {
          javaFilePath = resolveRemoteUrl(appPath);

        } catch (IOException e) {
          LOG.error("Failed to resolve app path: {}", appPath, e);
          return 1;
        }
      } else {
        javaFilePath = Paths.get(appPath);
      }

      LOG.info("Starting Javelit on file {}", javaFilePath.toAbsolutePath());
      if (!Files.exists(javaFilePath)) {
        LOG.error("File not found: {}", javaFilePath.toAbsolutePath());
        return 1;
      }
      // Create server
      final Server.Builder builder = Server
          .builder(javaFilePath, port)
          .additionalClasspath(classpath)
          .headersFile(headersFile);
      if (isUrl) {
        builder.originalUrl(appPath);
      }
      if (basePath != null) {
        builder.basePath(basePath);
      }

      final Server server = builder.build();

      // Start everything
      final String url = "http://localhost:" + port + (basePath != null ? "?ignoreBasePath=true" : "");
      try {
        server.start();
        LOG.info("Press Ctrl+C to stop");
      } catch (Exception e) {
        LOG.error("Error starting server", e);
        return 1;
      }
      if (!noBrowser) {
        openBrowser(url);
      }

      // wait for interruption
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        LOG.info("Shutting down...");
        server.stop();
      }));
      Thread.currentThread().join();

      return 0;
    }

    private boolean parametersAreValid() {
      boolean parametersAreValid = true;

      // Only validate .java extension for local files (not URLs)
      final boolean isUrl = isUrl(appPath);
      if (!isUrl && !appPath.endsWith(".java")) {
        // note: I know a Java file could in theory not end with .java but I want to reduce other issues downstream
        LOG.error("File {} does not look like a java file. File should end with .java", appPath);
        parametersAreValid = false;
      }
      // perform other parameter checks here

      return parametersAreValid;
    }

    private static boolean isUrl(final @Nonnull String localPathOrUrl) {
      return localPathOrUrl.startsWith("http://") || localPathOrUrl.startsWith("https://");
    }

    private Path resolveRemoteUrl(@NotNull String pathOrUrl) throws IOException {
      LOG.info("Detected remote file URL: {}", pathOrUrl);
      final Path tempDir = Files.createTempDirectory("javelit-remote-");
      final Path path = RemoteFileUtils.downloadRemoteFile(pathOrUrl, tempDir);
      if (watchRemoteSeconds > 0) {
        checkArgument(watchRemoteSeconds >= 60,
                      "Invalid watch remote interval of %s seconds. Interval should be at least 60 seconds.".formatted(
                          watchRemoteSeconds));
        // Schedule periodic refresh of remote file
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
          try {
            LOG.info("Refreshing remote file from {}", pathOrUrl);
            RemoteFileUtils.downloadRemoteFile(pathOrUrl, tempDir);
            LOG.info("Successfully refreshed remote file");
          } catch (Exception e) {
            LOG.error("Failed to refresh remote file", e);
          }
        }, watchRemoteSeconds, watchRemoteSeconds, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdownNow));
        LOG.info("Successfully scheduled remote file/folder refresh every {} seconds.", watchRemoteSeconds);
      }

      return path;
    }
  }

  @Command(name = "hello", description = "Create and run a Hello World app.")
  static class HelloWorldCommand implements Callable<Integer> {
    @SuppressWarnings("unused") @Option(names = {"-p", "--port"},
        description = "Port to run server on",
        defaultValue = "8080") private int port;

    @Override
    public Integer call() throws Exception {
      final Level logLevel = Level.valueOf("INFO");
      setLoggingLevel(logLevel);

      final Path helloWorldFile = Paths.get("HelloWorld.java");
      if (Files.exists(helloWorldFile)) {
        LOG.info("HelloWorld.java already exists in the current directory");
        LOG.info("You can run it with: javelit run --port {} HelloWorld.java", port);
        return 1;
      }
      try (InputStream resourceStream = getClass().getResourceAsStream("/cli/HelloWorld.java")) {
        if (resourceStream == null) {
          LOG.error("Could not find HelloWorld.java template in resources");
          return 1;
        }
        Files.copy(resourceStream, helloWorldFile);
        LOG.info("Created {} in the current directory", helloWorldFile.getFileName().toString());
      } catch (IOException e) {
        LOG.error("Failed to create HelloWorld.java", e);
        return 1;
      }

      // now just launch the RunCommand targeting the newly created HelloWorld.java
      final RunCommand runCommand = new RunCommand();
      runCommand.appPath = helloWorldFile.toString();
      runCommand.port = port;
      runCommand.noBrowser = false;

      return runCommand.call();
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
        .setUsageHelpWidth(90)
        .execute(args);
    System.exit(exitCode);
  }

  public static void setLoggingLevel(Level level) {
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    root.setLevel(level);
  }

  public static class JavelitVersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
      return new String[]{optional(Cli.class.getPackage())
                              .map(Package::getImplementationTitle)
                              .orElse("unknown project name") + " " + optional(Cli.class.getPackage())
                              .map(Package::getImplementationVersion)
                              .orElse("unknown version"),};
    }
  }

  private static void openBrowser(final String url) {
    try {
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(new URI(url));
      } else {
        LOG.warn("Desktop not supported, cannot open browser automatically");
      }
    } catch (Exception e) {
      LOG.error("Could not open browser. Please open browser manually. ", e);
    }
  }
}
