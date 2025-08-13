package tech.catheu.jeamlit.core.utils;

import jakarta.annotation.Nonnull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class JavaUtils {

    /// same as getting the String of a stack trace, but with every calls in the stack that belong to the internals of Jeamlit removed
    public static String stackTraceString(final @Nonnull Throwable throwable) {
        return stackTraceString(throwable,"jdk.internal.reflect.DirectMethodHandleAccessor");
    }

    /// return the stack trace string of the Throwable, with every calls in the stack encountered from filterPrefix removed
    protected static String stackTraceString(final @Nonnull Throwable t, final @Nonnull String filterPrefix) {
        filterInPlace(t, filterPrefix);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private static void filterInPlace(Throwable t, String filterPrefix) {
        if (t == null) return;

        StackTraceElement[] stack = t.getStackTrace();

        // Find the first match
        int cutoffIndex = -1;
        for (int i = 0; i < stack.length; i++) {
            if (stack[i].getClassName().startsWith(filterPrefix)) {
                cutoffIndex = i;
                break;
            }
        }

        // If found, cut the stack there
        if (cutoffIndex != -1) {
            stack = Arrays.copyOf(stack, cutoffIndex);
        }

        t.setStackTrace(stack);

        // Recurse into causes & suppressed exceptions
        filterInPlace(t.getCause(), filterPrefix);
        for (Throwable suppressed : t.getSuppressed()) {
            filterInPlace(suppressed, filterPrefix);
        }
    }

}
