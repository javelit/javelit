package tech.catheu.jeamlit.agent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileNameUtils {

    /**
     * Convert a file path to a class name
     * Examples:
     * src/main/java/com/example/MyClass.java -> com.example.MyClass
     * MyClass.java -> MyClass
     * multi/module/project/main/java/com/example/MyClass.java -> com.example.MyClass
     * <p>
     * should be compatible with windows
     */
    public static String classNameFor(final Path javaFile) {
        final String fileName = javaFile.toString();
        // Find the source root (.../main/java)
        final String sourceRootPath = Paths.get("main", "java").toString();
        final int srcIndex = fileName.indexOf(sourceRootPath);
        if (srcIndex == -1) {
            // assume the file is at the root in the form MyApp.java
            final String name = javaFile.getFileName().toString();
            return name.replace(".java", "");
        }

        // Extract the part after src/main/java/
        final String pathFromSource = fileName.substring(srcIndex + sourceRootPath.length());

        // Convert to class name
        return pathFromSource
                // com/example/MyClass.java --> com/example/MyClass
                .replace(".java", "")
                // com/example/MyClass --> com.example.MyClass
                .replace('/', '.');
    }

    /**
     * convert a class name to a class file path
     * com.example.MyClass --> com/example/MyClass.class
     * MyClass --> MyClass.class
     * should be compatible with windows
     */
    public static String classFilePathFor(final String className) {
        final var pathElems = className.split("\\.");
        if (pathElems.length > 1) {
            return Paths.get(pathElems[0],
                             Arrays.copyOfRange(pathElems, 1, pathElems.length)) + ".class";
        }
        return Paths.get(pathElems[0]) + ".class";
    }
}
