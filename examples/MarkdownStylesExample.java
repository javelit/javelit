///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.44.0



import java.util.List;import io.jeamlit.core.Jt;

public class MarkdownStylesExample {
    public static void main(String[] args) {
        Jt.title("**Markdown Styles Test** - All Components")
          .anchor("markdown-test")
          .use();

        Jt.markdown(
                "This example demonstrates **markdown support** across all *edited components* with `shared styles`. Test **bold**, *italic*, `code`, [links](https://example.com), and ~~strikethrough~~ formatting.");

        // ===== OPTION 1 COMPONENTS (span → div wrapper) =====

        Jt.title("## Option 1 Components (Complex - div wrapper)")
          .use();

        Jt.markdown(
                "These components use **div wrappers** for `markdown-content` due to *complex layouts* with tooltips and multiple elements.");

        // Input Components
        Jt.checkbox("Enable **advanced** *features* with `config.json`")
          .help("This is a **checkbox** with *markdown* support and `inline code`")
          .use();

        Jt.toggle("**Toggle** *dark mode* with `theme.css`")
          .help("Toggle component supports **bold**, *italic*, and `code` formatting")
          .use();

        Jt
                .radio("Select **data source**:",
                       List.of("**Local** `file.csv`",
                               "*Remote* **API** endpoint",
                               "~~Legacy~~ **Database** `connection`"))
                .help("Radio buttons with **markdown** in *labels* and `code snippets`")
                .use();

        Jt.selectbox("Choose **processing** *method*:",
                     List.of("**Fast** `algorithm.py`", "*Standard* **batch** processing", "~~Slow~~ **manual** `review.txt`"))
          .help("SelectBox with **markdown** support in *options* and `code`")
          .use();

        Jt.textInput("Enter **API** *key*:")
          .placeholder("**your-api-key** from `config.json`")
          .help("Text input with **markdown** in *placeholder* and `help`")
          .use();

        Jt.textArea("**Configuration** *settings*:")
          .placeholder("Enter **JSON** config with `key: value` pairs")
          .help("TextArea supports **bold**, *italic*, and `code` in placeholder")
          .use();

        Jt.numberInput("Set **max** *connections*:")
          .minValue(1)
          .maxValue(100)
          .help("Number input with **markdown** support and `validation`")
          .use();

        Jt.slider("**Memory** *allocation* `(GB)`:")
          .min(1.0)
          .max(32.0)
          .step(0.5)
          .help("Slider with **bold** *italic* and `code` formatting")
          .use();

        Jt.fileUploader("Upload **data** *files*:")
          .type(List.of(".csv", ".json", ".txt"))
          .help("File uploader supports **markdown** with `file types`")
          .use();

        Jt.pageLink("Go to **Settings** *page*", "/settings")
          .icon("⚙️")
          .help("Page link with **markdown** and `navigation`")
          .use();

        // ===== OPTION 2 COMPONENTS (Simple - merged classes) =====

        Jt.title("## Option 2 Components (Simple - merged classes)")
          .use();

        Jt.markdown(
                "These **simple components** merge `markdown-content` class with *existing elements* for cleaner HTML structure.");

        Jt.title("### **Title** with *markdown* and `code`")
          .anchor("title-test")
          .help("Title component with **bold**, *italic*, and `inline code`")
          .use();

        Jt.button("**Submit** *form* with `validation`")
          .type("primary")
          .icon("✅")
          .help("Button with **markdown** text and `icons`")
          .use();

        Jt.button("**Cancel** ~~operation~~")
          .type("secondary")
          .help("Secondary button with **bold** and ~~strikethrough~~")
          .use();

        // ===== ALREADY CORRECT COMPONENTS =====

        Jt.title("## Already Correct Components")
          .use();

        Jt.markdown("These components were **already implemented correctly** with proper `markdown-content` usage.");

        var tabs = Jt.tabs(List.of("**Data** *Analysis*", "*Visualization* `Charts`")).use();
        Jt.markdown("Tab content with **bold** *italic* and `code` support").use(tabs.tab(0));
        Jt.markdown("Another tab with **markdown** formatting").use(tabs.tab(1));

        var exp = Jt.expander("**Show** *advanced* `options`").use();
        Jt.markdown("Expander content supports **all markdown** *formatting* including `inline code`, [links](https://example.com), and ~~strikethrough~~.").use(exp);

        var popover = Jt.popover("**Hover** for *info*")
          .use();
        Jt.text("Hello").use(popover);

        // ===== COMPREHENSIVE MARKDOWN TEST =====

        Jt.title("## Comprehensive Markdown Test")
          .use();

        Jt.markdown("""
                            **All components** now use *shared* `MARKDOWN_CSS` styles for:
                            
                            - **Bold text** with proper font weight
                            - *Italic text* with font style
                            - `Inline code` with background and padding
                            - [External links](https://streamlit.io) with proper styling
                            - ~~Strikethrough text~~ for deprecated items
                            - **Mixed *formatting* with `code`** and [links](https://example.com)
                            
                            This ensures **consistent** *visual* `appearance` across all components!
                            """);

        // Test edge cases
        Jt.checkbox("**Bold** and *italic* and `code` all together")
          .use();

        Jt.button("**Start** *processing* `data.csv` from [source](https://api.example.com)")
          .type("primary")
          .use();

        Jt.selectbox("Complex **formatting** test:", List.of(
          "**Option 1:** *Standard* `processing` with [docs](https://example.com)",
          "**Option 2:** ~~Legacy~~ *mode* with `fallback.py`",
          "**Option 3:** *Advanced* `ML` processing ~~(beta)~~"))
          .use();
    }
}
