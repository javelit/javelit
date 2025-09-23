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
package io.jeamlit.core.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// we avoid a dependency on Guava - taking just what I need from Preconditions
// see https://github.com/google/guava/blob/master/guava/src/com/google/common/base/Preconditions.java
public class Preconditions {

    private static final Logger LOG = LoggerFactory.getLogger(Preconditions.class);

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression   a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *                     string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression           a boolean expression
     * @param errorMessageTemplate a template for the exception message should the check fail. The
     *                             message is formed by replacing each {@code %s} placeholder in the template with an
     *                             argument. These are matched by position - the first {@code %s} gets {@code
     *                             errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted message in
     *                             square braces. Unmatched placeholders will be left as-is.
     * @param errorMessageArgs     the arguments to be substituted into the message template. Arguments
     *                             are converted to strings using {@link String#valueOf(Object)}.
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, String errorMessageTemplate, @Nullable Object @org.jetbrains.annotations.Nullable ... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(lenientFormat(errorMessageTemplate,
                                                             errorMessageArgs));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression   a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *                     string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression           a boolean expression
     * @param errorMessageTemplate a template for the exception message should the check fail. The
     *                             message is formed by replacing each {@code %s} placeholder in the template with an
     *                             argument. These are matched by position - the first {@code %s} gets {@code
     *                             errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted message in
     *                             square braces. Unmatched placeholders will be left as-is.
     * @param errorMessageArgs     the arguments to be substituted into the message template. Arguments
     *                             are converted to strings using {@link String#valueOf(Object)}.
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression, @Nonnull String errorMessageTemplate, @Nullable Object @org.jetbrains.annotations.Nullable ... errorMessageArgs) {
        if (!expression) {
            throw new IllegalStateException(lenientFormat(errorMessageTemplate, errorMessageArgs));
        }
    }


    // see https://github.com/google/guava/blob/master/guava/src/com/google/common/base/Strings.java
    private static String lenientFormat(@Nullable String template, @Nullable Object @org.jetbrains.annotations.Nullable ... args) {
        template = String.valueOf(template); // null -> "null"

        if (args == null) {
            args = new Object[]{"(Object[])null"};
        } else {
            for (int i = 0; i < args.length; i++) {
                args[i] = lenientToString(args[i]);
            }
        }

        // start substituting the arguments into the '%s' placeholders
        StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;
        while (i < args.length) {
            int placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1) {
                break;
            }
            builder.append(template, templateStart, placeholderStart);
            builder.append(args[i++]);
            templateStart = placeholderStart + 2;
        }
        builder.append(template, templateStart, template.length());

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            builder.append(" [");
            builder.append(args[i++]);
            while (i < args.length) {
                builder.append(", ");
                builder.append(args[i++]);
            }
            builder.append(']');
        }

        return builder.toString();
    }

    // see https://github.com/google/guava/blob/master/guava/src/com/google/common/base/Strings.java
    @SuppressWarnings("CatchingUnchecked") // sneaky checked exception
    private static String lenientToString(@Nullable Object o) {
        if (o == null) {
            return "null";
        }
        try {
            return o.toString();
        } catch (Exception e) { // sneaky checked exception
            // Default toString() behavior - see Object.toString()
            String objectToString = o.getClass()
                                            .getName() + '@' + Integer.toHexString(System.identityHashCode(
                    o));
            // Logger is created inline with fixed name to avoid forcing Proguard to create another class.
            LOG.warn("Exception during lenientFormat for " + objectToString, e);
            return "<" + objectToString + " threw " + e.getClass().getName() + ">";
        }
    }
}
