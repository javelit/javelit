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
 *
 * <pre>{@code
 * public class MyApp {
 *     public static void main(String[] args) {
 *         Jt.title("Welcome").use();
 *         String name = Jt.textInput("Enter your name").use();
 *         if (Jt.button("Submit").use()) {
 *             Jt.text("Hello, " + name).use();
 *         }
 *     }
 * }
 * }</pre>
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
     */
    public static TypedMap cache() {
        return StateManager.getCache();
    }


    /**
     * Return the current url path.
     * <p>
     * May be used for multipage apps.
     * In a single page app, will always return {@code "/"}.
     */
    public static String urlPath() {
        return StateManager.getUrlContext().currentPath();
    }

    /**
     * Return the current query parameters as a map.
     * <p>
     * For instance: {@code ?key1=foo&key2=bar&key2=fizz} will return
     * {"key1": ["foo"], "key2": ["bar", "fizz"]}
     */
    // TODO consider adding a TypedMap interface with list unwrap
    public static Map<String, List<String>> urlQueryParameters() {
        return StateManager.getUrlContext().queryParameters();
    }

    /**
     * Return a deep copy of the provied object.
     * <p>
     * Utility that may be useful in combination with the cache, to implement a copy on read behavior.
     * For instance, you can get a value that is expensive to
     * instantiate from the cache, but perform a deep copy to prevent mutations and side effects across sessions.
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
     *
     * @param body The string to display.
     */
    public static TextComponent.Builder text(final @Nonnull String body) {
        return new TextComponent.Builder(body);
    }

    /**
     * Display text in title formatting.
     * Each document should have a single st.title(), although this is not enforced.
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
     *
     * @param body The text to display as Markdown.
     */
    public static MarkdownComponent.Builder markdown(final @Nonnull @Language("markdown") String body) {
        return new MarkdownComponent.Builder(body);
    }

    /**
     * Display a horizontal rule.
     */
    public static MarkdownComponent.Builder divider() {
        return new MarkdownComponent.Builder("---");
    }

    /**
     * Display error message.
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
     *
     * @param filePath The path of the file containing the HTML code to insert.
     */
    public static HtmlComponent.Builder html(final @Nonnull Path filePath) {
        return new HtmlComponent.Builder(filePath);
    }

    /**
     * Display a code block with optional syntax highlighting.
     *
     * @param body The string to display as code or monospace text.
     */
    public static CodeComponent.Builder code(final @Nonnull String body) {
        return new CodeComponent.Builder(body);
    }

    /**
     * Display a button widget.
     *
     * @param label A short label explaining to the user what this button is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static ButtonComponent.Builder button(@Language("markdown") final @Nonnull String label) {
        return new ButtonComponent.Builder(label);
    }

    /**
     * Display a checkbox widget.
     *
     * @param label A short label explaining to the user what this checkbox is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static CheckboxComponent.Builder checkbox(@Language("markdown") final @Nonnull String label) {
        return new CheckboxComponent.Builder(label);
    }

    /**
     * Display a toggle widget.
     *
     * @param label A short label explaining to the user what this toggle is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static ToggleComponent.Builder toggle(@Language("markdown") final @Nonnull String label) {
        return new ToggleComponent.Builder(label);
    }

    /**
     * Display a slider widget.
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
     *
     * @param label The text to display on the submit button
     */
    public static FormSubmitButtonComponent.Builder formSubmitButton(final @Nonnull String label) {
        return new FormSubmitButtonComponent.Builder(label);
    }

    /**
     * Display a single-line text input widget.
     *
     * @param label A short label explaining to the user what this input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static TextInputComponent.Builder textInput(@Language("markdown") final @Nonnull String label) {
        return new TextInputComponent.Builder(label);
    }

    /**
     * Display a multi-line text input widget.
     *
     * @param label A short label explaining to the user what this input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static TextAreaComponent.Builder textArea(@Language("markdown") final @Nonnull String label) {
        return new TextAreaComponent.Builder(label);
    }

    /**
     * Display a date input widget that can be configured to accept a single date or a date range.
     *
     * @param label A short label explaining to the user what this date input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static DateInputComponent.Builder dateInput(@Language("markdown") final @Nonnull String label) {
        return new DateInputComponent.Builder(label);
    }

    /**
     * Display a numeric input widget.
     *
     * @param label A short label explaining to the user what this numeric input is for. Markdown is supported, see {@link io.jeamlit.core.Jt#markdown(String)} for more details.
     */
    public static NumberInputComponent.Builder<Number> numberInput(@Language("markdown") final @Nonnull String label) {
        return new NumberInputComponent.Builder<>(label);
    }

    /**
     * Display a numeric input widget.
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
     * By default, {@code Jt.navogation} displays the available pages in the sidebar if there is more than one page.
     * This behavior can be changed using the {@code position} builder method.
     *
     * @param pages The pages to include in the navigation
     */
    public static NavigationComponent.Builder navigation(final JtPage.Builder... pages) {
        return new NavigationComponent.Builder(pages);
    }

    /**
     * Display a link to another page in a multipage app or to an external page.
     * <p>
     * If another page in a multipage app is specified, clicking the {Jt.pageLink} element stops the current page execution
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
     * If another page in a multipage app is specified, clicking the {Jt.pageLink} element stops the current page execution
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
