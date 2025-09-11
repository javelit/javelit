# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**Jeamlit** is a Streamlit-like framework for building data apps in Java.

- **Type**: Java application
- **Build System**: Maven
- **Java Version**: 21 (configured in pom.xml, runs on Java 21+)
- **Package Structure**: `tech.catheu`
- **Design Decisions**: See DESIGN_DECISIONS.md

## Build Commands

### Building
```bash
./mvnw clean compile    # Clean and compile
./mvnw package          # Package as JAR
./mvnw clean            # Clean build directory
```

### Running
To run Jeamlit applications, use the CLI:
```bash
./mvnw package -DskipTests         # Build JAR first
java -jar target/jeamlit-1.0-SNAPSHOT.jar run examples/PageLinkExample.java
```

### Testing
```bash
./mvnw test                            # Run all tests
./mvnw test -Dtest=TestClassName       # Run specific test class
./mvnw test -Dtest=TestClass#testMethod # Run specific test method
./mvnw package -DskipTests             # Build without tests
```

**IMPORTANT**: Do NOT use `java -cp target/classes ...` to test components. Use the proper CLI or write proper tests.
**IMPORTANT**: NEVER launch a server with java -jar jeamlit.jar run TestApp.java
**IMPORTANT**: Instead, write E2E tests

## Project Structure

```
src/main/java/tech/catheu/  # Main application code
src/test/java/              # Unit tests
src/main/resources/         # Application resources
```

## Configuration

**pom.xml** - Maven configuration
- Group ID: `tech.catheu`
- Artifact ID: `jeamlit`
- Version: `1.0-SNAPSHOT`

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
- when implementing frontend components: do not re-create window.jeamlit and a function to send messages via websocket. Just assume window.jeamlit.emit is available (thanks to index.html) 
- again make sure to follow the same spec as streamlit
- There is 1 exception to this: do not add args/kwargs equivalent (varargs and Map parameters). Just put a comment instead, saying it's not implemented.
- For a component that will have to support use_container_width:
    - Main containers (:host in lit css) should always be in display block
    - Then :host([use-container-width]) .[REPLACE_BY_SOME_INTERNAL_CLASS] { width: 100%; }
- Main containers (:host in lit css) should always be in display block
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

### Writing tests
#### E2E tests
1. Rule 1:
A test method using playwright should look like the following:
```
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            
            public class TestApp {
                public static void main(String[] args) {
                    <THE APP TO TEST>
                }
            }
            """;
        
        final Path appFile = JeamlitTestHelper.writeTestApp(app);
        Server server = null;

        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(HEADLESS);
             final Page page = browser.newPage()) {
             server = JeamlitTestHelper.startServer(appFile);
             page.navigate("http://localhost:" + server.port);
             
             <PERFORM ASSERTIONS HERE>
             
        } finally {
            JeamlitTestHelper.stopServer(server);
            JeamlitTestHelper.cleanupTempDir(appFile.getParent());
        }
```

2. Rule 2:
Use PlaywrightAssertions:
```
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
```
