package tech.catheu.jeamlit.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

public enum BuildSystem {
    GRADLE("build.gradle",
           IS_OS_WINDOWS ? "gradlew.bat" : "gradlew",
           "gradle",
           new String[]{"-q", "dependencies", "--configuration runtimeClasspath"},
           new String[]{"classes"}),
    MAVEN("pom.xml",
          IS_OS_WINDOWS ? "mvnw.cmd" : "mvnw",
          "mvn",
          IS_OS_WINDOWS ?
                  new String[]{"-q", "exec:exec", "-Dexec^.executable=cmd", "-Dexec^.args=\"/c echo %classpath\""} :
                  new String[]{"-q", "exec:exec", "-Dexec.executable=echo", "-Dexec.args=\"%classpath\""},
          new String[]{"compile"}),
    VANILLA("__NO_FILE__", "", "", new String[]{}, new String[]{});
    // jbang is not considered a build system, jbang commands can be combined with maven ones - may change later

    private final static Logger LOG = LoggerFactory.getLogger(BuildSystem.class);

    final @Nonnull String buildSystemFile;
    // do not use directly - use getExecutable
    private final @Nonnull String baseExecutable;
    // do not use directly - use getExecutable
    private final @Nonnull String wrapperFile;
    private final @Nonnull String[] classpathCmdArgs;
    private final @Nonnull String[] compileCmdArgs;

    private @Nullable String executable;

    BuildSystem(@Nonnull String buildSystemFile, @Nonnull String wrapperFile, @Nonnull String baseExecutable, @Nonnull String[] classpathCmdArgs, @Nonnull String[] compileCmdArgs) {
        this.buildSystemFile = buildSystemFile;
        this.baseExecutable = baseExecutable;
        this.wrapperFile = wrapperFile;
        this.classpathCmdArgs = classpathCmdArgs;
        this.compileCmdArgs = compileCmdArgs;
    }

    // lazily find whether the base executable of the wrapper should be used and return it
    private @Nonnull String getExecutable() {
        if (executable == null) {
            // use wrapper if found, else use basic executable
            final File wrapperInFS = lookForFile(wrapperFile, new File(""), 0);
            if (wrapperInFS != null) {
                this.executable = wrapperInFS.getAbsolutePath();
            } else {
                this.executable = baseExecutable;
            }
        }
        return executable;
    }

    static BuildSystem inferBuildSystem() {
        for (BuildSystem buildSystem : BuildSystem.values()) {
            if (new File(buildSystem.buildSystemFile).exists()) {
                return buildSystem;
            }
        }
        return BuildSystem.VANILLA;
    }

    String obtainClasspath() throws IOException, InterruptedException {
        final String[] cmd = ArrayUtils.addAll(new String[]{this.getExecutable()},
                                               this.classpathCmdArgs);
        final CmdRunResult cmdResult = runCmd(cmd);
        if (cmdResult.exitCode() != 0) {
            throw new RuntimeException(("""
                    Failed to obtain %s classpath.
                    %s command finished with exit code %s.
                    %s command: %s.
                    Error: %s""").formatted(this,
                                            this,
                                            cmdResult.exitCode,
                                            this,
                                            cmd,
                                            String.join("\n", cmdResult.outputLines())));
        }
        if (cmdResult.outputLines().isEmpty()) {
            LOG.warn("{} dependencies command ran successfully, but classpath is empty", this);
            return "";
        } else if (cmdResult.outputLines().size() == 1) {
            return cmdResult.outputLines().getFirst();
        } else {
            LOG.warn(
                    "{} dependencies command ran successfully, but multiple classpath were returned. This can happen with multi-modules projects. Combining all classpath.",
                    this);
            return String.join(File.pathSeparator, cmdResult.outputLines());
        }
    }

    void compile() throws IOException, InterruptedException {
        if (this == VANILLA) {
            return;
        }
        final String[] cmd = ArrayUtils.addAll(new String[]{this.getExecutable()},
                                               this.compileCmdArgs);
        final CmdRunResult cmdResult = runCmd(cmd);
        if (cmdResult.exitCode() != 0) {
            throw new RuntimeException(("""
                    Failed to compile with %s toolchain.
                    %s command finished with exit code %s.
                    %s command: %s.
                    Error: %s""").formatted(this, this, cmdResult.exitCode,
                                            this,
                                            cmd,
                                            String.join("\n", cmdResult.outputLines())));
        }
    }

    /**
     * Look for a file. If not found, look into the parent.
     */
    private static File lookForFile(final @Nonnull String filename, final @Nonnull File startDirectory, final int depthLimit) {
        final File absoluteDirectory = startDirectory.getAbsoluteFile();
        if (new File(absoluteDirectory, filename).exists()) {
            return new File(absoluteDirectory, filename);
        } else {
            File parentDirectory = absoluteDirectory.getParentFile();
            if (parentDirectory != null && depthLimit > 0) {
                return lookForFile(filename, parentDirectory, depthLimit - 1);
            } else {
                return null;
            }
        }
    }

    private record CmdRunResult(int exitCode, List<String> outputLines) {
    }

    private static CmdRunResult runCmd(final @Nonnull String[] cmd) throws IOException, InterruptedException {
        final Runtime run = Runtime.getRuntime();
        final Process pr = run.exec(cmd);
        final int exitCode = pr.waitFor();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            final List<String> outputLines = reader.lines().toList();
            return new CmdRunResult(exitCode, outputLines);
        }
    }
}