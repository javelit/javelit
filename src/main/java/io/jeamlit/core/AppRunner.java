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
package io.jeamlit.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.jeamlit.core.utils.JavaUtils.stackTraceString;
import static io.jeamlit.core.utils.LangUtils.optional;
import static io.jeamlit.core.utils.StringUtils.percentEncode;


final class AppRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AppRunner.class);
    private static final Pattern CLASSLOADER_MISMATCH_CLASSNAME_P = Pattern.compile("class (.*?) cannot be cast to");

    private final @Nonnull AtomicReference<Method> mainMethod = new AtomicReference<>();
    private final @Nonnull Semaphore reloadAvailable = new Semaphore(1, true);
    private final @Nonnull Reloader reloader;


    // NOTE: using the server.builder is not a good practice but allows to move faster for the moment
    AppRunner(final @Nonnull Server.Builder builder) {
        if (builder.appPath != null) {
            this.reloader = new FileReloader(builder);
        } else if (builder.appClass != null) {
            this.reloader = new ClassReloader(builder);
        } else {
            throw new IllegalArgumentException(
                    "Either appPath or appClass should be provided. Please reach out to support.");
        }
        new Thread(() -> {
            try {
                reloadAvailable.acquire();
                if (mainMethod.get() == null) {
                    LOG.info("Compiling the app for the first time.");
                    reload();
                    LOG.info("First time compilation successful.");
                }
            } catch (Exception e) {
                LOG.warn(
                        "First compilation failed. Will re-attempt compilation when user connects and return the error to the user.",
                        e);
            } finally {
                reloadAvailable.release();
            }
        }).start();
    }

    void reload() {
        mainMethod.set(reloader.reload());
    }

    /**
     * @throws CompilationException if it is called for the first time, the files have never been compiled and the compilation failed
     */
    void runApp(final String sessionId) {
        // if necessary: load the app for the first time
        if (mainMethod.get() == null) {
            StateManager.beginExecution(sessionId);
            try {
                reloadAvailable.acquire();
                if (mainMethod.get() == null) {
                    LOG.warn("Pre-compilation of the app failed the first time. Attempting first compilation again.");
                    mainMethod.set(reloader.reload());
                }
            } catch (InterruptedException e) {
                Jt.error("Compilation interrupted.").use();
            } catch (Exception e) {
                if (!(e instanceof CompilationException)) {
                    LOG.error("Unknown error type: {}", e.getClass(), e);
                }
                throw e;
            } finally {
                reloadAvailable.release();
                StateManager.endExecution();
            }
        }

        boolean doRerun = false;
        Consumer<String> runAfterBreak = null;
        try {
            StateManager.beginExecution(sessionId);
            // Apps may call ServiceLoader.load(SomeClass.class). This would use the current thread context classloader (not set here) or fallback to the System classloader.
            // We need to use the HotClassLoader used to define the mainMethod
            // NOTE: this is only necessary for when the mainMethod is built from a file but this should have no
            // impact on the case where the mainMethod is passed directly
            final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                final Method method = mainMethod.get();
                Thread.currentThread().setContextClassLoader(method.getDeclaringClass().getClassLoader());
                method.invoke(null, new Object[]{new String[]{}});
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        } catch (Exception e) {
            if (!(e instanceof InvocationTargetException || e instanceof DuplicateWidgetIDException || e instanceof PageRunException)) {
                LOG.error("Unexpected error type: {}", e.getClass(), e);
            }
            if (e.getCause() instanceof BreakAndReloadAppException u) {
                runAfterBreak = u.runAfterBreak;
                doRerun = true;
            } else if (e.getCause() != null && e.getCause()
                                                .getCause() instanceof BreakAndReloadAppException u) {
                runAfterBreak = u.runAfterBreak;
                doRerun = true;
            } else {
                final Throwable t = unwrapException(e);
                // send error feedback as in-app components - their lifecycle is managed like all other components
                sendUserFeedback(t);
                if (StateManager.isDeveloperSession(sessionId)) {
                    sendDeveloperFeedback(t);
                }
            }
        } finally {
            StateManager.endExecution();
        }

        if (doRerun) {
            if (runAfterBreak != null) {
                runAfterBreak.accept(sessionId);
            }
            runApp(sessionId);
        }
    }

    private void sendDeveloperFeedback(Throwable t) {
        if (t instanceof ClassCastException cce) {
            if (isClassloaderMismatch(cce)) {
                final String className = extractSourceClassName(cce);
                final JtContainer devC = Jt.container().border(true).use();
                Jt.markdown("""
                                    <sup>_This message only appears in **Dev Mode**_</sup> \s
                                    Following the hot-reload, a `%s` value in the Cache, Session State or Component State is not valid anymore. \s
                                    Try to clear the cache. \s
                                    To limit this issue, try to put the Class definition of `%s` in a separate
                                    Java file. During hot-reload, Jeamlit tries its best to not redefine a Class that is unchanged. \s
                                    This issue cannot happen in production. \s
                                    """.formatted(className, className)).use(devC);
                Jt.button("Clear cache").icon(":delete:").onClick(o -> StateManager.developerReset()).use(devC);
            }
        }
    }

    // find whether the ClassCastException is caused by a ClassLoader mismatch of Jeamlit classloaders
    private boolean isClassloaderMismatch(final ClassCastException cce) {
        String message = cce.getMessage();
        if (message == null) {
            return false;
        }
        final String loaderPattern = "of loader " + FileReloader.HotClassLoader.class.getName();
        // Check if the pattern appears twice (two different HotClassLoader instances)
        int firstIndex = message.indexOf(loaderPattern);
        if (firstIndex == -1) {
            return false;
        }
        int secondIndex = message.indexOf(loaderPattern, firstIndex + loaderPattern.length());
        return secondIndex != -1;
    }

    // for the classloader mismatch case
    private String extractSourceClassName(final @Nonnull ClassCastException cce) {
        final Matcher m = CLASSLOADER_MISMATCH_CLASSNAME_P.matcher(cce.getMessage() == null ? "" : cce.getMessage());
        return m.find() ? m.group(1) : "unknown class";
    }

    private static void sendUserFeedback(final Throwable error) {
        final String exceptionSimpleName = error.getClass().getSimpleName();
        final String errorMessage = optional(error.getMessage()).orElse("[ no error message ]");
        final String stackTrace = stackTraceString(error);
        final String googleLink = "https://www.google.com/search?q=" + percentEncode(
                exceptionSimpleName + " " + errorMessage);
        final String chatGptLink = "https://chatgpt.com/?q=" + percentEncode(String.join("\n",
                                                                                         List.of("Help me fix the following issue. I use Java Jeamlit and got:",
                                                                                                 exceptionSimpleName,
                                                                                                 errorMessage,
                                                                                                 stackTrace)));
        @Language("markdown") String errorMarkdown = """
                **%s**: %s
                
                **Stacktrace**:
                ```
                %s
                ```
                [Ask Google](%s) • [Ask ChatGPT](%s)
                """.formatted(exceptionSimpleName,
                              errorMessage,
                              stackTrace,
                              googleLink,
                              chatGptLink);

        // Send error as a component usage - its lifecycle is managed like all other components
        Jt.error(errorMarkdown).use();
    }

    private static Throwable unwrapException(Throwable error) {
        if (error instanceof PageRunException) {
            error = error.getCause();
        }
        if (error instanceof InvocationTargetException) {
            error = error.getCause();
        }
        return error;
    }
}
