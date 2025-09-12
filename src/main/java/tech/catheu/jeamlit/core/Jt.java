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
package tech.catheu.jeamlit.core;

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
import tech.catheu.jeamlit.components.chart.EchartsComponent;
import tech.catheu.jeamlit.components.data.TableComponent;
import tech.catheu.jeamlit.components.input.ButtonComponent;
import tech.catheu.jeamlit.components.input.NumberInputComponent;
import tech.catheu.jeamlit.components.input.SliderComponent;
import tech.catheu.jeamlit.components.input.TextAreaComponent;
import tech.catheu.jeamlit.components.input.TextInputComponent;
import tech.catheu.jeamlit.components.layout.ColumnsComponent;
import tech.catheu.jeamlit.components.layout.ContainerComponent;
import tech.catheu.jeamlit.components.layout.ExpanderComponent;
import tech.catheu.jeamlit.components.layout.FormComponent;
import tech.catheu.jeamlit.components.layout.FormSubmitButtonComponent;
import tech.catheu.jeamlit.components.layout.PopoverComponent;
import tech.catheu.jeamlit.components.layout.TabsComponent;
import tech.catheu.jeamlit.components.media.FileUploaderComponent;
import tech.catheu.jeamlit.components.multipage.JtPage;
import tech.catheu.jeamlit.components.multipage.NavigationComponent;
import tech.catheu.jeamlit.components.multipage.PageLinkComponent;
import tech.catheu.jeamlit.components.status.ErrorComponent;
import tech.catheu.jeamlit.components.text.CodeComponent;
import tech.catheu.jeamlit.components.text.HtmlComponent;
import tech.catheu.jeamlit.components.text.MarkdownComponent;
import tech.catheu.jeamlit.components.text.TextComponent;
import tech.catheu.jeamlit.components.text.TitleComponent;
import tech.catheu.jeamlit.datastructure.TypedMap;

import static tech.catheu.jeamlit.core.utils.Preconditions.checkArgument;
import static tech.catheu.jeamlit.core.utils.Preconditions.checkState;

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

    // syntactic sugar for all components - 1 method per component
    // Example: Jt.use(Jt.text("my text")); is equivalent to Jt.use(new TextComponent.Builder("my text"));
    public static TextComponent.Builder text(final @Nonnull @Language("Markdown") String body) {
        return new TextComponent.Builder(body);
    }

    public static TitleComponent.Builder title(@Language("markdown") final @Nonnull String body) {
        return new TitleComponent.Builder(body);
    }

    public static MarkdownComponent.Builder markdown(final @Nonnull @Language("markdown") String body) {
        return new MarkdownComponent.Builder(body);
    }

    public static ErrorComponent.Builder error(final @Language("markdown") @Nonnull String body) {
        return new ErrorComponent.Builder(body);
    }

    public static HtmlComponent.Builder html(final @Nonnull @Language("HTML") String body) {
        return new HtmlComponent.Builder(body);
    }

    public static HtmlComponent.Builder html(final @Nonnull Path filePath) {
        return new HtmlComponent.Builder(filePath);
    }

    public static CodeComponent.Builder code(final @Nonnull String body) {
        return new CodeComponent.Builder(body);
    }

    public static ButtonComponent.Builder button(@Language("markdown") final @Nonnull String label) {
        return new ButtonComponent.Builder(label);
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

    public static PopoverComponent.Builder popover(final @Nonnull String key, @Nonnull String label) {
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

    public static NumberInputComponent.Builder<Number> numberInput(@Language("markdown") final @Nonnull String label) {
        return new NumberInputComponent.Builder<>(label);
    }

    public static <T extends Number> NumberInputComponent.Builder<T> numberInput(@Language("markdown") final @Nonnull String label, final Class<T> valueClass) {
        return new NumberInputComponent.Builder<>(label, valueClass);
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

    public static PageLinkComponent.Builder pageLink(final @Nonnull String url, final @Language("markdown") @Nonnull String label) {
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

    public static <Values extends @NotNull SequencedCollection<@Nullable Object>> TableComponent.Builder tableFromListColumns(final @Nonnull Map<@NotNull String, Values> cols) {
        return TableComponent.Builder.ofColumnsLists(cols);
    }

    public static void switchPage(final @Nonnull Class<?> pageApp) {
        // note: the design here is pretty hacky
        final NavigationComponent nav = StateManager.getNavigationComponent();
        checkState(nav != null, "No navigation component found in app. switchPage only works with multipage app. Make sure switchPage is called after Jt.navigation().[...].use().");
        final JtPage newPage = nav.getPageFor(pageApp);
        checkArgument(newPage != null, "Invalid page %s. This page is not registered in Jt.navigation().", pageApp.getName());
        final InternalSessionState.UrlContext urlContext = new InternalSessionState
                .UrlContext(newPage.urlPath(), Map.of());
        throw new BreakAndReloadAppException(sessionId -> StateManager.setUrlContext(sessionId, urlContext));
    }

    private Jt() {
    }

}
