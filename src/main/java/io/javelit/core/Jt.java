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
package io.javelit.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import io.javelit.components.chart.EchartsComponent;
import io.javelit.components.data.TableComponent;
import io.javelit.components.input.ButtonComponent;
import io.javelit.components.input.CheckboxComponent;
import io.javelit.components.input.DateInputComponent;
import io.javelit.components.input.NumberInputComponent;
import io.javelit.components.input.RadioComponent;
import io.javelit.components.input.SelectBoxComponent;
import io.javelit.components.input.SliderComponent;
import io.javelit.components.input.TextAreaComponent;
import io.javelit.components.input.TextInputComponent;
import io.javelit.components.input.ToggleComponent;
import io.javelit.components.layout.ColumnsComponent;
import io.javelit.components.layout.ContainerComponent;
import io.javelit.components.layout.ExpanderComponent;
import io.javelit.components.layout.FormComponent;
import io.javelit.components.layout.FormSubmitButtonComponent;
import io.javelit.components.layout.PopoverComponent;
import io.javelit.components.layout.TabsComponent;
import io.javelit.components.media.AudioComponent;
import io.javelit.components.media.AudioInputComponent;
import io.javelit.components.media.FileUploaderComponent;
import io.javelit.components.media.ImageComponent;
import io.javelit.components.media.PdfComponent;
import io.javelit.components.multipage.PageLinkComponent;
import io.javelit.components.status.CalloutComponent;
import io.javelit.components.text.CodeComponent;
import io.javelit.components.text.HtmlComponent;
import io.javelit.components.text.MarkdownComponent;
import io.javelit.components.text.TextComponent;
import io.javelit.components.text.TitleComponent;
import io.javelit.datastructure.TypedMap;
import jakarta.annotation.Nonnull;
import org.icepear.echarts.Chart;
import org.icepear.echarts.Option;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * The main entrypoint for app creators.
 * Add elements with Jt.title(...).use(), Jt.button(...).use(), etc...
 * <p>
 * {@snippet :
 * import io.javelit.core.Jt;
 *
 * public class MyApp {
 *   public static void main(String[] args) {
 *     Jt.title("Welcome").use();
 *     String name = Jt.textInput("Enter your name").use();
 *     if (Jt.button("Submit").use()) {
 *       Jt.text("Hello, " + name).use();
 *     }
 *   }
 * }
 * }
 * <p>
 * Get the session state with {@link Jt#sessionState}.
 * Get the app cache with {@link Jt#cache}.
 */
@SuppressWarnings("JavadocReference")
public final class Jt {

  public static final JtContainer SIDEBAR = JtContainer.SIDEBAR;

  /**
   * Return the session state Map of the session. A session corresponds to an
   * opened tab of the app.
   * <p>
   * The session state is maintained across re-runs.
   * Values can be stored and persisted in this map.
   * <p>
   * Examples:
   * Basic counter with session state
   * {@snippet file = "CounterApp.java" appUrl =
   * "https://javelit-container-usfu-production.up.railway.app" appHeight = "300"}
   */
  public static TypedMap sessionState() {
    return StateManager.publicSessionState();
  }

  /**
   * Return the components state of the session. A session corresponds to an
   * opened tab of the app.
   * <p>
   * The current value of any component can be obtained from this map.
   * When putting a component in the app, us the {@code .key()} method to define a
   * specific key that will be easy
   * to access from this map.
   * <p>
   * Examples:
   * Accessing component values by key
   * {@snippet file = "ComponentsStateApp.java" appUrl =
   * "https://javelit-container-qkdk-production.up.railway.app" appHeight = "300"}
   */
  public static TypedMap componentsState() {
    return StateManager.publicComponentsState();
  }

  /**
   * Update a component's value by its user-defined key.
   * <p>
   * This method allows programmatic updating of component state. Limits:
   * <ul>
   * <li>You cannot modify the value of a component that has not been rendered
   * with a {@code .key()} in the session yet.</li>
   * <li>You cannot modify the value of a component that has already been rendered
   * in the current app run.</li>
   * </ul>
   * Learn more in the <a href=
   * "https://docs.javelit.io/develop/concepts/design/buttons#buttons-to-modify-or-reset-other-widgets">modify
   * widget examples</a>.
   * <p>
   * This method validates that the provided value is of the correct type and
   * respects constraints if any (for instance, value range).
   * <p>
   * Examples:
   * Programmatically update a text input value
   * {@snippet file = "UpdateStateApp.java" appUrl =
   * "https://javelit-container-production-5b01.up.railway.app/" appHeight =
   * "300"}
   * <p>
   *
   * @param key   The key of the component (as set via {@code .key()})
   * @param value The new value to set
   * @throws IllegalArgumentException if userKey is null or the provided key does
   *                                  not match an existing component
   * @throws IllegalStateException    if component has already been rendered in
   *                                  the current execution
   * @throws ClassCastException       if value type does not match component type
   */
  public static void setComponentState(final @Nonnull String key, final @Nullable Object value) {
    StateManager.handleUserCodeComponentUpdate(key, value);
  }

  /**
   * Return the app cache. The app cache is shared across all sessions.
   * Put values in this map that are meant to be shared across all users.
   * For instance: database long-lived connections, ML models loaded weights,
   * etc...
   * <p>
   * See <a href=
   * "https://docs.javelit.io/get-started/fundamentals/advanced-concepts#caching">documentation</a>.
   * <p>
   * Examples:
   * Caching expensive computations
   * {@snippet file = "CacheApp.java" appUrl =
   * "https://javelit-container-fyxu-production.up.railway.app" appHeight = "300"}
   * <p>
   * Sharing data across users
   * {@snippet file = "SharedDataApp.java" appUrl =
   * "https://javelit-container-xrmg-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Deleting values in the cache:
   * 
   * <pre>
   * {@code
   * // remove all values
   * Jt.cache().clear();
   * // remove a single key
   * Jt.cache().remove("my_key");
   * }
   * </pre>
   * 
   * {@code TypedMap} simply extends the java {@code Map} type with
   * quality-of-life
   * casting methods like {@code getInt}, {@code getDouble}, {@code getString},
   * etc...
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
   * {@snippet file = "PathApp.java" appUrl =
   * "https://javelit-container-uflt-production.up.railway.app/" appHeight =
   * "300"}
   */
  public static String urlPath() {
    return StateManager.getUrlContext().currentPath();
  }

  /**
   * Return the current URL query parameters as a map.
   * <p>
   * For instance: {@code ?key1=foo&key2=bar&key2=fizz} in the URL will return
   * {@code {"key1": ["foo"], "key2": ["bar", "fizz"]}}
   * <p>
   * Examples:
   * Using query parameters for app configuration
   * {@snippet file = "QueryParamsApp.java" appUrl =
   * "https://javelit-container-production-f57e.up.railway.app/?name=Alice"
   * appHeight = "300"}
   */
  // TODO consider adding a TypedMap interface with list unwrap
  public static Map<String, List<String>> urlQueryParameters() {
    return StateManager.getUrlContext().queryParameters();
  }

  /**
   * Return a deep copy of the provided object.
   * <p>
   * Utility that may be useful in combination with the cache, to implement a copy
   * on read behavior.
   * For instance, you can get a value that is expensive to
   * instantiate from the cache, but perform a deep copy to prevent mutations and
   * side effects across sessions.
   * <p>
   * Examples:
   * Safe copying from cache to prevent mutations
   * {@snippet file = "DeepCopyApp.java" appUrl =
   * "https://javelit-container-wlct-production.up.railway.app/" appHeight =
   * "300"}
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
   * {@snippet file = "TextExample.java" appUrl =
   * "https://javelit-container-production-0e7a.up.railway.app/" appHeight =
   * "250"}
   *
   * @param body The string to display.
   */
  public static TextComponent.Builder text(final @Nonnull String body) {
    return new TextComponent.Builder(body);
  }

  /**
   * Display text in title formatting.
   * Each document should have a single {@code Jt.title()}, although this is not
   * enforced.
   * <p>
   * Examples:
   * Basic title and title with markdown formatting and styling
   * {@snippet file = "TitleApp.java" appUrl =
   * "https://javelit-container-production-764a.up.railway.app/" appHeight =
   * "300"}
   *
   * @param body The text to display. Markdown is supported, see
   *             {@link #markdown(String)} for more details.
   */
  public static TitleComponent.Builder title(@Language("markdown") final @Nonnull String body) {
    return new TitleComponent.Builder(body, 1);
  }

  /**
   * Display text in header formatting.
   * <p>
   * Examples:
   * Basic header and header with markdown formatting and styling
   * {@snippet file = "HeaderApp.java" appUrl =
   * "https://javelit-container-faea-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param body The text to display. Markdown is supported, see
   *             {@link #markdown(String)} for more details.
   */
  public static TitleComponent.Builder header(@Language("markdown") final @Nonnull String body) {
    return new TitleComponent.Builder(body, 2);
  }

  /**
   * Display text in subheader formatting.
   * <p>
   * Examples:
   * Basic subheader and subheader with markdown formatting and styling
   * {@snippet file = "SubHeaderApp.java" appUrl =
   * "https://javelit-container-jey-production.up.railway.app/" appHeight = "300"}
   *
   * @param body The text to display. Markdown is supported, see
   *             {@link #markdown(String)} for more details.
   */
  public static TitleComponent.Builder subheader(@Language("markdown") final @Nonnull String body) {
    return new TitleComponent.Builder(body, 3);
  }

  /**
   * Display string formatted as Markdown.
   * <p>
   * Supported :
   * <ul>
   * <li>Emoji shortcodes, such as {@code :+1:} and {@code :sunglasses:}. For a
   * list of all supported codes, see <a href=
   * "https://www.webfx.com/tools/emoji-cheat-sheet/">https://www.webfx.com/tools/emoji-cheat-sheet/</a>.</li>
   * <li>Tables</li>
   * <li>Strikethrough</li>
   * <li>Autolink: turns plain links such as URLs and email addresses into
   * links</li>
   * </ul>
   * <p>
   * Examples:
   * Basic markdown formatting and colored text styling
   * {@snippet file = "MarkdownApp.java" appUrl =
   * "https://javelit-container-nlt3-production.up.railway.app/" appHeight =
   * "300"}
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
   * {@snippet file = "DividerApp.java" appUrl =
   * "https://javelit-container-hru4-production.up.railway.app" appHeight = "350"}
   */
  public static MarkdownComponent.Builder divider() {
    return new MarkdownComponent.Builder("---");
  }

  /**
   * Display a horizontal rule.
   * Deprecated. Use Jt.divider() instead. Passing unique keys is not necessary
   * anymore.
   *
   * @param key A custom key.
   */
  @Deprecated(forRemoval = true) // use Jt.divider() instead. Passing unique keys is not necessary anymore.
  public static MarkdownComponent.Builder divider(final @Nonnull String key) {
    return new MarkdownComponent.Builder("---").key(key);
  }

  /**
   * Display error message.
   * <p>
   * Examples:
   * Simple error message
   * {@snippet file = "ErrorApp.java" appUrl =
   * "https://javelit-container-mh0v-production.up.railway.app" appHeight = "300"}
   * <p>
   * Error with markdown formatting
   * {@snippet file = "FormattedErrorApp.java" appUrl =
   * "https://javelit-container-adrx-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param body The error text to display. Markdown is supported, see
   *             {@link Jt#markdown(String)} for more details.
   */
  public static CalloutComponent.Builder error(final @Language("markdown") @Nonnull String body) {
    return CalloutComponent.Builder.newError(body);
  }

  /**
   * Display warning message.
   * <p>
   * Examples:
   * Simple warning message
   * {@snippet file = "WarningApp.java" appUrl =
   * "https://javelit-container-2hya-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Warning with markdown formatting
   * {@snippet file = "FormattedWarningApp.java" appUrl =
   * "https://javelit-container-n7e-production.up.railway.app/" appHeight = "300"}
   *
   * @param body The warning text to display. Markdown is supported, see
   *             {@link Jt#markdown(String)} for more details.
   */
  public static CalloutComponent.Builder warning(final @Language("markdown") @Nonnull String body) {
    return CalloutComponent.Builder.newWarning(body);
  }

  /**
   * Display success message.
   * <p>
   * Examples:
   * Simple success message
   * {@snippet file = "SuccessApp.java" appUrl =
   * "https://javelit-container-util-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Success with markdown formatting
   * {@snippet file = "FormattedSuccessApp.java" appUrl =
   * "https://javelit-container-shmx-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param body The success text to display. Markdown is supported, see
   *             {@link Jt#markdown(String)} for more details.
   */
  public static CalloutComponent.Builder success(final @Language("markdown") @Nonnull String body) {
    return CalloutComponent.Builder.newSuccess(body);
  }

  /**
   * Display info message.
   * <p>
   * Examples:
   * Simple info message
   * {@snippet file = "InfoApp.java" appUrl =
   * "https://javelit-container-sfd2-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Info with markdown formatting
   * {@snippet file = "FormattedInfoApp.java" appUrl =
   * "https://javelit-container-o2ma-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param body The info text to display. Markdown is supported, see
   *             {@link Jt#markdown(String)} for more details.
   */
  public static CalloutComponent.Builder info(final @Language("markdown") @Nonnull String body) {
    return CalloutComponent.Builder.newInfo(body);
  }

  /**
   * Insert HTML into your app.
   * <p>
   * Adding custom HTML to your app impacts safety, styling, and maintainability.
   * We sanitize HTML with
   * <a href="https://github.com/cure53/DOMPurify">DOMPurify</a>, but inserting
   * HTML remains a developer risk.
   * Passing untrusted code to Jt.html or dynamically loading external code can
   * increase the risk of vulnerabilities in your app.
   * <p>
   * {@code Jt.html} content is not iframed. Executing JavaScript is not
   * supported.
   * <p>
   * Examples:
   * Simple HTML content
   * {@snippet file = "HtmlApp.java" appUrl =
   * "https://javelit-container-xqjt-production.up.railway.app/" appHeight =
   * "300"}
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
   * We sanitize HTML with
   * <a href="https://github.com/cure53/DOMPurify">DOMPurify</a>, but inserting
   * HTML remains a developer risk.
   * Passing untrusted code to Jt.html or dynamically loading external code can
   * increase the risk of vulnerabilities in your app.
   * <p>
   * {@code Jt.html} content is not iframed. Executing JavaScript is not
   * supported.
   * <p>
   * Examples:
   * Loading HTML from file
   * {@snippet file = "HtmlFileApp.java"}
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
   * {@snippet file = "CodeApp.java" appUrl =
   * "https://javelit-container-748i-production.up.railway.app" appHeight = "300"}
   * <p>
   * Multi-line code with syntax highlighting
   * {@snippet file = "MultilineCodeApp.java" appUrl =
   * "https://javelit-container-lzvc-production.up.railway.app/" appHeight =
   * "300"}
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
   * {@snippet file = "ButtonApp.java" appUrl =
   * "https://javelit-container-p3l0-production.up.railway.app" appHeight = "300"}
   *
   * @param label A short label explaining to the user what this button is for.
   *              Markdown is supported, see {@link Jt#markdown(String)} for more
   *              details.
   */
  public static ButtonComponent.Builder button(@Language("markdown") final @Nonnull String label) {
    return new ButtonComponent.Builder(label);
  }

  /**
   * Display a checkbox widget.
   * <p>
   * Examples:
   * Basic checkbox usage
   * {@snippet file = "CheckboxApp.java" appUrl =
   * "https://javelit-container-dq4o-production.up.railway.app" appHeight = "300"}
   *
   * @param label A short label explaining to the user what this checkbox is for.
   *              Markdown is supported, see {@link Jt#markdown(String)} for more
   *              details.
   */
  public static CheckboxComponent.Builder checkbox(@Language("markdown") final @Nonnull String label) {
    return new CheckboxComponent.Builder(label);
  }

  /**
   * Display a toggle widget.
   * <p>
   * Examples:
   * Simple toggle
   * {@snippet file = "ToggleApp.java" appUrl =
   * "https://javelit-container-nlsw-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Toggle with default value
   * {@snippet file = "ToggleDefaultApp.java" appUrl =
   * "https://javelit-container-niog-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param label A short label explaining to the user what this toggle is for.
   *              Markdown is supported, see {@link Jt#markdown(String)} for more
   *              details.
   */
  public static ToggleComponent.Builder toggle(@Language("markdown") final @Nonnull String label) {
    return new ToggleComponent.Builder(label);
  }

  /**
   * Display a slider widget.
   * <p>
   * Examples:
   * Basic integer slider usage
   * {@snippet file = "SliderApp.java" appUrl =
   * "https://javelit-container-ats7-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param label A short label explaining to the user what this slider is for.
   *              Markdown is supported, see {@link Jt#markdown(String)} for more
   *              details.
   */
  public static SliderComponent.Builder slider(@Language("markdown") final @Nonnull String label) {
    return new SliderComponent.Builder(label);
  }

  /**
   * Insert a multi-element container.
   * <p>
   * Insert an invisible container into your app that can be used to hold multiple
   * elements.
   * This allows you to, for example, insert multiple elements into your app out
   * of order.
   * <p>
   * To add elements to the returned container:
   * 
   * <pre>
   * {@code
   * var container = Jt.container("container-1").use();
   * Jt.yourElement().use(container);
   * }
   * </pre>
   * 
   * See examples below.
   * <p>
   * Examples:
   * Basic container usage and adding elements out of order
   * {@snippet file = "ContainerApp.java" appUrl =
   * "https://javelit-container-xdhp-production.up.railway.app" appHeight = "300"}
   *
   */
  public static ContainerComponent.Builder container() {
    return new ContainerComponent.Builder(false);
  }

  /**
   * Insert a single-element container.
   * <p>
   * Insert a container into your app that can be used to hold a single element.
   * This allows you to, for example, remove elements at any point, or replace
   * several elements at once (using a child multi-element container).
   * <p>
   * To insert/replace/clear an element on the returned container:
   * 
   * <pre>
   * {@code
   * var container = Jt.empty("empty-1").use();
   * Jt.yourElement().use(container);
   * }
   * </pre>
   * 
   * See examples below.
   * <p>
   * Examples:
   * Dynamic content replacement
   * {@snippet file = "EmptyApp.java" appUrl =
   * "https://javelit-container-cp9j-production.up.railway.app" appHeight = "400"}
   * <p>
   * Simple animations
   * {@snippet file = "AnimationEmptyApp.java" appUrl =
   * "https://javelit-container-production.up.railway.app" appHeight = "300"}
   *
   */
  public static ContainerComponent.Builder empty() {
    return new ContainerComponent.Builder(true);
  }

  /**
   * Insert containers laid out as side-by-side columns.
   * <p>
   * Inserts a number of multi-element containers laid out side-by-side and
   * returns a list of container objects.
   * <p>
   * To add elements to the returned columns container:
   * 
   * <pre>
   * {@code
   * var cols = Jt.columns("my-3-cols", 3).use();
   * Jt.yourElement().use(cols.col(1));
   * Jt.yourElement().use(cols.col(0));
   * Jt.yourElement().use(cols.col(2));
   * }
   * </pre>
   * 
   * See examples below.
   * <p>
   * Examples:
   * Basic three-column layout with headers and content
   * {@snippet file = "ColumnsApp.java" appUrl =
   * "https://javelit-container-tnzd-production.up.railway.app" appHeight = "300"}
   *
   * @param numColumns The number of columns to create
   */
  public static ColumnsComponent.Builder columns(final int numColumns) {
    return new ColumnsComponent.Builder(numColumns);
  }

  /**
   * Insert containers separated into tabs.
   * <p>
   * Inserts a number of multi-element containers as tabs.
   * Tabs are a navigational element that allows users to easily move between
   * groups of related content.
   * <p>
   * To add elements to the returned tabs container:
   * 
   * <pre>
   * {@code
   * var tabs = Jt.tabs("my-tabs", List.of("E-commerce", "Industry", "Finance")).use();
   * // get tab by name
   * Jt.yourElement().use(tabs.tab("E-commerce"));
   * // get tab by index
   * Jt.yourElement().use(tabs.tab(2));
   * }
   * </pre>
   * 
   * See examples below.
   * <p>
   * Examples:
   * Basic tabbed interface
   * {@snippet file = "TabsApp.java" appUrl =
   * "https://javelit-container-ebco-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Data analysis tabs
   * {@snippet file = "DataTabsApp.java" appUrl =
   * "https://javelit-container-7zqb-production.up.railway.app" appHeight = "300"}
   *
   * @param tabs A list of tab labels
   */
  public static TabsComponent.Builder tabs(@Nonnull List<@NotNull String> tabs) {
    return new TabsComponent.Builder(tabs);
  }

  /**
   * Insert a multi-element container that can be expanded/collapsed.
   * <p>
   * Insert a container into your app that can be used to hold multiple elements
   * and can be expanded or collapsed by the user.
   * When collapsed, all that is visible is the provided label.
   * <p>
   * To add elements to the returned expander:
   * 
   * <pre>
   * {@code
   * var expander = Jt.expander("my-expander", "More details").use();
   * Jt.yourElement().use(expander);
   * }
   * </pre>
   * 
   * See examples below.
   * <p>
   * Examples:
   * Basic expander with explanation content
   * {@snippet file = "ExpanderApp.java" appUrl =
   * "https://javelit-container-b8jy-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param label The label for the expander header
   */
  public static ExpanderComponent.Builder expander(@Language("markdown") @Nonnull String label) {
    return new ExpanderComponent.Builder(label);
  }

  /**
   * Insert a popover container.
   * <p>
   * Inserts a multi-element container as a popover. It consists of a button-like
   * element and a container that opens when the button is clicked.
   * <p>
   * Opening and closing the popover will not trigger a rerun. Interacting with
   * widgets inside of an open popover will
   * rerun the app while keeping the popover open. Clicking outside of the popover
   * will close it.
   * <p>
   * To add elements to the returned popover:
   * {@snippet :
   * var popover = Jt.popover("my-popover", "Advanced configuration").use();
   * Jt.yourElement().use(popover);
   * }
   * See examples below.
   * <p>
   * Examples:
   * Settings popover
   * {@snippet file = "PopoverApp.java" appUrl =
   * "https://javelit-container-kuvn-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Help popover with information
   * {@snippet file = "HelpPopoverApp.java" appUrl =
   * "https://javelit-container-8mdm-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param label The label for the popover button. Markdown is supported, see
   *              {@link Jt#markdown(String)} for more details.
   */
  public static PopoverComponent.Builder popover(@Language("markdown") @Nonnull String label) {
    return new PopoverComponent.Builder(label);
  }

  /**
   * Create a form that batches elements together with a 'Submit' button.
   * <p>
   * A form is a container that visually groups other elements and widgets
   * together, and contains a Submit button.
   * When the form's Submit button is pressed, all widget values inside the form
   * will be sent to Javelit in a batch.
   * <p>
   * To add elements to the form:
   * 
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
   * <li>Every form must contain a {@code Jt.formSubmitButton)}</li>
   * <li>{@code Jt.button} and {@code Jt.downloadButton} cannot be added to a
   * form</li>
   * <li>Forms can appear anywhere in your app (sidebar, columns, etc), but they
   * cannot be embedded inside other forms</li>
   * <li>Within a form, the only widget that can have a callback function is
   * {@code Jt.formSubmitButton)}</li>
   * </ul>
   * <p>
   * Examples:
   * User registration form
   * {@snippet file = "FormApp.java" appUrl =
   * "https://javelit-container-ovom-production.up.railway.app/" appHeight =
   * "510"}
   * <p>
   * Survey form
   * {@snippet file = "SurveyFormApp.java" appUrl =
   * "https://javelit-container-isdo-production.up.railway.app/" appHeight =
   * "550"}
   *
   */
  public static FormComponent.Builder form() {
    return new FormComponent.Builder();
  }

  /**
   * Display a form submit button.
   * <p>
   * When clicked, all widget values inside the form will be sent from the user's
   * browser to the Javelit server in a batch.
   * <p>
   * Every form must have at least one {@code Jt.formSubmitButton}. A
   * {@code Jt.formSubmitButton} cannot exist outside a form.
   * <p>
   * Examples:
   * Basic form submit button
   * {@snippet file = "FormSubmitApp.java" appUrl =
   * "https://javelit-container-bq0o-production.up.railway.app/" appHeight =
   * "450"}
   * <p>
   * Multiple submit buttons in same form
   * {@snippet file = "MultiSubmitApp.java" appUrl =
   * "https://javelit-container-wdwc-production.up.railway.app/" appHeight =
   * "490"}
   *
   * @param label The text to display on the submit button
   */
  public static FormSubmitButtonComponent.Builder formSubmitButton(@Language("markdown") final @Nonnull String label) {
    return new FormSubmitButtonComponent.Builder(label);
  }

  /**
   * Display a single-line text input widget.
   * <p>
   * Examples:
   * Simple text input
   * {@snippet file = "TextInputApp.java" appUrl =
   * "https://javelit-container-lyjk-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Text input with validation
   * {@snippet file = "ValidatedTextInputApp.java" appUrl =
   * "https://javelit-container-fh90-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param label A short label explaining to the user what this input is for.
   *              Markdown is supported, see {@link Jt#markdown(String)} for more
   *              details.
   */
  public static TextInputComponent.Builder textInput(@Language("markdown") final @Nonnull String label) {
    return new TextInputComponent.Builder(label);
  }

  /**
   * Display a multi-line text input widget.
   * <p>
   * Examples:
   * Simple text area
   * {@snippet file = "TextAreaApp.java" appUrl =
   * "https://javelit-container-oyie-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Text area for code input
   * {@snippet file = "CodeTextAreaApp.java" appUrl =
   * "https://javelit-container-e8he-production.up.railway.app" appHeight = "500"}
   *
   * @param label A short label explaining to the user what this input is for.
   *              Markdown is supported, see {@link Jt#markdown(String)} for more
   *              details.
   */
  public static TextAreaComponent.Builder textArea(@Language("markdown") final @Nonnull String label) {
    return new TextAreaComponent.Builder(label);
  }

  /**
   * Display a date input widget that can be configured to accept a single date or
   * a date range.
   * <p>
   * Examples:
   * Simple date input
   * {@snippet file = "DateInputApp.java" appUrl =
   * "https://javelit-container-hiol-production.up.railway.app" appHeight = "300"}
   *
   * @param label A short label explaining to the user what this date input is
   *              for. Markdown is supported, see {@link Jt#markdown(String)} for
   *              more details.
   */
  public static DateInputComponent.Builder dateInput(@Language("markdown") final @Nonnull String label) {
    return new DateInputComponent.Builder(label);
  }

  /**
   * Display a numeric input widget.
   * <p>
   * Examples:
   * Simple number input
   * {@snippet file = "NumberInputApp.java" appUrl =
   * "https://javelit-container-yrri-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param label A short label explaining to the user what this numeric input is
   *              for. Markdown is supported, see {@link Jt#markdown(String)} for
   *              more details.
   */
  public static NumberInputComponent.Builder<Number> numberInput(@Language("markdown") final @Nonnull String label) {
    return new NumberInputComponent.Builder<>(label);
  }

  /**
   * Display a numeric input widget.
   * <p>
   * Examples:
   * Integer input with specific type
   * {@snippet file = "TypedNumberInputApp.java" appUrl =
   * "https://javelit-container-qcrg-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param label      A short label explaining to the user what this numeric
   *                   input is for. Markdown is supported, see
   *                   {@link Jt#markdown(String)} for more details.
   * @param valueClass The number type class (Integer, Double, Float, etc.)
   */
  public static <T extends Number> NumberInputComponent.Builder<T> numberInput(
      @Language("markdown") final @Nonnull String label,
      final Class<T> valueClass) {
    return new NumberInputComponent.Builder<>(label, valueClass);
  }

  /**
   * Display a radio button widget.
   * <p>
   * Examples:
   * Simple radio selection
   * {@snippet file = "RadioApp.java" appUrl =
   * "https://javelit-container-jo9r-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Radio with custom objects
   * {@snippet file = "ProductRadioApp.java" appUrl =
   * "https://javelit-container-hh-u-production.up.railway.app/" appHeight =
   * "350"}
   *
   * @param label   A short label explaining to the user what this radio selection
   *                is for. Markdown is supported, see {@link Jt#markdown(String)}
   *                for more details.
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
   * {@snippet file = "SelectBoxApp.java" appUrl =
   * "https://javelit-container-ydef-production.up.railway.app/" appHeight =
   * "300"}
   * <p>
   * Dropdown with default value
   * {@snippet file = "ProcessingSelectBoxApp.java" appUrl =
   * "https://javelit-container-vyft-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param label   A short label explaining to the user what this selection is
   *                for. Markdown is supported, see {@link Jt#markdown(String)}
   *                for more details.
   * @param options The list of options to choose from
   * @param <T>     The type of the options
   */
  public static <T> SelectBoxComponent.Builder<T> selectbox(@Language("markdown") final @Nonnull String label,
      final @Nonnull List<T> options) {
    return new SelectBoxComponent.Builder<>(label, options);
  }

  public static JtPage.Builder page(final @Nonnull Class<?> pageApp) {
    final String simpleName = pageApp.getSimpleName();
    throw new RuntimeException("""
        Sorry, this method is not supported anymore. Use page(String path, JtRunnable page) instead. \s
        To quickfix: replace `Jt.page(%s.class)` with `Jt.page("/%s", %s::main)`
        """.formatted(simpleName, pageApp.getSimpleName(), pageApp.getSimpleName()));
  }

  /**
   * Create a page for {@code Jt.navigation} in a multipage app.
   * <p>
   * Examples:
   * Basic page creation with custom title and icon
   * {@snippet file = "NavigationApp.java" appUrl =
   * "https://javelit-container-ygun-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param path The url path where the page should be found
   * @param page The page app logic
   */
  public static JtPage.Builder page(final @Nonnull String path, final @Nonnull JtRunnable page) {
    return JtPage.builder(path, page);
  }

  /**
   * Create a navigation component with multiple pages to create a multipage app.
   * <p>
   * Call {@code Jt.navigation} in your entrypoint app class to define the
   * available pages in your app.
   * {@code Jt.navigation} use() returns the current page.
   * <p>
   * When using {@code Jt.navigation}, your entrypoint app class acts like a frame
   * of common elements around each of your pages.
   * <p>
   * The set of available pages can be updated with each rerun for dynamic
   * navigation.
   * By default, {@code Jt.navigation} displays the available pages in the sidebar
   * if there is more than one page.
   * This behavior can be changed using the {@code position} builder method.
   * <p>
   * Examples:
   * Basic multipage navigation setup
   * {@snippet file = "NavigationApp.java" appUrl =
   * "https://javelit-container-ygun-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param pages The pages to include in the navigation
   */
  public static NavigationComponent.Builder navigation(final JtPage.Builder... pages) {
    return new NavigationComponent.Builder(pages);
  }

  /**
   * Display a link to another page in a multipage app or to an external page.
   * <p>
   * If another page in a multipage app is specified, clicking the
   * {@code Jt.pageLink} element stops the current page execution
   * and runs the specified page as if the user clicked on it in the sidebar
   * navigation.
   * <p>
   * If an external page is specified, clicking the {@code Jt.pageLink} element
   * opens a new tab to the specified page.
   * The current script run will continue if not complete.
   * <p>
   * Examples:
   * A multipage app with the sidebar hidden.
   * A footer replaces the sidebar. The footer contains links to all pages of the
   * app and an external link.
   * {@snippet file = "PageLinkApp.java" appUrl =
   * "https://javelit-container-hgr7-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param pagePath The path of the page to link to in a multipage app. If null,
   *                 target the home page.
   */
  public static PageLinkComponent.Builder pageLink(final @jakarta.annotation.Nullable String pagePath) {
    return new PageLinkComponent.Builder(pagePath);
  }

  /**
   * Display a link to another page in a multipage app or to an external page.
   * <p>
   * If another page in a multipage app is specified, clicking the
   * {@code Jt.pageLink} element stops the current page execution
   * and runs the specified page as if the user clicked on it in the sidebar
   * navigation.
   * <p>
   * If an external page is specified, clicking the {@code Jt.pageLink} element
   * opens a new tab to the specified page.
   * The current script run will continue if not complete.
   *
   * @param url   The URL to link to
   * @param label The text to display for the link. Markdown is supported, see
   *              {@link Jt#markdown(String)} for more details.
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
   * {@snippet file = "FileUploadApp.java" appUrl =
   * "https://javelit-container-ala-production.up.railway.app/" appHeight = "300"}
   *
   * @param label A short label explaining to the user what this file uploader is
   *              for. Markdown is supported, see {@link Jt#markdown(String)} for
   *              more details.
   */
  public static FileUploaderComponent.Builder fileUploader(@Language("markdown") final @Nonnull String label) {
    return new FileUploaderComponent.Builder(label);
  }

  /**
   * Display a chart using ECharts library.
   * See <a href="https://echarts.icepear.org/" target="_blank">echarts-java
   * documentation</a> for more info.
   * <p>
   * Examples:
   * Plot from a {@code Chart} ({@code Bar} extends {@code Chart}).
   * {@snippet file = "BarChartApp.java" appUrl =
   * "https://javelit-container-8pdg-production.up.railway.app/" appHeight =
   * "500"}
   *
   * @param chart The ECharts {@code Chart} object to display
   */
  public static EchartsComponent.Builder echarts(final @Nonnull Chart<?, ?> chart) {
    return new EchartsComponent.Builder(chart);
  }

  /**
   * Display a chart using ECharts library.
   * See <a href="https://echarts.icepear.org/" target="_blank">echarts-java
   * documentation</a> for more info.
   * <p>
   * Examples:
   * Plot from an {@code Option}.
   * {@snippet file = "OptionChartApp.java" appUrl =
   * "https://javelit-container-hqjs-production.up.railway.app/" appHeight =
   * "500"}
   *
   * @param chartOption The ECharts {@code Option} object to display
   */
  public static EchartsComponent.Builder echarts(final @Nonnull Option chartOption) {
    return new EchartsComponent.Builder(chartOption);
  }

  /**
   * Display a chart using ECharts library.
   * See <a href="https://echarts.icepear.org/" target="_blank">echarts-java
   * documentation</a> for more info.
   * <p>
   * Examples:
   * Plot from a JSON {@code String}
   * {@snippet file = "OptionJsonChartApp.java" appUrl =
   * "https://javelit-container-n71a-production.up.railway.app/" appHeight =
   * "500"}
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
   * {@snippet file = "TableApp.java" appUrl =
   * "https://javelit-container-m0nf-production.up.railway.app/" appHeight =
   * "400"}
   *
   * @param rows The list of objects representing table rows
   */
  public static <E> TableComponent.Builder table(final @Nonnull List<E> rows) {
    return TableComponent.Builder.ofObjsList((List) rows);
  }

  /**
   * Display a static table.
   *
   * @param dataframe A tablesaw Table instance. tablesaw is an optional
   *                  dependency, so this method is not typed.
   */
  public static TableComponent.Builder table(final @Nonnull Object dataframe) {
    try {
      if (dataframe instanceof tech.tablesaw.api.Table t) {
        final Map<String, Object[]> cols = TablesawUtils.toColumnArrays(t);
        return tableFromArrayColumns(cols);
      }
    } catch (NoClassDefFoundError e) {
      // TODO CYRIL add bom explanation once implemented - this error should only
      // happen in embedded mode
      throw new RuntimeException(
          "Could not load optional tablesaw dependency. If you wish to create tables from dataframe, make sure tablesaw is available in the classpath.",
          e);
    }
    throw new IllegalArgumentException(
        "Invalid dataframe type. dataframe has class %s. Supported dataframe class is: %s"
            .formatted(dataframe.getClass(), tech.tablesaw.api.Table.class));
  }

  /**
   * Display a static table.
   * <p>
   * Examples:
   * Basic table with array of objects
   * {@snippet file = "TableArrayApp.java" appUrl =
   * "https://javelit-container-9siz-production.up.railway.app/" appHeight =
   * "400"}
   *
   * @param rows The array of objects representing table rows
   */
  public static <E> TableComponent.Builder table(final @Nonnull E[] rows) {
    return TableComponent.Builder.ofObjsArray(rows);
  }

  /**
   * Display a static table.
   * <p>
   * Examples:
   * Table from column arrays
   * {@snippet file = "TableColumnsArrayApp.java" appUrl =
   * "https://javelit-container-j1rl-production.up.railway.app/" appHeight =
   * "400"}
   *
   * @param cols A map where keys are column names and values are arrays of column
   *             data
   */
  public static <E> TableComponent.Builder tableFromArrayColumns(
      final @Nonnull Map<@NotNull String, @NotNull E[]> cols) {
    return TableComponent.Builder.ofColumnsArrays((Map) cols);
  }

  /**
   * Display a static table.
   * <p>
   * Examples:
   * Table from column lists
   * {@snippet file = "TableColumnsListApp.java" appUrl =
   * "https://javelit-container-21w6-production.up.railway.app/" appHeight =
   * "400"}
   *
   * @param cols A map where keys are column names and values are collections of
   *             column data
   */
  public static TableComponent.Builder tableFromListColumns(
      final @Nonnull Map<@NotNull String, @NotNull List<Object>> cols) {
    return TableComponent.Builder.ofColumnsLists(cols);
  }

  /**
   * Programmatically switch the current page in a multipage app.
   * <p>
   * When {@code Jt.switchPage} is called, the current page execution stops and
   * the specified page runs as if the
   * user clicked on it in the sidebar navigation. The specified page must be
   * recognized by Javelit's multipage
   * architecture (your main app class or an app class in the available pages).
   * <p>
   * Examples:
   * Conditional page switching with checkboxes
   * {@snippet file = "SwitchPageApp.java" appUrl =
   * "https://javelit-container-wyfi-production.up.railway.app/" appHeight =
   * "400"}
   *
   * @param path The target page path. If {@code null}, target the home page.
   */
  public static void switchPage(final @jakarta.annotation.Nullable String path) {
    // note: the design here is pretty hacky
    final NavigationComponent nav = StateManager.getNavigationComponent();
    checkState(nav != null,
        "No navigation component found in app. switchPage only works with multipage app. Make sure switchPage is called after Jt.navigation().[...].use().");
    final JtPage newPage = nav.getPageFor(path);
    checkArgument(newPage != null, "Invalid page %s. This page is not registered in Jt.navigation().", path);
    final UrlContext urlContext = new UrlContext(newPage.urlPath(),
        Map.of());
    throw new BreakAndReloadAppException(sessionId -> StateManager.setUrlContext(sessionId, urlContext));
  }

  /**
   * Display an audio player.
   * <p>
   * Javelit attempts to infer the format (MIME type) from the input. If format
   * inference fails, passing the format
   * directly with {@code .format()} is necessary.
   * <p>
   * Examples:
   * Audio from external URL
   * {@snippet file = "UrlAudioApp.java" appUrl =
   * "https://javelit-container-agf8-production.up.railway.app/" appHeight =
   * "300"}
   * Audio from static resource
   * {@snippet file = "staticUrlAudioApp/StaticUrlAudioApp.java" appUrl =
   * "https://javelit-container-o7n6-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param url A URL for a hosted audio file.
   */
  public static AudioComponent.Builder audio(final @Nonnull String url) {
    return new AudioComponent.Builder(url);
  }

  /**
   * Display a widget that returns an audio recording from the user's microphone.
   * <p>
   * Examples:
   * Record a voice message and play it back.
   * The default sample rate of 16000 Hz is optimal for speech recognition.
   * {@snippet file = "AudioInputApp.java" appUrl =
   * "https://javelit-container-oz84-production.up.railway.app" appHeight = "400"}
   * Record high-fidelity audio and play it back. Higher sample rates can create
   * higher-quality, larger audio files.
   * This might require a nicer microphone to fully appreciate the difference.
   * {@snippet file = "HighQualityAudioInputApp.java" appUrl =
   * "https://javelit-container-1en5-production.up.railway.app/" appHeight =
   * "400"}
   *
   * @param label A short label explaining to the user what this audio input
   *              widget is for. Markdown is supported, see
   *              {@link Jt#markdown(String)} for more details.
   */
  public static AudioInputComponent.Builder audioInput(final @Nonnull String label) {
    return new AudioInputComponent.Builder(label);
  }

  /**
   * Display an audio player.
   * <p>
   * Javelit attempts to infer the format (MIME type) from the input. If format
   * inference fails, passing the format
   * directly with {@code .format()} is necessary.
   * <p>
   * Examples:
   * Audio from raw data
   * {@snippet file = "RawDataAudioApp.java" appUrl =
   * "https://javelit-container-xnj4-production.up.railway.app/" appHeight =
   * "300"}
   *
   * @param data Raw audio data.
   */
  public static AudioComponent.Builder audio(final @Nonnull byte[] data) {
    return new AudioComponent.Builder(data);
  }

  /**
   * Display an audio player.
   * <p>
   * Javelit attempts to infer the format (MIME type) from the input. If format
   * inference fails, passing the format
   * directly with {@code .format()} is necessary.
   * <p>
   * Examples:
   * Audio from local file
   * {@snippet file = "FileAudioApp.java"}
   *
   * @param filePath A path to a local audio file. The path can be absolute or
   *                 relative to the working directory.
   */
  public static AudioComponent.Builder audio(final @Nonnull Path filePath) {
    return new AudioComponent.Builder(filePath);
  }

  /**
   * Display an audio player.
   * <p>
   * Javelit attempts to infer the format (MIME type) from the input. If format
   * inference fails, passing the format
   * directly with {@code .format()} is necessary.
   *
   * @param uploadedFile An uploaded file.
   */
  // TODO add an example once audio input is implemented
  public static AudioComponent.Builder audio(final @Nonnull JtUploadedFile uploadedFile) {
    return AudioComponent.Builder.of(uploadedFile);
  }

  /**
   * Display an image.
   * <p>
   * Examples:
   * Image from external URL
   * {@snippet file = "UrlImageApp.java" appUrl =
   * "https://javelit-container-sagw-production.up.railway.app/" appHeight =
   * "700"}
   * Image from static resource
   * {@snippet file = "staticImageApp/StaticImageApp.java" appUrl =
   * "https://javelit-container-o6qa-production.up.railway.app/" appHeight =
   * "700"}
   *
   * @param url A URL for a hosted image file.
   */
  public static ImageComponent.Builder image(final @Nonnull String url) {
    return ImageComponent.Builder.of(url);
  }

  /**
   * Display an image.
   * <p>
   * Examples:
   * Image from raw data
   * {@snippet file = "ByteImageApp.java" appUrl =
   * "https://javelit-container-jabg-production.up.railway.app" appHeight = "550"}
   *
   * @param data Raw image data.
   */
  public static ImageComponent.Builder image(final @Nonnull byte[] data) {
    return ImageComponent.Builder.of(data);
  }

  /**
   * Display an image.
   * <p>
   * Examples:
   * Image from local file
   * {@snippet file = "FileImageApp.java"}
   *
   * @param filePath A path to a local image file. The path can be absolute or
   *                 relative to the working directory.
   */
  public static ImageComponent.Builder image(final @Nonnull Path filePath) {
    return ImageComponent.Builder.of(filePath);
  }

  /**
   * Display an image.
   *
   * @param uploadedFile An uploaded file.
   */
  // TODO add an example once image input is implemented
  public static ImageComponent.Builder image(final @Nonnull JtUploadedFile uploadedFile) {
    return ImageComponent.Builder.of(uploadedFile);
  }

  /**
   * Display an image.
   * <p>
   * Examples:
   * From an SVG image
   * {@snippet file = "SvgImageApp.java" appUrl =
   * "https://javelit-container-kkah-production.up.railway.app/" appHeight =
   * "400"}
   */
  public static ImageComponent.Builder imageFromSvg(final @Language("html") @Nonnull String svg) {
    return ImageComponent.Builder.ofSvg(svg);
  }

  /**
   * Display an image from base64-encoded data.
   * <p>
   * This is useful when working with Gen AI services that return images as base64
   * strings.
   * <p>
   * Examples:
   * From a base64-encoded image
   * {@snippet file = "Base64ImageApp.java" appUrl = "TODO" appHeight = "400"}
   *
   * @param base64Img A base64-encoded image string. May include the data URI
   *                  prefix
   *                  (e.g., "data:image/png;base64,...") or just the raw base64
   *                  data.
   */
  public static ImageComponent.Builder imageFromBase64(final @Nonnull String base64Img) {
    return ImageComponent.Builder.ofBase64(base64Img);
  }

  /**
   * Display a PDF viewer.
   * <p>
   * Examples:
   * Display PDF from URL
   * {@snippet file = "PdfApp.java" appUrl =
   * "https://javelit-container-zhd-production.up.railway.app/" appHeight = "700"}
   *
   * @param url A URL for a hosted PDF, or a path to a PDF in the static folder.
   */
  public static PdfComponent.Builder pdf(final @Nonnull String url) {
    return PdfComponent.Builder.of(url);
  }

  /**
   * Display a PDF viewer.
   * <p>
   * Examples:
   * PDF from raw data
   * {@snippet file = "BytePdfApp.java"}
   *
   * @param data Raw PDF data.
   */
  public static PdfComponent.Builder pdf(final @Nonnull byte[] data) {
    return PdfComponent.Builder.of(data);
  }

  /**
   * Display a PDF viewer.
   * <p>
   * Examples:
   * PDF from local file
   * {@snippet file = "FilePdfApp.java"}
   *
   * @param filePath A path to a local PDF file. The path can be absolute or
   *                 relative to the working directory.
   */
  public static PdfComponent.Builder pdf(final @Nonnull Path filePath) {
    return PdfComponent.Builder.of(filePath);
  }

  /**
   * Display a PDF from an uploaded file.
   *
   * @param uploadedFile An uploaded PDF file from {@link #fileUploader}
   */
  public static PdfComponent.Builder pdf(final @Nonnull JtUploadedFile uploadedFile) {
    return PdfComponent.Builder.of(uploadedFile);
  }

  /**
   * Rerun the script immediately.
   * <p>
   * When {@code Jt.rerun()} is called, Javelit halts the current app run and
   * executes no further statements. Javelit immediately
   * queues the script to rerun. In a multipage app: by default, the rerun is for
   * the same url path (same page). If the rerun could make
   * the current page unavailable, pass {@code toHome = true} to send back to the
   * home url and avoid 404 errors.
   * <p>
   * Examples:
   * Updating session state and triggering rerun
   * {@snippet file = "RerunApp.java" appUrl =
   * "https://javelit-container-aopo-production.up.railway.app/" appHeight =
   * "400"}
   *
   * @param toHome If {@code true}, rerun in {@code /} url path. If {@code false},
   *               rerun in current path.
   */
  public static void rerun(final boolean toHome) {
    if (toHome) {
      final UrlContext urlContext = new UrlContext("/", Map.of());
      throw new BreakAndReloadAppException(sessionId -> StateManager.setUrlContext(sessionId, urlContext));
    } else {
      throw new BreakAndReloadAppException(null);
    }
  }

  /**
   * Rerun the script immediately.
   * <p>
   * When {@code Jt.rerun()} is called, Javelit halts the current app run and
   * executes no further statements. Javelit immediately
   * queues the script to rerun. In a multipage app: by default, the rerun is for
   * the same url path (same page). If the rerun could make
   * the current page unavailable, pass {@code toHome = true} to send back to the
   * home url and avoid 404 errors.
   */
  public static void rerun() {
    Jt.rerun(false);
  }

  private Jt() {
  }

}
