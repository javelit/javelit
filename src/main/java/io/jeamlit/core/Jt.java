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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;

import com.fasterxml.jackson.core.type.TypeReference;
import io.jeamlit.components.chart.EchartsComponent;
import io.jeamlit.components.data.TableComponent;
import io.jeamlit.components.input.ButtonComponent;
import io.jeamlit.components.input.CheckboxComponent;
import io.jeamlit.components.input.DateInputComponent;
import io.jeamlit.components.input.NumberInputComponent;
import io.jeamlit.components.input.RadioComponent;
import io.jeamlit.components.input.SelectBoxComponent;
import io.jeamlit.components.input.SliderComponent;
import io.jeamlit.components.input.TextAreaComponent;
import io.jeamlit.components.input.TextInputComponent;
import io.jeamlit.components.input.ToggleComponent;
import io.jeamlit.components.layout.ColumnsComponent;
import io.jeamlit.components.layout.ContainerComponent;
import io.jeamlit.components.layout.ExpanderComponent;
import io.jeamlit.components.layout.FormComponent;
import io.jeamlit.components.layout.FormSubmitButtonComponent;
import io.jeamlit.components.layout.PopoverComponent;
import io.jeamlit.components.layout.TabsComponent;
import io.jeamlit.components.media.FileUploaderComponent;
import io.jeamlit.components.multipage.JtPage;
import io.jeamlit.components.multipage.NavigationComponent;
import io.jeamlit.components.multipage.PageLinkComponent;
import io.jeamlit.components.status.ErrorComponent;
import io.jeamlit.components.text.CodeComponent;
import io.jeamlit.components.text.HtmlComponent;
import io.jeamlit.components.text.MarkdownComponent;
import io.jeamlit.components.text.TextComponent;
import io.jeamlit.components.text.TitleComponent;
import io.jeamlit.datastructure.TypedMap;
import jakarta.annotation.Nonnull;
import org.icepear.echarts.Chart;
import org.icepear.echarts.Option;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.jeamlit.core.utils.Preconditions.checkArgument;
import static io.jeamlit.core.utils.Preconditions.checkState;

/**
 * The main entrypoint for app creators.
 * Add elements with Jt.title(...).use(), Jt.button(...).use(), etc...
 * <p>
 * {@snippet :
 * import io.jeamlit.core.Jt;
 *
 * public class MyApp {
 *     public static void main(String[] args) {
 *         Jt.title("Welcome").use();
 *         String name = Jt.textInput("Enter your name").use();
 *         if (Jt.button("Submit").use()) {
 *             Jt.text("Hello, " + name).use();
 *         }
 *     }
 * }
 *}
 * <p>
 * Get the session state with {@link Jt#sessionState}.
 * Get the app cache with {@link Jt#cache}.
 */
public final class Jt {

    /**
     * Return the session state Map of the session. A session corresponds to an opened tab of the app.
     * <p>
     * The session state is maintained across re-runs.
     * Values can be stored and persisted in this map.
     * <p>
     * Examples:
     * Basic counter with session state
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * public class CounterApp {
     *     public static void main(String[] args) {
     *         // initialize a counter
     *         Jt.sessionState().putIfAbsent("counter", 0);
     *
     *         if (Jt.button("Increment").use()) {
     *             Jt.sessionState().computeInt("counter", (k, v) -> v + 1);
     *         }
     *
     *         Jt.text("Counter: " + Jt.sessionState().get("counter")).use();
     *     }
     * }
     *}
     */
    public static TypedMap sessionState() {
        final InternalSessionState session = StateManager.getCurrentSession();
        return new TypedMap(session.getUserState());
    }

    /**
     * Return the components state of the session. A session corresponds to an opened tab of the app.
     * <p>
     * The current value of any component can be obtained from this map.
     * When putting a component in the app, us the {@code .key()} method to define a specific key that will be easy
     * to access from this map.
     * <p>
     * Examples:
     * Accessing component values by key
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * public class ComponentsStateApp {
     *     public static void main(String[] args) {
     *         double volumeFromUse = Jt.slider("Volume").key("volume").min(0).max(100).value(50).use();
     *         double volumeFromState = Jt.componentsState().getDouble("volume");
     *
     *         Jt.text("Volume from slider return value: " + volumeFromUse).use();
     *         Jt.text("Value from components state map: " + volumeFromState).use();
     *     }
     * }
     *}
     */
    public static TypedMap componentsState() {
        final InternalSessionState session = StateManager.getCurrentSession();
        // NOTE: best would be to have a deep-copy-on-read map
        // here it's the responsibility of the user to not play around with the values inside this map
        return new TypedMap(Map.copyOf(session.getComponentsState()));
    }

    /**
     * Return the app cache. The app cache is shared across all sessions.
     * Put values in this map that are meant to be shared across all users.
     * For instance: database long-lived connections, ML models loaded weights, etc...
     * <p>
     * See <a href="https://docs.jeamlit.io/get-started/fundamentals/advanced-concepts#caching">documentation</a>.
     * <p>
     * Examples:
     * Caching expensive computations
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * public class CacheApp {
     *      public static void main(String[] args) {
     *          String cacheKey = "long_running_operation";
     *          Long result = Jt.cache().getLong(cacheKey);
     *
     *          if (result == null) {
     *              Jt.text("Performing a long running operation. This will take a few seconds").use();
     *              result = long_running_operation();
     *              Jt.cache().put(cacheKey, result);
     *          }
     *
     *          Jt.text("Result of long operation: " + result).use();
     *          Jt.text("Refresh or Open the page in another tab: the long running operation result will be cached").use();
     *      }
     *
     *      private static long long_running_operation(){
     *          try {
     *              Thread.sleep(5000);
     *          } catch (InterruptedException ignored) {
     *          }
     *          return 42;
     *      }
     *  }
     *}
     * <p>
     * Sharing data across users
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * public class SharedDataApp {
     *     public static void main(String[] args) {
     *         // initialization
     *         Jt.cache().putIfAbsent("counter", 0);
     *         // increment visits
     *         int totalVisits = Jt.cache().computeInt("counter", (k, v) -> v + 1);
     *
     *         Jt.text("Total app visits: " + totalVisits).use();
     *     }
     * }
     *}
     * <p>
     * Deleting values in the cache:
     * <pre>
     * {@code
     * // remove all values
     * Jt.cache().clear();
     * // remove a single key
     * Jt.cache().remove("my_key");
     * }
     * </pre>
     * {@code TypedMap} simply extends the java {@code Map} type with quality-of-life
     * casting methods like {@code getInt}, {@code getDouble}, {@code getString}, etc...
     *
     */
    public static TypedMap cache() {
        return StateManager.getCache();
    }


