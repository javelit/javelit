# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**Javelit** is a Streamlit-like framework for building data apps in Java.

- **Type**: Java application
- **Build System**: Maven
- **Java Version**: 21 (configured in pom.xml, runs on Java 21+)
- **Package Structure**: `io.javelit`

## Build Commands

### Building
```bash
./mvnw clean compile    # Clean and compile
./mvnw package          # Package as JAR
./mvnw clean            # Clean build directory
```

### Running
To run Javelit applications, use the CLI:
```bash
./mvnw package -DskipTests         # Build JAR first
java -jar target/javelit-1.0-SNAPSHOT.jar run examples/PageLinkExample.java
```

### Testing
```bash
./mvnw test                            # Run all tests
./mvnw test -Dtest=TestClassName       # Run specific test class
./mvnw test -Dtest=TestClass#testMethod # Run specific test method
./mvnw package -DskipTests             # Build without tests
```

**IMPORTANT**: Do NOT use `java -cp target/classes ...` to test components. Use the proper CLI or write proper tests.
**IMPORTANT**: NEVER launch a server with java -jar javelit.jar run TestApp.java
**IMPORTANT**: Instead, write E2E tests

### Generate JSON Documentation

To generate the `javelit.json` file with API documentation:

```bash
./mvnw clean package -Prelease -DskipTests
```

This command:
- Cleans the project
- Builds the project with the release profile
- Skips tests
- Triggers the JsonDoclet execution which generates `javelit.json`

The JSON file will be created in the project root directory and contains documentation for:
- All public methods in the `Jt` class
- All public methods in component classes (components package)
- Return types are extracted from the component's `JtComponent<T>` generic parameter where applicable

## JsonDoclet

The custom JsonDoclet implementation:
- Processes only `io.javelit.core.Jt` and `io.javelit.components.*` classes
- For methods in the `Jt` class that return component builders, extracts the actual component return type from the component's `JtComponent<T>` generic parameter
- Uses Jackson for JSON serialization
- Outputs in streamlit.json compatible format

## Project Structure

```
src/main/java/io/javelit  # Main application code
src/main/resources/         # Application resources
src/test/java/              # Unit tests
```

## Configuration

**pom.xml** - Maven configuration
- Group ID: `io.javelit`
- Artifact ID: `javelit`

## Development Notes

- Maven wrapper (mvnw) included - no global Maven installation needed
- Standard Maven directory structure
- See DESIGN_DECISIONS.md for architectural details
- all examples and test apps should be put in the examples folder

### Implementation of Components 
- Focus on the quality of the component themselves.
- Make sure to look at streamlit doc. I will share the links. Make sure to also look at it during implementation.
- Make sure to build a proper plans for each component, especially if they are complex. It's ok if the task becomes very big.
- always use a builder pattern like it's done in the existing components
- use the same color effects and animations as streamlit components
- Ensure things compile regularly
- always use lit. To defined components
- when implementing frontend components: do not re-create window.javelit and a function to send messages via websocket. Just assume window.javelit.emit is available (thanks to index.html) 
- again make sure to follow the same spec as streamlit
- There is 1 exception to this: do not add args/kwargs equivalent (varargs and Map parameters). Just put a comment instead, saying it's not implemented.
- For a component that will have to support use_container_width:
    - Main containers (:host in lit css) should always be in display block
    - Then :host([use-container-width]) .[REPLACE_BY_SOME_INTERNAL_CLASS] { width: 100%; }
- Main containers (:host in lit css) should always be in display block
- **CRITICAL - Mustache Escaping for @Language("html") Fields**:
  - When a Java component field is annotated with `@Language("html")` (e.g., `final @Language("html") @Nullable String caption;`), it means the field contains HTML content that was already processed in Java (typically via `markdownToHtml()`)
  - In the `.render.html.mustache` template, when passing this field as an HTML attribute to a web component, **ALWAYS** use double brackets `{{fieldName}}`, **NEVER** triple brackets `{{{fieldName}}}`
  - Correct example: `<jt-image caption="{{caption}}"></jt-image>`
  - Wrong example: `<jt-image caption="{{{caption}}}"></jt-image>`
  - Reason: Double brackets properly escape the HTML for safe insertion into the attribute value. The web component will then use `unsafeHTML(this.caption)` in its register template to render the HTML content.
  - Triple brackets `{{{` should ONLY be used in `.register.html.mustache` templates for injecting CSS (e.g., `{{{ MARKDOWN_CSS }}}`)
- **CRITICAL - Markdown CSS Class**: When rendering markdown content in Lit components, ALWAYS use the class name `markdown-content`, NEVER `markdown-body`. This is the correct class name for markdown styling in this project.
- **Width method pattern**: Whenever a ComponentBuilder has a `width(String)` method, it must also include a `width(int)` overload for pixel values:
```java
public Builder width(final int widthPixels) {
    if (widthPixels < 0) {
        throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
    }
    this.width = String.valueOf(widthPixels);
    return this;
}
```
- **CRITICAL - CSS Design System Variables**: All component CSS must use design system variables from `design_system.html.mustache`, never hardcoded colors
  - Always use CSS variables like `var(--jt-text-primary)`, `var(--jt-bg-primary)`, etc.
  - Never use hardcoded colors like `white`, `black`, `#ffffff`, `rgb(255,255,255)`, etc.
  - This enables dark mode support and consistent theming across all components
  - Examples of correct usage:
    - ✅ `background: var(--jt-bg-primary);`
    - ✅ `color: var(--jt-text-white);`
    - ✅ `border: 1px solid var(--jt-border-color);`
  - Examples of incorrect usage:
    - ❌ `background: white;`
    - ❌ `color: #000000;`
    - ❌ `border: 1px solid rgb(240, 242, 246);`
  - See TableComponent, RadioComponent, ToggleComponent fixes as reference examples

### Writing tests
#### E2E tests
1. Rule 1:
A test method using playwright should use the PlaywrightUtils.runInSharedBrowser.
Here is an example: 
```
    @Test
    void testBasicCodeDisplay() {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    Jt.code("some code").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(app, page -> {
            // Verify code component is rendered (filter by content to find the right one)
            assertThat(page.locator("#app jt-internal-code")).isVisible(WAIT_1_SEC_MAX);
            // other tests
        });
    }
```

2. Rule 2:
Use PlaywrightAssertions:
```
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
```
