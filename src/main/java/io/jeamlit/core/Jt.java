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
import jakarta.annotation.Nonnull;
import org.icepear.echarts.Chart;
import org.icepear.echarts.Option;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import static io.jeamlit.core.utils.Preconditions.checkArgument;
import static io.jeamlit.core.utils.Preconditions.checkState;

// main interface for developers - should only contain functions of the public API.

/**
 * The main entrypoint for app creators.
 * Add elements with Jt.title(...).use(), Jt.button(...).use(), etc...
 * Get the session state with Jt.sessionState().
 * Get the app cache Jt.cache().
 * Perform a deep copy with Jt.deepCopy(someObject).
 */
public final class Jt {

    public static TypedMap sessionState() {
        final InternalSessionState session = StateManager.getCurrentSession();
        return new TypedMap(session.getUserState());
    }

    public static TypedMap componentsState() {
        final InternalSessionState session = StateManager.getCurrentSession();
        // NOTE: best would be to have a deep-copy-on-read map
        // here it's the responsibility of the user to not play around with the values inside this map
        return new TypedMap(Map.copyOf(session.getComponentsState()));
    }

    /**
     * Returns the app cache.
     * See https://docs.streamlit.io/get-started/fundamentals/advanced-concepts#caching
     */
    public static TypedMap cache() {
        return StateManager.getCache();
    }

    public static String urlPath() {
        return StateManager.getUrlContext().currentPath();
    }

    // TODO consider adding a TypedMap interface + getOne to unwrap the list
    public static Map<String, List<String>> urlQueryParameters() {
        return StateManager.getUrlContext().queryParameters();
    }

    /**
     * Slow deep copy utility: serialize then deserialize json.
     * Made available to be able to implement the behaviour of st.cache_data that does a copy on read.
     * https://docs.streamlit.io/get-started/fundamentals/advanced-concepts#caching
     *
     * @return a deep copy of the provided object.
     * TODO add example usage for typeRef
     */
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
     *     <li>Emoji shortcodes, such as :+1: and :sunglasses:. For a list of all supported codes, see <a href="https://www.webfx.com/tools/emoji-cheat-sheet/">https://www.webfx.com/tools/emoji-cheat-sheet/</a>.</li>
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

    public static MarkdownComponent.Builder divider() {
        return new MarkdownComponent.Builder("---");
    }

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
     * @param body  The HTML code to insert.
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

    public static CodeComponent.Builder code(final @Nonnull String body) {
        return new CodeComponent.Builder(body);
    }

    public static ButtonComponent.Builder button(@Language("markdown") final @Nonnull String label) {
        return new ButtonComponent.Builder(label);
    }

    public static CheckboxComponent.Builder checkbox(@Language("markdown") final @Nonnull String label) {
        return new CheckboxComponent.Builder(label);
    }

    public static ToggleComponent.Builder toggle(@Language("markdown") final @Nonnull String label) {
        return new ToggleComponent.Builder(label);
    }

    public static SliderComponent.Builder slider(@Language("markdown") final @Nonnull String label) {
        return new SliderComponent.Builder(label);
    }

    public static ContainerComponent.Builder container(final @Nonnull String key) {
        return new ContainerComponent.Builder(key, false);
    }

    public static ContainerComponent.Builder empty(final @Nonnull String key) {
        return new ContainerComponent.Builder(key, true);
    }

    public static ColumnsComponent.Builder columns(final @Nonnull String key, final int numColumns) {
        return new ColumnsComponent.Builder(key, numColumns);
    }

    public static TabsComponent.Builder tabs(final @Nonnull String key, @Nonnull List<@NotNull String> tabs) {
        return new TabsComponent.Builder(key, tabs);
    }

    public static ExpanderComponent.Builder expander(final @Nonnull String key, @Nonnull String label) {
        return new ExpanderComponent.Builder(key, label);
    }

    public static PopoverComponent.Builder popover(final @Nonnull String key,
                                                   @Language("markdown") @Nonnull String label) {
        return new PopoverComponent.Builder(key, label);
    }

    public static FormComponent.Builder form(final @Nonnull String key) {
        return new FormComponent.Builder(key);
    }

    public static FormSubmitButtonComponent.Builder formSubmitButton(final @Nonnull String label) {
        return new FormSubmitButtonComponent.Builder(label);
    }

    public static TextInputComponent.Builder textInput(@Language("markdown") final @Nonnull String label) {
        return new TextInputComponent.Builder(label);
    }

    public static TextAreaComponent.Builder textArea(@Language("markdown") final @Nonnull String label) {
        return new TextAreaComponent.Builder(label);
    }

    public static DateInputComponent.Builder dateInput(@Language("markdown") final @Nonnull String label) {
        return new DateInputComponent.Builder(label);
    }

    public static NumberInputComponent.Builder<Number> numberInput(@Language("markdown") final @Nonnull String label) {
        return new NumberInputComponent.Builder<>(label);
    }

    public static <T extends Number> NumberInputComponent.Builder<T> numberInput(@Language("markdown") final @Nonnull String label,
                                                                                 final Class<T> valueClass) {
        return new NumberInputComponent.Builder<>(label, valueClass);
    }

    public static <T> RadioComponent.Builder<T> radio(@Language("markdown") final @Nonnull String label,
                                                      final @Nonnull List<T> options) {
        return new RadioComponent.Builder<>(label, options);
    }

    public static <T> SelectBoxComponent.Builder<T> selectBox(@Language("markdown") final @Nonnull String label,
                                                              final @Nonnull List<T> options) {
        return new SelectBoxComponent.Builder<>(label, options);
    }

    public static JtPage.Builder page(final @Nonnull Class<?> pageApp) {
        return new JtPage.Builder(pageApp);
    }

    public static NavigationComponent.Builder navigation(final JtPage.Builder... pages) {
        return new NavigationComponent.Builder(pages);
    }

    public static PageLinkComponent.Builder pageLink(final @Nonnull Class<?> pageClass) {
        return new PageLinkComponent.Builder(pageClass);
    }

    public static PageLinkComponent.Builder pageLink(final @Nonnull String url,
                                                     final @Language("markdown") @Nonnull String label) {
        return new PageLinkComponent.Builder(url, label);
    }

    public static FileUploaderComponent.Builder fileUploader(@Language("markdown") final @Nonnull String label) {
        return new FileUploaderComponent.Builder(label);
    }

    public static EchartsComponent.Builder echarts(final @Nonnull Chart<?, ?> chart) {
        return new EchartsComponent.Builder(chart);
    }

    public static EchartsComponent.Builder echarts(final @Nonnull Option chartOption) {
        return new EchartsComponent.Builder(chartOption);
    }

    public static EchartsComponent.Builder echarts(final @Language("json") String chartOptionJson) {
        return new EchartsComponent.Builder(chartOptionJson);
    }

    public static TableComponent.Builder table(final @Nonnull List<Object> rows) {
        return TableComponent.Builder.ofObjsList(rows);
    }

    public static TableComponent.Builder table(final @Nonnull Object[] rows) {
        return TableComponent.Builder.ofObjsArray(rows);
    }

    public static TableComponent.Builder tableFromArrayColumns(final @Nonnull Map<@NotNull String, @NotNull Object[]> cols) {
        return TableComponent.Builder.ofColumnsArrays(cols);
    }

    public static <Values extends @NotNull SequencedCollection<@Nullable Object>> TableComponent.Builder tableFromListColumns(
            final @Nonnull Map<@NotNull String, Values> cols) {
        return TableComponent.Builder.ofColumnsLists(cols);
    }

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