    /**
     * Return the current url path.
     * <p>
     * May be used for multipage apps.
     * In a single page app, will always return {@code "/"}.
     * <p>
     * Examples:
     * Conditional content based on current path
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * public class PathApp {
     *     public static void main(String[] args) {
     *         Jt.navigation(Jt.page(HomePage.class), Jt.page(DetailsPage.class)).use();
     *
     *         Jt.text("The current path is: " + Jt.urlPath()).use();
     *     }
     *
     *     public static class HomePage {
     *         public static void main(String[] args) {
     *             Jt.title("Home Page").use();
     *         }
     *     }
     *
     *     public static class DetailsPage {
     *         public static void main(String[] args) {
     *             Jt.title("Details Page").use();
     *         }
     *     }
     * }
     *}
     */
    public static String urlPath() {
        return StateManager.getUrlContext().currentPath();
    }

    /**
     * Return the current query parameters as a map.
     * <p>
     * For instance: {@code ?key1=foo&key2=bar&key2=fizz} will return
     * {"key1": ["foo"], "key2": ["bar", "fizz"]}
     * <p>
     * Examples:
     * Using query parameters for app configuration
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * import java.util.List;
     *
     * public class QueryParamsApp {
     *     public static void main(String[] args) {
     *         var params = Jt.urlQueryParameters();
     *
     *         String name = params.getOrDefault("name", List.of("unknown user")).get(0);
     *
     *         Jt.title("App Settings").use();
     *         Jt.text("Hello " + name).use();
     *         // URL: ?name=Alice would show:
     *         // Hello Alice
     *     }
     * }
     *}
     */
    // TODO consider adding a TypedMap interface with list unwrap
    public static Map<String, List<String>> urlQueryParameters() {
        return StateManager.getUrlContext().queryParameters();
    }

