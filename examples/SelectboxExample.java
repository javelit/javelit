///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.47.0



import java.util.List;import io.jeamlit.core.Jt;
import io.jeamlit.core.JtComponent;

public class SelectboxExample {

    enum Status {
        DRAFT, PENDING, APPROVED, REJECTED
    }

    record Product(String name, double price) {}

    public static void main(String[] args) {
        Jt.title("# selectbox Component Demo").use();

        Jt.markdown("## Basic selectbox with Strings").use();
        String basicChoice = Jt.selectbox("Choose your favorite programming language",
                List.of("Java", "Python", "JavaScript", "TypeScript", "Go"))
                .use();
        if (basicChoice != null) {
            Jt.text("Selected: **" + basicChoice + "**").use();
        } else {
            Jt.text("No language selected").use();
        }

        Jt.markdown("## selectbox with Help").use();
        String frameworkChoice = Jt.selectbox("Select web framework",
                List.of("React", "Vue.js", "Angular", "Svelte"))
                .help("Choose your preferred frontend framework")
                .use();
        if (frameworkChoice != null) {
            Jt.text("🚀 Framework: **" + frameworkChoice + "**").use();
        }

        Jt.markdown("## selectbox with Default Selection").use();
        String priorityChoice = Jt.selectbox("Select priority level",
                List.of("Low", "Medium", "High", "Critical"))
                .index(1)// Pre-select "Medium" (index 1)
                .help("Choose the priority for this task")
                .use();
        if (priorityChoice != null) {
            Jt.text("⭐ Priority: **" + priorityChoice + "**").use();
        }

        Jt.markdown("## selectbox with No Default Selection").use();
        String regionChoice = Jt.selectbox("Select region",
                List.of("North America", "Europe", "Asia", "South America", "Africa", "Oceania"))
                .index(null)// Explicitly no pre-selection
                .help("Choose your target region")
                .use();
        if (regionChoice != null) {
            Jt.text("🌍 Region: **" + regionChoice + "**").use();
        } else {
            Jt.text("⚠️ No region selected yet").use();
        }

        Jt.markdown("## selectbox with Custom Placeholder").use();
        String cityChoice = Jt.selectbox("Select destination city",
                List.of("New York", "London", "Tokyo", "Sydney", "Paris"))
                .index(null)
                .placeholder("Pick a destination...")
                .use();
        if (cityChoice != null) {
            Jt.text("✈️ Destination: **" + cityChoice + "**").use();
        }

        Jt.markdown("## selectbox with Accept New Options (Combobox)").use();
        String skillChoice = Jt.selectbox("Add or select a skill",
                List.of("Java Programming", "Web Design", "Data Analysis", "Machine Learning"))
                .acceptNewOptions(true)
                .help("You can select from the list or type a new skill")
                .use();
        if (skillChoice != null) {
            Jt.text("💪 Skill: **" + skillChoice + "**").use();
        }

        Jt.markdown("## Disabled selectbox").use();
        Jt.selectbox("Premium features",
                List.of("Advanced Analytics", "Custom Reports", "API Access", "Priority Support"))
                .disabled(true)
                .help("Upgrade to premium to access these features")
                .use();
        Jt.text("⚪ This select box is disabled").use();

        Jt.markdown("## Label Visibility Options").use();

        // Visible label (default)
        String visibleChoice = Jt.selectbox("**Visible** label selectbox",
                List.of("Option A", "Option B", "Option C"))
                .labelVisibility(JtComponent.LabelVisibility.VISIBLE)
                .use();

        // Hidden label (spacer)
        String hiddenChoice = Jt.selectbox("Hidden label selectbox",
                List.of("Choice 1", "Choice 2", "Choice 3"))
                .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                .use();

        // Collapsed label (no space)
        String collapsedChoice = Jt.selectbox("Collapsed label selectbox",
                List.of("Item X", "Item Y", "Item Z"))
                .labelVisibility(JtComponent.LabelVisibility.COLLAPSED)
                .use();

        Jt.markdown("## Width Options").use();

        // Content width (default)
        Jt.text("Content width:").use();
        String contentWidth = Jt.selectbox("Content width selectbox",
                List.of("Short", "Medium Length", "Very Long Option Name"))
                .width("content")
                .use();

        // Stretch width
        Jt.text("Stretch width:").use();
        String stretchWidth = Jt.selectbox("Stretch width selectbox",
                List.of("Alpha", "Beta", "Gamma"))
                .width("stretch")
                .use();

        // Fixed pixel width
        Jt.text("Fixed 350px width:").use();
        String fixedWidth = Jt.selectbox("Fixed width selectbox",
                List.of("One", "Two", "Three"))
                .width(350)
                .use();

        Jt.markdown("## selectbox with onChange Callback").use();
        Jt.selectbox("Status", List.of("Active", "Inactive", "Pending"))
                .onChange(selected -> {
                    if (selected != null) {
                        Jt.text("🔄 Callback triggered: Status changed to **" + selected + "**").use();
                    }
                })
                .use();

        Jt.markdown("## Enum-based selectbox").use();
        Status status = Jt.selectbox("Select document status", List.of(Status.values()))
                .use();
        if (status != null) {
            Jt.text("📄 Document status: **" + status + "**").use();
        }

        Jt.markdown("## selectbox with Objects and FormatFunction").use();
        List<Product> products = List.of(
            new Product("Laptop", 999.99),
            new Product("Mouse", 29.99),
            new Product("Keyboard", 79.99),
            new Product("Monitor", 299.99)
        );

        Product selectedProduct = Jt.selectbox("Select product", products)
                .formatFunction(p -> p.name() + " - $" + p.price())
                .help("Choose a product from our catalog")
                .use();

        if (selectedProduct != null) {
            Jt.text("🛒 Selected: **" + selectedProduct.name() + "** ($" + selectedProduct.price() + ")").use();
        }

        Jt.markdown("## Markdown Support in Options").use();
        String markdownChoice = Jt.selectbox("Choose formatting style",
                List.of(
                    "**Bold** text style",
                    "*Italic* text style",
                    "`Code` text style",
                    "[Link](https://example.com) style",
                    "~~Strikethrough~~ style"
                ))
                .use();

        Jt.markdown("## Empty Options with Accept New Options").use();
        String newTagChoice = Jt.selectbox("Add a new tag",
                                           List.<String>of())// Empty options list
                .acceptNewOptions(true)
                .help("Type a new tag name")
                .use();
        if (newTagChoice != null) {
            Jt.text("🏷️ New tag: **" + newTagChoice + "**").use();
        }

        Jt.markdown("## Empty Options without Accept New Options (Disabled)").use();
        Jt.selectbox("No options available",
                List.of())// Empty options list
                .acceptNewOptions(false)// This makes it disabled
                .help("This selectbox is disabled because no options are available")
                .use();
        Jt.text("This selectbox is automatically disabled").use();

        // Summary section
        Jt.markdown("---").use();
        Jt.markdown("### Summary").use();

        int selectedCount = 0;
        if (basicChoice != null) {
            selectedCount++;
        }
        if (frameworkChoice != null) {
            selectedCount++;
        }
        if (priorityChoice != null) {
            selectedCount++;
        }
        if (regionChoice != null) {
            selectedCount++;
        }
        if (cityChoice != null) {
            selectedCount++;
        }
        if (skillChoice != null) {
            selectedCount++;
        }
        if (visibleChoice != null) {
            selectedCount++;
        }
        if (hiddenChoice != null) {
            selectedCount++;
        }
        if (collapsedChoice != null) {
            selectedCount++;
        }
        if (contentWidth != null) {
            selectedCount++;
        }
        if (stretchWidth != null) {
            selectedCount++;
        }
        if (fixedWidth != null) {
            selectedCount++;
        }
        if (markdownChoice != null) {
            selectedCount++;
        }
        if (status != null) {
            selectedCount++;
        }
        if (selectedProduct != null) {
            selectedCount++;
        }
        if (newTagChoice != null) {
            selectedCount++;
        }

        Jt.text("Total select boxes with selections: **" + selectedCount + "**").use();
    }
}
