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
 * Get the session state with Jt.sessionState().
 * Get the app cache Jt.cache().
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
     * public class CounterApp {
     *     public static void main(String[] args) {
     *         int counter = Jt.sessionState().getInt("counter", 0);
     *
     *         Jt.text("Counter: " + counter).use();
     *
     *         if (Jt.button("Increment").use()) {
     *             Jt.sessionState().put("counter", counter + 1);
     *         }
     *     }
     * }
     *}
     * <p>
     * Storing user preferences
     * {@snippet :
     * public class UserPrefsApp {
     *     public static void main(String[] args) {
     *         String name = Jt.sessionState().getString("name", "Guest");
     *         boolean darkMode = Jt.sessionState().getBoolean("dark_mode", false);
     *
     *         String newName = Jt.textInput("Your name").value(name).use();
     *         boolean newDarkMode = Jt.checkbox("Dark mode").value(darkMode).use();
     *
     *         Jt.sessionState().put("name", newName);
     *         Jt.sessionState().put("dark_mode", newDarkMode);
     *
     *         Jt.text("Hello, " + newName + "!").use();
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
     * public class ComponentsStateApp {
     *     public static void main(String[] args) {
     *         Jt.textInput("Username").key("username").use();
     *         Jt.checkbox("Remember me").key("remember").use();
     *         Jt.slider("Volume").key("volume").min(0).max(100).value(50).use();
     *
     *         String username = Jt.componentsState().getString("username", "");
     *         boolean remember = Jt.componentsState().getBoolean("remember", false);
     *         int volume = Jt.componentsState().getInt("volume", 50);
     *
     *         Jt.text("Username: " + username).use();
     *         Jt.text("Remember: " + remember).use();
     *         Jt.text("Volume: " + volume).use();
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
     * See https://docs.jeamlit.io/get-started/fundamentals/advanced-concepts#caching
     * <p>
     * Examples:
     * Caching expensive computations
     * {@snippet :
     * public class CacheApp {
     *     public static void main(String[] args) {
     *         String cacheKey = "fibonacci_100";
     *         Long result = Jt.cache().get(cacheKey, Long.class);
     *
     *         if (result == null) {
     *             Jt.text("Computing Fibonacci(100)...").use();
     *             result = computeFibonacci(100);
     *             Jt.cache().put(cacheKey, result);
     *         }
     *
     *         Jt.text("Fibonacci(100) = " + result).use();
     *     }
     *
     *     private static long computeFibonacci(int n) {
     *         // Expensive computation
     *         return n <= 1 ? n : computeFibonacci(n-1) + computeFibonacci(n-2);
     *     }
     * }
     *}
     * <p>
     * Sharing data across users
     * {@snippet :
     * public class SharedDataApp {
     *     public static void main(String[] args) {
     *         int totalVisits = Jt.cache().getInt("total_visits", 0);
     *         Jt.cache().put("total_visits", totalVisits + 1);
     *
     *         Jt.text("Total app visits: " + (totalVisits + 1)).use();
     *     }
     * }
     *}
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
     * public class PathApp {
     *     public static void main(String[] args) {
     *         String currentPath = Jt.urlPath();
     *
     *         switch (currentPath) {
     *             case "/" -> {
     *                 Jt.title("Home Page").use();
     *                 Jt.text("Welcome to the home page!").use();
     *             }
     *             case "/about" -> {
     *                 Jt.title("About Page").use();
     *                 Jt.text("Learn more about us.").use();
     *             }
     *             case "/contact" -> {
     *                 Jt.title("Contact Page").use();
     *                 Jt.text("Get in touch with us.").use();
     *             }
     *             default -> {
     *                 Jt.title("Page Not Found").use();
     *                 Jt.text("Current path: " + currentPath).use();
     *             }
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
     * public class QueryParamsApp {
     *     public static void main(String[] args) {
     *         var params = Jt.urlQueryParameters();
     *
     *         String theme = params.getOrDefault("theme", List.of("light")).get(0);
     *         String lang = params.getOrDefault("lang", List.of("en")).get(0);
     *
     *         Jt.title("App Settings").use();
     *         Jt.text("Theme: " + theme).use();
     *         Jt.text("Language: " + lang).use();
     *
     *         // URL: ?theme=dark&lang=fr would show:
     *         // Theme: dark
     *         // Language: fr
     *     }
     * }
     *}
     * <p>
     * Handling multiple values for same parameter
     * {@snippet :
     * public class MultiValueParamsApp {
     *     public static void main(String[] args) {
     *         var params = Jt.urlQueryParameters();
     *         List<String> tags = params.getOrDefault("tags", List.of());
     *
     *         Jt.title("Selected Tags").use();
     *         if (tags.isEmpty()) {
     *             Jt.text("No tags selected").use();
     *         } else {
     *             for (String tag : tags) {
     *                 Jt.text("- " + tag).use();
     *             }
     *         }
     *         // URL: ?tags=java&tags=web&tags=app would show all three tags
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
     * public class DeepCopyApp {
     *     public static void main(String[] args) {
     *         // Get shared data from cache
     *         List<String> sharedList = Jt.cache().get("shared_list", new TypeReference<List<String>>() {});
     *         if (sharedList == null) {
     *             sharedList = List.of("item1", "item2", "item3");
     *             Jt.cache().put("shared_list", sharedList);
     *         }
     *
     *         // Create a safe copy to avoid mutations affecting other sessions
     *         List<String> safeCopy = Jt.deepCopy(sharedList, new TypeReference<List<String>>() {});
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
     *             This is preformatted text.
     *             It preserves    spacing
     *             and line breaks.
     *             """).use();
     *
     *         Jt.text("Fixed-width font makes this text perfect for code snippets.").use();
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
     * public class TitleApp {
     *     public static void main(String[] args) {
     *         // Basic title
     *         Jt.title("This is a title").use();
     *
     *         // Title with Markdown and styling
     *         Jt.title("_Jeamlit_ is :blue[cool] :sunglasses:").use();
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
     * public class MarkdownApp {
     *     public static void main(String[] args) {
     *         // Basic text formatting
     *         Jt.markdown("*Jeamlit* is **really** ***cool***.").use();
     *
     *         // Colored text and styling
     *         Jt.markdown("""
     *             :red[Jeamlit] :orange[can] :green[write] :blue[text] :violet[in]
     *             :gray[pretty] :rainbow[colors] and :blue-background[highlight] text.
     *             """).use();
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
     * public class HtmlApp {
     *     public static void main(String[] args) {
     *         Jt.html("<h3>Custom HTML Header</h3>").use();
     *         Jt.html("<p style='color: blue;'>This is blue text</p>").use();
     *         Jt.html("<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>").use();
     *     }
     * }
     *}
     * <p>
     * HTML table
     * {@snippet :
     * public class HtmlTableApp {
     *     public static void main(String[] args) {
     *         String htmlTable = """
     *             <table border="1" style="border-collapse: collapse;">
     *               <thead>
     *                 <tr><th>Name</th><th>Age</th><th>City</th></tr>
     *               </thead>
     *               <tbody>
     *                 <tr><td>Alice</td><td>30</td><td>New York</td></tr>
     *                 <tr><td>Bob</td><td>25</td><td>London</td></tr>
     *               </tbody>
     *             </table>
     *             """;
     *         Jt.html(htmlTable).use();
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
     * public class CodeApp {
     *     public static void main(String[] args) {
     *         Jt.code("public class HelloWorld {}").use();
     *     }
     * }
     *}
     * <p>
     * Multi-line code with syntax highlighting
     * {@snippet :
     * public class MultilineCodeApp {
     *     public static void main(String[] args) {
     *         String javaCode = """
     *             public class Calculator {
     *                 public int add(int a, int b) {
     *                     return a + b;
     *                 }
     *             }
     *             """;
     *         Jt.code(javaCode).language("java").use();
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
     * Conditional element display
     * {@snippet :
     * public class ConditionalEmptyApp {
     *     public static void main(String[] args) {
     *         var statusContainer = Jt.empty("status").use();
     *         boolean showStatus = Jt.checkbox("Show status").use();
     *
     *         if (showStatus) {
     *             Jt.text("Status: Active").use(statusContainer);
     *         }
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
     * public class ExpanderApp {
     *     public static void main(String[] args) {
     *         var expander = Jt.expander("explanation", "See explanation").use();
     *
     *         Jt.text("""
     *             The chart above shows some numbers I picked for you.
     *             I rolled actual dice for these, so they're *guaranteed* to
     *             be random.
     *             """).use(expander);
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
     * <pre>
     * {@code
     * var popover = Jt.popover("my-popover", "Advanced configuration").use();
     * Jt.yourElement().use(popover);
     * }
     * </pre>
     * See examples below.
     * <p>
     * Examples:
     * Settings popover
     * {@snippet :
     * public class PopoverApp {
     *     public static void main(String[] args) {
     *         var settings = Jt.popover("settings", "⚙️ Settings").use();
     *
     *         Jt.text("Configure your preferences:").use(settings);
     *         boolean notifications = Jt.checkbox("Enable notifications").use(settings);
     *         String theme = Jt.selectBox("Theme", List.of("Light", "Dark")).use(settings);
     *     }
     * }
     *}
     * <p>
     * Help popover with information
     * {@snippet :
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
     * public class SurveyFormApp {
     *     public static void main(String[] args) {
     *         var form = Jt.form("survey").use();
     *
     *         int satisfaction = Jt.slider("Satisfaction (1-10)").min(1).max(10).value(5).use(form);
     *         String feedback = Jt.textArea("Additional feedback").use(form);
     *         String department = Jt.selectBox("Department",
     *             List.of("Engineering", "Marketing", "Sales", "Support")).use(form);
     *
     *         if (Jt.formSubmitButton("Submit Survey").use()) {
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
     * public class FormSubmitApp {
     *     public static void main(String[] args) {
     *         var form = Jt.form("contact").use();
     *
     *         String name = Jt.textInput("Your Name").use(form);
     *         String message = Jt.textArea("Message").use(form);
     *
     *         if (Jt.formSubmitButton("Send Message").use()) {
     *             Jt.text("Message sent successfully!").use();
     *             Jt.text("From: " + name).use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Multiple submit buttons in same form
     * {@snippet :
     * public class MultiSubmitApp {
     *     public static void main(String[] args) {
     *         var form = Jt.form("document").use();
     *
     *         String title = Jt.textInput("Document Title").use(form);
     *         String content = Jt.textArea("Content").use(form);
     *
     *         if (Jt.formSubmitButton("Save Draft").key("save").use()) {
     *             Jt.text("Draft saved: " + title).use();
     *         }
     *
     *         if (Jt.formSubmitButton("Publish").key("publish").use()) {
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
     * public class ValidatedTextInputApp {
     *     public static void main(String[] args) {
     *         String email = Jt.textInput("Email address")
     *             .placeholder("Enter your email")
     *             .use();
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
     * public class CodeTextAreaApp {
     *     public static void main(String[] args) {
     *         String code = Jt.textArea("Enter your Java code")
     *             .height(200)
     *             .placeholder("public class MyClass {\n    // Your code here\n}")
     *             .use();
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
     * <p>
     * Date range input
     * {@snippet :
     * public class DateRangeInputApp {
     *     public static void main(String[] args) {
     *         DateRange range = Jt.dateInput("Select date range")
     *             .range(true)
     *             .use();
     *
     *         if (range != null && range.start() != null && range.end() != null) {
     *             long days = ChronoUnit.DAYS.between(range.start(), range.end());
     *             Jt.text("Selected range: " + days + " days").use();
     *             Jt.text("From: " + range.start()).use();
     *             Jt.text("To: " + range.end()).use();
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
     * public class NumberInputApp {
     *     public static void main(String[] args) {
     *         Number quantity = Jt.numberInput("Quantity").min(1).max(100).use();
     *
     *         if (quantity != null) {
     *             Jt.text("You selected: " + quantity).use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Price input with validation
     * {@snippet :
     * public class PriceInputApp {
     *     public static void main(String[] args) {
     *         Double price = Jt.numberInput("Price ($)", Double.class)
     *             .min(0.01)
     *             .step(0.01)
     *             .placeholder("0.00")
     *             .use();
     *
     *         if (price != null && price > 0) {
     *             Jt.text(String.format("Price: $%.2f", price)).use();
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
     * public class TypedNumberInputApp {
     *     public static void main(String[] args) {
     *         Integer age = Jt.numberInput("Age", Integer.class)
     *             .min(0)
     *             .max(150)
     *             .use();
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
     * public class ProductRadioApp {
     *     public static void main(String[] args) {
     *         record Product(String name, double price) {
     *             @Override public String toString() { return name + " ($" + price + ")"; }
     *         }
     *
     *         Product selected = Jt.radio("Choose product",
     *             List.of(
     *                 new Product("Basic Plan", 9.99),
     *                 new Product("Pro Plan", 19.99),
     *                 new Product("Enterprise Plan", 49.99)
     *             )).use();
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
     * public class SelectBoxApp {
     *     public static void main(String[] args) {
     *         String country = Jt.selectBox("Select your country",
     *             List.of("United States", "Canada", "United Kingdom", "Germany", "France")).use();
     *
     *         if (country != null) {
     *             Jt.text("Selected country: " + country).use();
     *         }
     *     }
     * }
     *}
     * <p>
     * Dropdown with default value and processing
     * {@snippet :
     * public class ProcessingSelectBoxApp {
     *     public static void main(String[] args) {
     *         String priority = Jt.selectBox("Task priority",
     *             List.of("Low", "Medium", "High", "Critical"))
     *             .value("Medium")
     *             .use();
     *
     *         if (priority != null) {
     *             String color = switch (priority) {
     *                 case "Low" -> "green";
     *                 case "Medium" -> "yellow";
     *                 case "High" -> "orange";
     *                 case "Critical" -> "red";
     *                 default -> "gray";
     *             };
     *             Jt.text("Priority: " + priority + " (" + color + ")").use();
     *         }
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
     * public class NavigationApp {
     *     public static void main(String[] args) {
     *         var page = Jt.navigation(
     *             Jt.page(FirstPage.class).title("First page").icon("🔥"),
     *             Jt.page(SecondPage.class).title("Second page").icon(":material/favorite:")
     *         ).use();
     *
     *         page.run();
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
     * public class MultiPageApp {
     *     public static void main(String[] args) {
     *         var currentPage = Jt.navigation(
     *             Jt.page(CreateAccountPage.class).title("Create your account"),
     *             Jt.page(ManageAccountPage.class).title("Manage your account"),
     *             Jt.page(LearnPage.class).title("Learn about us"),
     *             Jt.page(TrialPage.class).title("Try it out")
     *         ).use();
     *
     *         currentPage.run();
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
     * Basic line chart with random data
     * {@snippet :
     * public class LineChartApp {
     *     public static void main(String[] args) {
     *         // Create sample data (equivalent to pandas DataFrame with random numbers)
     *         List<Number> data = List.of(1, 5, 2, 6, 2, 1);
     *
     *         var lineChart = new org.icepear.echarts.charts.line.Line()
     *             .addData(data);
     *
     *         Jt.echarts(lineChart).use();
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
     *
     * @param chartOption The ECharts {@code Option} object to display
     */
    public static EchartsComponent.Builder echarts(final @Nonnull Option chartOption) {
        return new EchartsComponent.Builder(chartOption);
    }

    /**
     * Display a chart using ECharts library.
     * See <a href="https://echarts.icepear.org/" target="_blank">echarts-java documentation</a> for more info.
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
     * public class TableApp {
     *     public static void main(String[] args) {
     *         record Person(String name, int age, String city) {}
     *
     *         List<Object> data = List.of(
     *             new Person("Alice", 25, "New York"),
     *             new Person("Bob", 30, "San Francisco"),
     *             new Person("Charlie", 35, "Chicago")
     *         );
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
     *
     * @param rows The array of objects representing table rows
     */
    public static TableComponent.Builder table(final @Nonnull Object[] rows) {
        return TableComponent.Builder.ofObjsArray(rows);
    }

    /**
     * Display a static table.
     *
     * @param cols A map where keys are column names and values are arrays of column data
     */
    public static TableComponent.Builder tableFromArrayColumns(final @Nonnull Map<@NotNull String, @NotNull Object[]> cols) {
        return TableComponent.Builder.ofColumnsArrays(cols);
    }

    /**
     * Display a static table.
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