    /**
     * Return a deep copy of the provided object.
     * <p>
     * Utility that may be useful in combination with the cache, to implement a copy on read behavior.
     * For instance, you can get a value that is expensive to
     * instantiate from the cache, but perform a deep copy to prevent mutations and side effects across sessions.
     * <p>
     * Examples:
     * Safe copying from cache to prevent mutations
     * {@snippet :
     * import java.util.ArrayList;
     * import java.util.List;
     *
     * import io.jeamlit.core.Jt;
     *
     * import com.fasterxml.jackson.core.type.TypeReference;
     *
     * public class DeepCopyApp {
     *     public static void main(String[] args) {
     *         // init
     *         List<String> sharedList = (List<String>) Jt.cache().get("shared_list");
     *         if (sharedList == null) {
     *             sharedList = new ArrayList<>();
     *             sharedList.add("item1");
     *             sharedList.add("item2");
     *             Jt.cache().put("shared_list", sharedList);
     *         }
     *
     *         // Create a safe copy to avoid mutations affecting other sessions
     *         List<String> safeCopy = Jt.deepCopy(sharedList, new TypeReference<>() {
     *         });
     *
     *         if (Jt.button("remove elements from user lists").use()) {
     *             safeCopy.clear();
     *         }
     *
     *         Jt.text("Original list size: " + sharedList.size()).use();
     *         Jt.text("Safe copy size: " + safeCopy.size()).use();
     *     }
     * }
     *}
     *
     * @return a deep copy of the provided object.
     */
    // TODO add example usage for typeRef
    public static <T> T deepCopy(final T original, final TypeReference<T> typeRef) {
        try {
            return Shared.OBJECT_MAPPER.readValue(Shared.OBJECT_MAPPER.writeValueAsBytes(original), typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Deep copy failed", e);
        }
    }

    /**
     * Write text without Markdown or HTML parsing.
     * For monospace text, use {@link Jt#code}
     * Examples:
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class TextApp {
     *     public static void main(String[] args) {
     *         Jt.text("This is some plain text.").use();
     *
     *         Jt.text("""
     *                         This is preformatted text.
     *                         It preserves    spacing
     *                         and line breaks.
     *                         """).use();
     *     }
     * }
     *}
     *
     * @param body The string to display.
     */
    public static TextComponent.Builder text(final @Nonnull String body) {
        return new TextComponent.Builder(body);
    }

    /**
     * Display text in title formatting.
     * Each document should have a single title(), although this is not enforced.
     * <p>
     * Examples:
     * Basic title and title with markdown formatting and styling
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class TitleApp {
     *     public static void main(String[] args) {
     *         // Basic title
     *         Jt.title("This is a title").use();
     *
     *         // Title with Markdown and styling
     *         Jt.title("_Jeamlit_ is **cool** :sunglasses:").use();
     *     }
     * }
     *}
     *
     * @param body The text to display. Markdown is supported, see {@link #markdown(String)} for more details.
     */
    public static TitleComponent.Builder title(@Language("markdown") final @Nonnull String body) {
        return new TitleComponent.Builder(body);
    }

    /**
     * Display string formatted as Markdown.
     * <p>
     * Supported :
     * <ul>
     *     <li>Emoji shortcodes, such as {@code :+1:} and {@code :sunglasses:}. For a list of all supported codes, see <a href="https://www.webfx.com/tools/emoji-cheat-sheet/">https://www.webfx.com/tools/emoji-cheat-sheet/</a>.</li>
     *     <li>Tables</li>
     *     <li>Strikethrough</li>
     *     <li>Autolink: turns plain links such as URLs and email addresses into links</li>
     * </ul>
     * <p>
     * Examples:
     * Basic markdown formatting and colored text styling
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class MarkdownApp {
     *     public static void main(String[] args) {
     *         // Basic text formatting
     *         Jt.markdown("*Jeamlit* is **really** ***cool***.").use();
     *
     *         // Divider
     *         Jt.markdown("---").use();
     *
     *         // Emoji and line breaks
     *         Jt.markdown("Here's a bouquet — :tulip::cherry_blossom::rose::hibiscus::sunflower::blossom:").use();
     *     }
     * }
     *}
     *
     * @param body The text to display as Markdown.
     */
    public static MarkdownComponent.Builder markdown(final @Nonnull @Language("markdown") String body) {
        return new MarkdownComponent.Builder(body);
    }

    /**
     * Display a horizontal rule.
     * <p>
     * Examples:
     * Basic section separator
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class DividerApp {
     *     public static void main(String[] args) {
     *         Jt.title("Section 1").use();
     *         Jt.text("Content for section 1").use();
     *
     *         Jt.divider().use();
     *
     *         Jt.title("Section 2").use();
     *         Jt.text("Content for section 2").use();
     *     }
     * }
     *}
     */
    public static MarkdownComponent.Builder divider() {
        return new MarkdownComponent.Builder("---");
    }

    /**
     * Display error message.
     * <p>
     * Examples:
     * Simple error message
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ErrorApp {
     *     public static void main(String[] args) {
     *         String username = Jt.textInput("Username").use();
     *
     *         if (username.isEmpty()) {
     *             Jt.error("Username is required!").use();
     *         } else if (username.length() < 3) {
     *             Jt.error("Username must be at least 3 characters long.").use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Error with markdown formatting
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class FormattedErrorApp {
     *     public static void main(String[] args) {
     *         Jt.error("**Connection Failed**: Unable to connect to the database. Please check your settings.").use();
     *     }
     * }
     *}
     *
     * @param body The error text to display. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static ErrorComponent.Builder error(final @Language("markdown") @Nonnull String body) {
        return new ErrorComponent.Builder(body);
    }

    /**
     * Insert HTML into your app.
     * <p>
     * Adding custom HTML to your app impacts safety, styling, and maintainability.
     * We sanitize HTML with <a href="https://github.com/cure53/DOMPurify">DOMPurify</a>, but inserting HTML remains a developer risk.
     * Passing untrusted code to Jt.html or dynamically loading external code can increase the risk of vulnerabilities in your app.
     * <p>
     * {@code Jt.html} content is not iframed. Executing JavaScript is not supported.
     * <p>
     * Examples:
     * Simple HTML content
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class HtmlApp {
     *     public static void main(String[] args) {
     *         Jt.html("<h3>Custom HTML Header</h3>").use();
     *         Jt.html("<p style='color: blue;'>This is blue text</p>").use();
     *         Jt.html("<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>").use();
     *     }
     * }
     *}
     *
     * @param body The HTML code to insert.
     */
    public static HtmlComponent.Builder html(final @Nonnull @Language("HTML") String body) {
        return new HtmlComponent.Builder(body);
    }

    /**
     * Insert HTML into your app.
     * <p>
     * Adding custom HTML to your app impacts safety, styling, and maintainability.
     * We sanitize HTML with <a href="https://github.com/cure53/DOMPurify">DOMPurify</a>, but inserting HTML remains a developer risk.
     * Passing untrusted code to Jt.html or dynamically loading external code can increase the risk of vulnerabilities in your app.
     * <p>
     * {@code Jt.html} content is not iframed. Executing JavaScript is not supported.
     * <p>
     * Examples:
     * Loading HTML from file
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.nio.file.Path;
     *
     * public class HtmlFileApp {
     *     public static void main(String[] args) {
     *         // Assumes you have a file "content.html" in your project
     *         Jt.html(Path.of("content.html")).use();
     *     }
     * }
     *}
     *
     * @param filePath The path of the file containing the HTML code to insert.
     */
    public static HtmlComponent.Builder html(final @Nonnull Path filePath) {
        return new HtmlComponent.Builder(filePath);
    }

    /**
     * Display a code block with optional syntax highlighting.
     * <p>
     * Examples:
     * Simple code block
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class CodeApp {
     *     public static void main(String[] args) {
     *         Jt.code("public class HelloWorld {}").use();
     *     }
     * }
     *}
     * <p>
     * Multi-line code with syntax highlighting
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class MultilineCodeApp {
     *     public static void main(String[] args) {
     *         String pythonCode = """
     *                 import numpy as np
     *
     *                 a = np.arange(15).reshape(3, 5)
     *                 """;
     *         Jt.code(pythonCode).language("python").use();
     *     }
     * }
     *}
     *
     * @param body The string to display as code or monospace text.
     */
    public static CodeComponent.Builder code(final @Nonnull String body) {
        return new CodeComponent.Builder(body);
    }

    /**
     * Display a button widget.
     * <p>
     * Examples:
     * Basic button usage and interaction
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ButtonApp {
     *     public static void main(String[] args) {
     *         if (Jt.button("Say hello").use()) {
     *             Jt.text("Why hello there").use();
     *         } else {
     *             Jt.text("Goodbye").use();
     *         }
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this button is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static ButtonComponent.Builder button(@Language("markdown") final @Nonnull String label) {
        return new ButtonComponent.Builder(label);
    }

    /**
     * Display a checkbox widget.
     * <p>
     * Examples:
     * Basic checkbox usage
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class CheckboxApp {
     *     public static void main(String[] args) {
     *         boolean agree = Jt.checkbox("I agree").use();
     *
     *         if (agree) {
     *             Jt.text("Great!").use();
     *         }
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this checkbox is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static CheckboxComponent.Builder checkbox(@Language("markdown") final @Nonnull String label) {
        return new CheckboxComponent.Builder(label);
    }

    /**
     * Display a toggle widget.
     * <p>
     * Examples:
     * Simple toggle
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ToggleApp {
     *     public static void main(String[] args) {
     *         boolean enabled = Jt.toggle("Enable notifications").use();
     *
     *         Jt.text("Notifications: " + (enabled ? "Enabled" : "Disabled")).use();
     *     }
     * }
     *}
     * <p>
     * Toggle with default value
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ToggleDefaultApp {
     *     public static void main(String[] args) {
     *         boolean autoSave = Jt.toggle("Auto-save")
     *             .value(true)
     *             .use();
     *
     *         if (autoSave) {
     *             Jt.text("Changes will be saved automatically").use();
     *         }
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this toggle is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static ToggleComponent.Builder toggle(@Language("markdown") final @Nonnull String label) {
        return new ToggleComponent.Builder(label);
    }

    /**
     * Display a slider widget.
     * <p>
     * Examples:
     * Basic integer slider usage
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class SliderApp {
     *     public static void main(String[] args) {
     *         int age = Jt.slider("How old are you?")
     *             .min(0)
     *             .max(130)
     *             .value(25)
     *             .use();
     *
     *         Jt.text("I'm " + age + " years old").use();
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this slider is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static SliderComponent.Builder slider(@Language("markdown") final @Nonnull String label) {
        return new SliderComponent.Builder(label);
    }

    /**
     * Insert a multi-element container.
     * <p>
     * Insert an invisible container into your app that can be used to hold multiple elements.
     * This allows you to, for example, insert multiple elements into your app out of order.
     * <p>
     * To add elements to the returned container:
     * <pre>
     * {@code
     * var container = Jt.container("container-1").use();
     * Jt.yourElement().use(container);
     * }
     * </pre>
     * See examples below.
     * <p>
     * Examples:
     * Basic container usage and adding elements out of order
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ContainerApp {
     *     public static void main(String[] args) {
     *         var container = Jt.container("my-container").use();
     *
     *         Jt.text("This is inside the container").use(container);
     *         Jt.text("This is outside the container").use();
     *         Jt.text("This is inside too").use(container);
     *     }
     * }
     *}
     *
     * @param key A unique string used to identify this container
     */
    public static ContainerComponent.Builder container(final @Nonnull String key) {
        return new ContainerComponent.Builder(key, false);
    }

    /**
     * Insert a single-element container.
     * <p>
     * Insert a container into your app that can be used to hold a single element.
     * This allows you to, for example, remove elements at any point, or replace several elements at once (using a child multi-element container).
     * <p>
     * To insert/replace/clear an element on the returned container:
     * <pre>
     * {@code
     * var container = Jt.empty("empty-1").use();
     * Jt.yourElement().use(container);
     * }
     * </pre>
     * See examples below.
     * <p>
     * Examples:
     * Dynamic content replacement
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.util.List;
     *
     * public class EmptyApp {
     *     public static void main(String[] args) {
     *         var placeholder = Jt.empty("content").use();
     *         String selected = Jt.selectBox("Choose content",
     *             List.of("None", "Text", "Button")).use();
     *
     *         switch (selected) {
     *             case "Text" -> Jt.text("Dynamic text content").use(placeholder);
     *             case "Button" -> {
     *                 if (Jt.button("Dynamic button").use(placeholder)) {
     *                     Jt.text("Button clicked!").use();
     *                 }
     *             }
     *             // case "None" -> container remains empty
     *         }
     *     }
     * }
     *}
     * <p>
     * Simple animations
     * {@snippet :
     * import io.jeamlit.core.Jt;import tech.catheu.jeamlit.core.Jt;
     *
     * public class AnimationEmptyApp {
     *     public static void main(String[] args) {
     *         var emptyContainer = Jt.empty("content").use();
     *          for (i = 10; i>=1; i--) {
     *               Jt.text(i + "!").use(emptyContainer);
     *                Thread.sleep(1000);
     *           }
     *           Jt.text("Happy new Year !").use(emptyContainer);
     *           Jt.button("rerun").use();
     *     }
     * }
     *}
     *
     * @param key A unique string used to identify this container
     */
    public static ContainerComponent.Builder empty(final @Nonnull String key) {
        return new ContainerComponent.Builder(key, true);
    }

    /**
     * Insert containers laid out as side-by-side columns.
     * <p>
     * Inserts a number of multi-element containers laid out side-by-side and returns a list of container objects.
     * <p>
     * To add elements to the returned columns container:
     * <pre>
     * {@code
     * var cols = Jt.columns("my-3-cols", 3).use();
     * Jt.yourElement().use(cols.col(1));
     * Jt.yourElement().use(cols.col(0));
     * Jt.yourElement().use(cols.col(2));
     * }
     * </pre>
     * See examples below.
     * <p>
     * Examples:
     * Basic three-column layout with headers and content
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ColumnsApp {
     *     public static void main(String[] args) {
     *         var cols = Jt.columns("main-cols", 3).use();
     *
     *         Jt.title("A cat").use(cols.col(0));
     *         Jt.title("A dog").use(cols.col(1));
     *         Jt.title("An owl").use(cols.col(2));
     *     }
     * }
     *}
     *
     * @param key        A unique string used to identify this columns container
     * @param numColumns The number of columns to create
     */
    public static ColumnsComponent.Builder columns(final @Nonnull String key, final int numColumns) {
        return new ColumnsComponent.Builder(key, numColumns);
    }

    /**
     * Insert containers separated into tabs.
     * <p>
     * Inserts a number of multi-element containers as tabs.
     * Tabs are a navigational element that allows users to easily move between groups of related content.
     * <p>
     * To add elements to the returned tabs container:
     * <pre>
     * {@code
     * var tabs = Jt.tabs("my-tabs", List.of("E-commerce", "Industry", "Finance")).use();
     * // get tab by name
     * Jt.yourElement().use(tabs.tab("E-commerce"));
     * // get tab by index
     * Jt.yourElement().use(tabs.tab(2));
     * }
     * </pre>
     * See examples below.
     * <p>
     * Examples:
     * Basic tabbed interface
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.util.List;
     *
     * public class TabsApp {
     *     public static void main(String[] args) {
     *         var tabs = Jt.tabs("content-tabs", List.of("Overview", "Details", "Settings")).use();
     *
     *         Jt.text("Welcome to the overview page").use(tabs.tab("Overview"));
     *         Jt.text("Here are the details").use(tabs.tab("Details"));
     *         Jt.text("Configure your settings here").use(tabs.tab("Settings"));
     *     }
     * }
     *}
     * <p>
     * Data analysis tabs
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class DataTabsApp {
     *     public static void main(String[] args) {
     *         var tabs = Jt.tabs("analysis", List.of("Sales", "Marketing", "Finance")).use();
     *
     *         // Sales tab
     *         Jt.title("Sales Dashboard").use(tabs.tab(0));
     *         Jt.text("Total sales: $100,000").use(tabs.tab(0));
     *
     *         // Marketing tab
     *         Jt.title("Marketing Metrics").use(tabs.tab(1));
     *         Jt.text("Conversion rate: 3.5%").use(tabs.tab(1));
     *
     *         // Finance tab
     *         Jt.title("Financial Overview").use(tabs.tab(2));
     *         Jt.text("Revenue growth: +15%").use(tabs.tab(2));
     *     }
     * }
     *}
     *
     * @param key  A unique string used to identify this tabs container
     * @param tabs A list of tab labels
     */
    public static TabsComponent.Builder tabs(final @Nonnull String key, @Nonnull List<@NotNull String> tabs) {
        return new TabsComponent.Builder(key, tabs);
    }

    /**
     * Insert a multi-element container that can be expanded/collapsed.
     * <p>
     * Insert a container into your app that can be used to hold multiple elements and can be expanded or collapsed by the user.
     * When collapsed, all that is visible is the provided label.
     * <p>
     * To add elements to the returned expander:
     * <pre>
     * {@code
     * var expander = Jt.expander("my-expander", "More details").use();
     * Jt.yourElement().use(expander);
     * }
     * </pre>
     * See examples below.
     * <p>
     * Examples:
     * Basic expander with explanation content
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ExpanderApp {
     *     public static void main(String[] args) {
     *         var expander = Jt.expander("explanation", "See explanation").use();
     *
     *         Jt.text("""
     *                 [A great explanation on the why and how of life.]
     *                 """).use(expander);
     *     }
     * }
     *}
     *
     * @param key   A unique string used to identify this expander
     * @param label The label for the expander header
     */
    public static ExpanderComponent.Builder expander(final @Nonnull String key, @Nonnull String label) {
        return new ExpanderComponent.Builder(key, label);
    }

    /**
     * Insert a popover container.
     * <p>
     * Inserts a multi-element container as a popover. It consists of a button-like element and a container that opens when the button is clicked.
     * <p>
     * Opening and closing the popover will not trigger a rerun. Interacting with widgets inside of an open popover will
     * rerun the app while keeping the popover open. Clicking outside of the popover will close it.
     * <p>
     * To add elements to the returned popover:
     * {@snippet :
     * var popover = Jt.popover("my-popover", "Advanced configuration").use();
     * Jt.yourElement().use(popover);
     *}
     * See examples below.
     * <p>
     * Examples:
     * Settings popover
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.util.List;
     *
     * public class PopoverApp {
     *     public static void main(String[] args) {
     *         var settings = Jt.popover("settings", "⚙️ Settings").use();
     *
     *         Jt.text("Configure your preferences:").use(settings);
     *         boolean notifications = Jt.checkbox("Enable notifications").use(settings);
     *         String theme = Jt.selectBox("Theme", List.of("Light", "Dark")).use(settings);
     *
     *         if (notifications) {
     *             Jt.text("Notifications are enabled").use();
     *         }
     *         Jt.text("The selected theme is " + theme).use();
     *     }
     * }
     *}
     * <p>
     * Help popover with information
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class HelpPopoverApp {
     *     public static void main(String[] args) {
     *         Jt.text("Username:").use();
     *         Jt.textInput("Enter username").use();
     *
     *         var help = Jt.popover("help", "❓ Help").use();
     *         Jt.text("**Username requirements:**").use(help);
     *         Jt.text("- Must be 3-20 characters long").use(help);
     *         Jt.text("- Only letters and numbers allowed").use(help);
     *         Jt.text("- Case sensitive").use(help);
     *     }
     * }
     *}
     *
     * @param key   A unique string used to identify this popover
     * @param label The label for the popover button. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static PopoverComponent.Builder popover(final @Nonnull String key,
                                                   @Language("markdown") @Nonnull String label) {
        return new PopoverComponent.Builder(key, label);
    }

    /**
     * Create a form that batches elements together with a 'Submit' button.
     * <p>
     * A form is a container that visually groups other elements and widgets together, and contains a Submit button.
     * When the form's Submit button is pressed, all widget values inside the form will be sent to Jeamlit in a batch.
     * <p>
     * To add elements to the form:
     * <pre>
     * {@code
     * var form = Jt.form("my-form-1").use();
     * Jt.yourElement().use(form);
     * ...
     * Jt.formSubmitButton("submit form").use();
     * }
     * </pre>
     * <p>
     * Forms have a few constraints:
     * <ul>
     *     <li>Every form must contain a {@code Jt.formSubmitButton)}</li>
     *     <li>{@code Jt.button} and {@code Jt.downloadButton} cannot be added to a form</li>
     *     <li>Forms can appear anywhere in your app (sidebar, columns, etc), but they cannot be embedded inside other forms</li>
     *     <li>Within a form, the only widget that can have a callback function is {@code Jt.formSubmitButton)}</li>
     * </ul>
     * <p>
     * Examples:
     * User registration form
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class FormApp {
     *     public static void main(String[] args) {
     *         var form = Jt.form("registration").use();
     *
     *         String name = Jt.textInput("Full Name").use(form);
     *         String email = Jt.textInput("Email").use(form);
     *         int age = Jt.numberInput("Age", Integer.class).min(0).max(120).use(form);
     *         boolean subscribe = Jt.checkbox("Subscribe to newsletter").use(form);
     *
     *         if (Jt.formSubmitButton("Register").use()) {
     *             Jt.text("Welcome, " + name + "!").use();
     *             Jt.text("Email: " + email).use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Survey form
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.util.List;
     *
     * public class SurveyFormApp {
     *     public static void main(String[] args) {
     *         var form = Jt.form("survey").use();
     *         double satisfaction = Jt.slider("Satisfaction (1-10)").min(1).max(10).value(5).use(form);
     *         String feedback = Jt.textArea("Additional feedback").use(form);
     *         String department = Jt.selectBox("Department",
     *                                          List.of("Engineering", "Marketing", "Sales", "Support")).use(form);
     *
     *         if (Jt.formSubmitButton("Submit Survey").use(form)) {
     *             Jt.text("Thank you for your feedback!").use();
     *             Jt.text("Satisfaction: " + satisfaction + "/10").use();
     *         }
     *     }
     * }
     *}
     *
     * @param key A unique string used to identify this form
     */
    public static FormComponent.Builder form(final @Nonnull String key) {
        return new FormComponent.Builder(key);
    }

    /**
     * Display a form submit button.
     * <p>
     * When clicked, all widget values inside the form will be sent from the user's browser to the Jeamlit server in a batch.
     * <p>
     * Every form must have at least one {@code Jt.formSubmitButton}. A {@code Jt.formSubmitButton} cannot exist outside a form.
     * <p>
     * Examples:
     * Basic form submit button
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class FormSubmitApp {
     *     public static void main(String[] args) {
     *         var form = Jt.form("contact").use();
     *
     *         String name = Jt.textInput("Your Name").use(form);
     *         String message = Jt.textArea("Message").use(form);
     *
     *         if (Jt.formSubmitButton("Send Message").use(form)) {
     *             Jt.text("Message sent successfully!").use();
     *             Jt.text("From: " + name).use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Multiple submit buttons in same form
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class MultiSubmitApp {
     *     public static void main(String[] args) {
     *         var form = Jt.form("document").use();
     *
     *         String title = Jt.textInput("Document Title").use(form);
     *         String content = Jt.textArea("Content").use(form);
     *
     *         if (Jt.formSubmitButton("Save Draft").key("save").use(form)) {
     *             Jt.text("Draft saved: " + title).use();
     *         }
     *
     *         if (Jt.formSubmitButton("Publish").key("publish").use(form)) {
     *             Jt.text("Document published: " + title).use();
     *         }
     *     }
     * }
     *}
     *
     * @param label The text to display on the submit button
     */
    public static FormSubmitButtonComponent.Builder formSubmitButton(final @Nonnull String label) {
        return new FormSubmitButtonComponent.Builder(label);
    }

    /**
     * Display a single-line text input widget.
     * <p>
     * Examples:
     * Simple text input
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class TextInputApp {
     *     public static void main(String[] args) {
     *         String name = Jt.textInput("Your name").use();
     *
     *         if (!name.isEmpty()) {
     *             Jt.text("Hello, " + name + "!").use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Text input with validation
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ValidatedTextInputApp {
     *     public static void main(String[] args) {
     *         String email = Jt.textInput("Email address")
     *                          .placeholder("Enter your email")
     *                          .use();
     *
     *         if (!email.isEmpty() && !email.contains("@")) {
     *             Jt.error("Please enter a valid email address").use();
     *         } else if (!email.isEmpty()) {
     *             Jt.text("Valid email: " + email).use();
     *         }
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static TextInputComponent.Builder textInput(@Language("markdown") final @Nonnull String label) {
        return new TextInputComponent.Builder(label);
    }

    /**
     * Display a multi-line text input widget.
     * <p>
     * Examples:
     * Simple text area
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class TextAreaApp {
     *     public static void main(String[] args) {
     *         String feedback = Jt.textArea("Your feedback").use();
     *
     *         if (!feedback.isEmpty()) {
     *             Jt.text("Thank you for your feedback!").use();
     *             Jt.text("Character count: " + feedback.length()).use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Text area for code input
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class CodeTextAreaApp {
     *     public static void main(String[] args) {
     *         String code = Jt.textArea("Enter your Java code")
     *                         .height(200)
     *                         .placeholder("public class MyClass {\n    // Your code here\n}")
     *                         .use();
     *
     *         if (!code.isEmpty()) {
     *             Jt.text("Code preview:").use();
     *             Jt.code(code).language("java").use();
     *         }
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static TextAreaComponent.Builder textArea(@Language("markdown") final @Nonnull String label) {
        return new TextAreaComponent.Builder(label);
    }

    /**
     * Display a date input widget that can be configured to accept a single date or a date range.
     * <p>
     * Examples:
     * Simple date input
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.time.LocalDate;
     * import java.time.Period;
     *
     * public class DateInputApp {
     *     public static void main(String[] args) {
     *         LocalDate birthday = Jt.dateInput("Your birthday").use();
     *
     *         if (birthday != null) {
     *             int age = Period.between(birthday, LocalDate.now()).getYears();
     *             Jt.text("You are " + age + " years old").use();
     *         }
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this date input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static DateInputComponent.Builder dateInput(@Language("markdown") final @Nonnull String label) {
        return new DateInputComponent.Builder(label);
    }

    /**
     * Display a numeric input widget.
     * <p>
     * Examples:
     * Simple number input
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class NumberInputApp {
     *     public static void main(String[] args) {
     *         Number quantity = Jt.numberInput("Quantity").minValue(1).maxValue(100).use();
     *
     *         if (quantity != null) {
     *             Jt.text("You selected: " + quantity).use();
     *         }
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this numeric input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static NumberInputComponent.Builder<Number> numberInput(@Language("markdown") final @Nonnull String label) {
        return new NumberInputComponent.Builder<>(label);
    }

    /**
     * Display a numeric input widget.
     * <p>
     * Examples:
     * Integer input with specific type
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class TypedNumberInputApp {
     *     public static void main(String[] args) {
     *         Integer age = Jt.numberInput("Age", Integer.class)
     *                         .minValue(0)
     *                         .maxValue(150)
     *                         .use();
     *
     *         if (age != null) {
     *             String category = age < 18 ? "Minor" : age < 65 ? "Adult" : "Senior";
     *             Jt.text("Category: " + category).use();
     *         }
     *     }
     * }
     *}
     *
     * @param label      A short label explaining to the user what this numeric input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     * @param valueClass The number type class (Integer, Double, Float, etc.)
     */
    public static <T extends Number> NumberInputComponent.Builder<T> numberInput(@Language("markdown") final @Nonnull String label,
                                                                                 final Class<T> valueClass) {
        return new NumberInputComponent.Builder<>(label, valueClass);
    }

    /**
     * Display a radio button widget.
     * <p>
     * Examples:
     * Simple radio selection
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.util.List;
     *
     * public class RadioApp {
     *     public static void main(String[] args) {
     *         String size = Jt.radio("Select size",
     *             List.of("Small", "Medium", "Large")).use();
     *
     *         if (size != null) {
     *             Jt.text("Selected size: " + size).use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Radio with custom objects
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ProductRadioApp {
     *     public static void main(String[] args) {
     *         record Product(String name, double price) {}
     *
     *         Product selected = Jt
     *                 .radio("Choose product",
     *                        List.of(new Product("Basic Plan", 9.99),
     *                                new Product("Pro Plan", 19.99),
     *                                new Product("Enterprise Plan", 49.99)))
     *                 .formatFunction(e -> e.name + " ($" + e.price + ")")
     *                 .use();
     *
     *         if (selected != null) {
     *             Jt.text("You chose: " + selected.name()).use();
     *             Jt.text("Price: $" + selected.price()).use();
     *         }
     *     }
     * }
     *}
     *
     * @param label   A short label explaining to the user what this radio selection is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     * @param options The list of options to choose from
     */
    public static <T> RadioComponent.Builder<T> radio(@Language("markdown") final @Nonnull String label,
                                                      final @Nonnull List<T> options) {
        return new RadioComponent.Builder<>(label, options);
    }

    /**
     * Display a select widget.
     * <p>
     * Examples:
     * Simple dropdown selection
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.util.List;
     *
     * public class SelectBoxApp {
     *     public static void main(String[] args) {
     *         String country = Jt.selectBox("Select your country",
     *                                       List.of("United States", "Canada", "United Kingdom", "Germany", "France")).use();
     *
     *         if (country != null) {
     *             Jt.text("Selected country: " + country).use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Dropdown with default value
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ProcessingSelectBoxApp {
     *     public static void main(String[] args) {
     *         String priority = Jt.selectBox("Task priority",
     *                                        List.of("Low", "Medium", "High", "Critical"))
     *                             .index(1)
     *                             .use();
     *         Jt.text("Priority: " + priority).use();
     *     }
     * }
     *}
     *
     * @param label   A short label explaining to the user what this selection is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     * @param options The list of options to choose from
     * @param <T>     The type of the options
     */
    public static <T> SelectBoxComponent.Builder<T> selectBox(@Language("markdown") final @Nonnull String label,
                                                              final @Nonnull List<T> options) {
        return new SelectBoxComponent.Builder<>(label, options);
    }

    /**
     * Create a page for {@code Jt.navigation} in a multipage app.
     * <p>
     * Examples:
     * Basic page creation with custom title and icon
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class NavigationApp {
     *     public static class FirstPage {
     *         public static void main(String[] args) {
     *             Jt.title("First Page").use();
     *         }
     *     }
     *
     *     public static class SecondPage {
     *         public static void main(String[] args) {
     *             Jt.title("Second Page").use();
     *         }
     *     }
     *
     *     public static void main(String[] args) {
     *         var page = Jt
     *                 .navigation(Jt.page(FirstPage.class).title("First page").icon("🔥"),
     *                             Jt.page(SecondPage.class).title("Second page").icon(":favorite:"))
     *                 .use();
     *     }
     * }
     *}
     *
     * @param pageApp The class containing the main method for this page
     */
    public static JtPage.Builder page(final @Nonnull Class<?> pageApp) {
        return new JtPage.Builder(pageApp);
    }

    /**
     * Create a navigation component with multiple pages to create a multipage app.
     * <p>
     * Call {@code Jt.navigation} in your entrypoint app class to define the available pages in your app.
     * {@code Jt.navigation} use() returns the current page.
     * <p>
     * When using {@code Jt.navigation}, your entrypoint app class acts like a frame of common elements around each of your pages.
     * <p>
     * The set of available pages can be updated with each rerun for dynamic navigation.
     * By default, {@code Jt.navigation} displays the available pages in the sidebar if there is more than one page.
     * This behavior can be changed using the {@code position} builder method.
     * <p>
     * Examples:
     * Basic multipage navigation setup
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class NavigationApp {
     *     public static class FirstPage {
     *         public static void main(String[] args) {
     *             Jt.title("First Page").use();
     *         }
     *     }
     *
     *     public static class SecondPage {
     *         public static void main(String[] args) {
     *             Jt.title("Second Page").use();
     *         }
     *     }
     *
     *     public static void main(String[] args) {
     *         var page = Jt
     *                 .navigation(Jt.page(FirstPage.class).title("First page").icon("🔥"),
     *                             Jt.page(SecondPage.class).title("Second page").icon(":favorite:"))
     *                 .use();
     *     }
     * }
     *}
     *
     * @param pages The pages to include in the navigation
     */
    public static NavigationComponent.Builder navigation(final JtPage.Builder... pages) {
        return new NavigationComponent.Builder(pages);
    }

    /**
     * Display a link to another page in a multipage app or to an external page.
     * <p>
     * If another page in a multipage app is specified, clicking the {@code Jt.pageLink} element stops the current page execution
     * and runs the specified page as if the user clicked on it in the sidebar navigation.
     * <p>
     * If an external page is specified, clicking the {@code Jt.pageLink} element opens a new tab to the specified page.
     * The current script run will continue if not complete.
     * <p>
     * Examples:
     * A multipage app with the sidebar hidden.
     * A footer replaces the sidebar. The footer contains links to all pages of the app and an external link.
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * public class ToDelete {
     *
     *     public static class FirstPage {
     *         public static void main(String[] args) {
     *             Jt.title("First Page").use();
     *             Jt.text("first page content").use();
     *         }
     *     }
     *
     *     public static class SecondPage {
     *         public static void main(String[] args) {
     *             Jt.title("Second Page").use();
     *             Jt.text("Second page content").use();
     *         }
     *     }
     *
     *     public static void main(String[] args) {
     *         var page = Jt
     *                 .navigation(Jt.page(FirstPage.class).title("First page").icon("🔥"),
     *                             Jt.page(SecondPage.class).title("Second page").icon(":favorite:"))
     *                 .hidden()
     *                 .use();
     *
     *         Jt.divider().use();
     *         Jt.pageLink(FirstPage.class).use();
     *         Jt.pageLink(SecondPage.class).use();
     *         Jt.pageLink("https://github.com/jeamlit/jeamlit", "Github project").icon(":link:").use();
     *     }
     * }
     *}
     *
     * @param pageClass The class of the page to link to in a multipage app.
     */
    public static PageLinkComponent.Builder pageLink(final @Nonnull Class<?> pageClass) {
        return new PageLinkComponent.Builder(pageClass);
    }

    /**
     * Display a link to another page in a multipage app or to an external page.
     * <p>
     * If another page in a multipage app is specified, clicking the {@code Jt.pageLink} element stops the current page execution
     * and runs the specified page as if the user clicked on it in the sidebar navigation.
     * <p>
     * If an external page is specified, clicking the {@code Jt.pageLink} element opens a new tab to the specified page.
     * The current script run will continue if not complete.
     *
     * @param url   The URL to link to
     * @param label The text to display for the link. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static PageLinkComponent.Builder pageLink(final @Nonnull String url,
                                                     final @Language("markdown") @Nonnull String label) {
        return new PageLinkComponent.Builder(url, label);
    }

    /**
     * Display a file uploader widget.
     * <p>
     * Examples:
     * Basic file upload with processing
     * {@snippet :
     * import io.jeamlit.core.Jt;
     * import io.jeamlit.core.JtUploadedFile;
     *
     * import java.util.List;
     *
     * public class FileUploadApp {
     *     public static void main(String[] args) {
     *         var uploadedFiles = Jt.fileUploader("Choose a CSV file")
     *                               .type(List.of(".csv"))
     *                               .use();
     *
     *         if (!uploadedFiles.isEmpty()) {
     *             JtUploadedFile file = uploadedFiles.getFirst();
     *             Jt.text("Uploaded file: " + file.filename()).use();
     *             Jt.text("File size: " + file.content().length + " bytes").use();
     *             Jt.text("Content type: " + file.contentType()).use();
     *         }
     *     }
     * }
     *}
     *
     * @param label A short label explaining to the user what this file uploader is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static FileUploaderComponent.Builder fileUploader(@Language("markdown") final @Nonnull String label) {
        return new FileUploaderComponent.Builder(label);
    }

    /**
     * Display a chart using ECharts library.
     * See <a href="https://echarts.icepear.org/" target="_blank">echarts-java documentation</a> for more info.
     * <p>
     * Examples:
     * Plot from a {@code Chart} ({@code Bar} extends {@code Chart}).
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import org.icepear.echarts.Bar;
     *
     * public class BarChartApp {
     *     public static void main(String[] args) {
     *         Bar bar = new Bar()
     *                 .setLegend()
     *                 .setTooltip("item")
     *                 .addXAxis(new String[] { "Matcha Latte", "Milk Tea", "Cheese Cocoa", "Walnut Brownie" })
     *                 .addYAxis()
     *                 .addSeries("2015", new Number[] { 43.3, 83.1, 86.4, 72.4 })
     *                 .addSeries("2016", new Number[] { 85.8, 73.4, 65.2, 53.9 })
     *                 .addSeries("2017", new Number[] { 93.7, 55.1, 82.5, 39.1 });
     *
     *         Jt.echarts(bar).use();
     *     }
     * }
     *}
     *
     * @param chart The ECharts {@code Chart} object to display
     */
    public static EchartsComponent.Builder echarts(final @Nonnull Chart<?, ?> chart) {
        return new EchartsComponent.Builder(chart);
    }

    /**
     * Display a chart using ECharts library.
     * See <a href="https://echarts.icepear.org/" target="_blank">echarts-java documentation</a> for more info.
     * <p>
     * Examples:
     * Plot from an {@code Option}.
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import org.icepear.echarts.Option;
     * import org.icepear.echarts.charts.bar.BarSeries;
     * import org.icepear.echarts.components.coord.cartesian.CategoryAxis;
     * import org.icepear.echarts.components.coord.cartesian.ValueAxis;
     * import org.icepear.echarts.origin.util.SeriesOption;
     *
     * public class OptionChartApp {
     *     public static void main(String[] args) {
     *         CategoryAxis xAxis = new CategoryAxis()
     *                 .setType("category")
     *                 .setData(new String[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" });
     *
     *         ValueAxis yAxis = new ValueAxis().setType("value");
     *
     *         BarSeries series = new BarSeries()
     *                 .setData(new Number[] { 120, 200, 150, 80, 70, 110, 130 })
     *                 .setType("bar");
     *
     *         Option option = new Option()
     *                 .setXAxis(xAxis)
     *                 .setYAxis(yAxis)
     *                 .setSeries(new SeriesOption[] { series });
     *
     *         Jt.echarts(option).use();
     *     }
     * }
     *}
     *
     * @param chartOption The ECharts {@code Option} object to display
     */
    public static EchartsComponent.Builder echarts(final @Nonnull Option chartOption) {
        return new EchartsComponent.Builder(chartOption);
    }

    /**
     * Display a chart using ECharts library.
     * See <a href="https://echarts.icepear.org/" target="_blank">echarts-java documentation</a> for more info.
     * <p>
     * Examples:
     * Plot from a JSON {@code String}
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import org.icepear.echarts.Option;
     * import org.icepear.echarts.charts.bar.BarSeries;
     * import org.icepear.echarts.components.coord.cartesian.CategoryAxis;
     * import org.icepear.echarts.components.coord.cartesian.ValueAxis;
     * import org.icepear.echarts.origin.util.SeriesOption;
     *
     * public class OptionChartApp {
     *     public static void main(String[] args) {
     *         String echartsOptionJson = """
     *                 {
     *                   "xAxis": {
     *                     "type": "category",
     *                     "data": ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
     *                   },
     *                   "yAxis": {
     *                     "type": "value"
     *                   },
     *                   "series": [
     *                     {
     *                       "data": [150, 230, 224, 218, 135, 147, 260],
     *                       "type": "line"
     *                     }
     *                   ]
     *                 }
     *                 """;
     *
     *         Jt.echarts(echartsOptionJson).use();
     *     }
     * }
     *}
     *
     * @param chartOptionJson The ECharts option as a JSON string
     */
    public static EchartsComponent.Builder echarts(final @Language("json") String chartOptionJson) {
        return new EchartsComponent.Builder(chartOptionJson);
    }

    /**
     * Display a static table.
     * <p>
     * Examples:
     * Basic table with data objects
     * {@snippet :
     * import tech.catheu.jeamlit.core.Jt;
     *
     * import java.util.List;
     *
     * public class TableApp {
     *     public static void main(String[] args) {
     *         record Person(String name, int age, String city) {
     *         }
     *
     *         List<Object> data = List.of(new Person("Alice", 25, "New York"),
     *                                     new Person("Bob", 30, "San Francisco"),
     *                                     new Person("Charlie", 35, "Chicago"));
     *
     *         Jt.table(data).use();
     *     }
     * }
     *}
     *
     * @param rows The list of objects representing table rows
     */
    public static TableComponent.Builder table(final @Nonnull List<Object> rows) {
        return TableComponent.Builder.ofObjsList(rows);
    }

    /**
     * Display a static table.
     * <p>
     * Examples:
     * Basic table with array of objects
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * public class TableArrayApp {
     *     public static void main(String[] args) {
     *         record Product(String name, double price, boolean inStock) {}
     *
     *         Product[] products = {
     *             new Product("Laptop", 999.99, true),
     *             new Product("Mouse", 25.50, false),
     *             new Product("Keyboard", 75.00, true)
     *         };
     *
     *         Jt.table(products).use();
     *     }
     * }
     *}
     *
     * @param rows The array of objects representing table rows
     */
    public static TableComponent.Builder table(final @Nonnull Object[] rows) {
        return TableComponent.Builder.ofObjsArray(rows);
    }

    /**
     * Display a static table.
     * <p>
     * Examples:
     * Table from column arrays
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * import java.util.Map;
     *
     * public class TableColumnsArrayApp {
     *     public static void main(String[] args) {
     *         Map<String, Object[]> salesData = Map.of(
     *                 "Month", new String[]{"Jan", "Feb", "Mar", "Apr"},
     *                 "Sales", new Integer[]{1200, 1350, 1100, 1450},
     *                 "Target", new Integer[]{1000, 1300, 1200, 1400},
     *                 "Achieved", new Boolean[]{true, true, false, true}
     *         );
     *
     *         Jt.tableFromArrayColumns(salesData).use();
     *     }
     * }
     *}
     *
     * @param cols A map where keys are column names and values are arrays of column data
     */
    public static TableComponent.Builder tableFromArrayColumns(final @Nonnull Map<@NotNull String, @NotNull Object[]> cols) {
        return TableComponent.Builder.ofColumnsArrays(cols);
    }

    /**
     * Display a static table.
     * <p>
     * Examples:
     * Table from column lists
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * import java.util.List;
     * import java.util.Map;
     *
     * public class TableColumnsListApp {
     *     public static void main(String[] args) {
     *         Map<String, List<Object>> employeeData = Map.of(
     *                 "Name", List.of("Alice", "Bob", "Charlie", "Diana"),
     *                 "Department", List.of("Engineering", "Sales", "Marketing", "Engineering"),
     *                 "Salary", List.of(95000, 75000, 68000, 102000),
     *                 "Remote", List.of(true, false, true, true)
     *         );
     *
     *         Jt.tableFromListColumns(employeeData).use();
     *     }
     * }
     *}
     *
     * @param cols     A map where keys are column names and values are collections of column data
     * @param <Values> The type of collection containing the column values
     */
    public static <Values extends @NotNull SequencedCollection<@Nullable Object>> TableComponent.Builder tableFromListColumns(
            final @Nonnull Map<@NotNull String, Values> cols) {
        return TableComponent.Builder.ofColumnsLists(cols);
    }


    /**
     * Programmatically switch the current page in a multipage app.
     * <p>
     * When {@code Jt.switchPage} is called, the current page execution stops and the specified page runs as if the
     * user clicked on it in the sidebar navigation. The specified page must be recognized by Jeamlit's multipage
     * architecture (your main app class or an app class in the available pages).
     * <p>
     * Examples:
     * Conditional page switching with checkboxes
     * {@snippet :
     * import io.jeamlit.core.Jt;
     *
     * public class SwitchPageApp {
     *     public static class WelcomePage {
     *         public static void main(String[] args) {
     *             Jt.title("Welcome Page").use();
     *             Jt.text("Please complete the requirements below to proceed:").use();
     *
     *             boolean agreedToTerms = Jt.checkbox("I agree with Bob").use();
     *             boolean confirmedAge = Jt.checkbox("I agree with Alice").use();
     *
     *             if (agreedToTerms && confirmedAge) {
     *                 Jt.text("All requirements met! Redirecting to dashboard...").use();
     *                 Jt.switchPage(DashboardPage.class);
     *             } else {
     *                 Jt.text("Please check both boxes to continue.").use();
     *             }
     *         }
     *     }
     *
     *     public static class DashboardPage {
     *         public static void main(String[] args) {
     *             Jt.title("Dashboard").use();
     *             Jt.text("Welcome to your dashboard!").use();
     *             Jt.text("You have successfully completed the requirements.").use();
     *         }
     *     }
     *
     *     public static void main(String[] args) {
     *         Jt.navigation(Jt.page(WelcomePage.class).title("Welcome").icon("👋").home(),
     *                       Jt.page(DashboardPage.class).title("Dashboard").icon("📊"))
     *           .hidden()
     *           .use();
     *     }
     * }
     *}
     */
    public static void switchPage(final @Nonnull Class<?> pageApp) {
        // note: the design here is pretty hacky
        final NavigationComponent nav = StateManager.getNavigationComponent();
        checkState(nav != null,
                   "No navigation component found in app. switchPage only works with multipage app. Make sure switchPage is called after Jt.navigation().[...].use().");
        final JtPage newPage = nav.getPageFor(pageApp);
        checkArgument(newPage != null,
                      "Invalid page %s. This page is not registered in Jt.navigation().",
                      pageApp.getName());
        final InternalSessionState.UrlContext urlContext = new InternalSessionState.UrlContext(newPage.urlPath(),
                                                                                               Map.of());
        throw new BreakAndReloadAppException(sessionId -> StateManager.setUrlContext(sessionId, urlContext));
    }

    private Jt() {
    }

}
