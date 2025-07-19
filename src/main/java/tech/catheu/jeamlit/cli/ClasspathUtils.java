package tech.catheu.jeamlit.cli;

import dev.jbang.dependencies.DependencyResolver;
import dev.jbang.dependencies.ModularClassPath;
import dev.jbang.source.Project;
import dev.jbang.source.Source;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

public class ClasspathUtils {

    private static final Method DEPENDENCY_COLLECT_REFLECTION;

    static {
        try {
            DEPENDENCY_COLLECT_REFLECTION = Source.class.getDeclaredMethod("collectBinaryDependencies");
            DEPENDENCY_COLLECT_REFLECTION.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Detect and combine all required classpaths.
     * Note: maven and gradle support not implemented yet.
     */
    public static String buildClasspath(final @Nullable String providedClasspath, final @Nonnull Path javaFilePath) {
        final StringBuilder cp = new StringBuilder();
        // Add current directory for standalone Java files
        cp.append(".");

        // add provided classpath
        if (providedClasspath != null && !providedClasspath.isEmpty()) {
            cp.append(File.pathSeparator).append(providedClasspath);
        }

        // Add target/classes directory for compiled Maven classes
        cp.append(File.pathSeparator).append("target/classes");

        // add jbang style deps
        final Project jbangProject = Project.builder().build(javaFilePath);
        final Source mainSource = jbangProject.getMainSource();
        final List<String> dependencies = getDependenciesFrom(mainSource);
        if (!dependencies.isEmpty()) {
            final DependencyResolver resolver = new DependencyResolver();
            resolver.addDependencies(dependencies);
            final ModularClassPath modularClasspath = resolver.resolve();
            cp.append(File.pathSeparator).append(modularClasspath.getClassPath());
        }
        // note: multi file support of jbang not implemented

        return cp.toString();
    }

    private static List<String> getDependenciesFrom(final Source mainSource) {
        try {
            return (List<String>) DEPENDENCY_COLLECT_REFLECTION.invoke(mainSource);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
