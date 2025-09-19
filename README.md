# Jeamlit <span style="transform: scale(-1,1); display:inline-block;">🚡</span>

A Streamlit-like framework for building interactive data applications in Java.

## Overview

Jeamlit brings the simplicity of Streamlit to Java developers. Write data apps with minimal code using a familiar API, complete with automatic hot-reloading and session state management.

## Features

- **Simple API**: Familiar Streamlit-like syntax (`Jeamlit.title()`, `Jeamlit.slider()`, etc.)
- **Hot Reload**: Automatic recompilation and browser refresh on file changes
- **Session State**: Persistent state management across app reruns
- **Web Components**: Modern frontend built with Lit web components
- **WebSocket Communication**: Real-time updates between frontend and backend

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven (wrapper included)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd jeamlit
```

2. Build the project:
```bash
./mvnw clean package
```

### Running Your First App

1. Create a simple Java file (e.g., `MyApp.java`):

```java
import io.jeamlit.core.Jt;

public class MyApp {
    public static void main(String[] args) {
        Jt.title("My First Jeamlit App");

        Jt.text("Welcome to Jeamlit - Streamlit for Java!");

        int age = Jt.slider("Select your age", 0, 100, 25);
        Jt.write("You selected age: " + age);

        if (Jt.button("Click me!")) {
            Jt.write("Button was clicked!");

            var state = Jt.sessionState();
            int clickCount = state.computeInt("clicks", (k, v) -> v == null ? 1 : v + 1);

            Jt.write("Button clicked " + clickCount + " times");
        }
    }
}
```

2. Run the app:
```bash
java -jar target/jeamlit-1.0-SNAPSHOT.jar run MyApp.java
```

3. Open your browser to `http://localhost:8080`

The app will automatically reload when you save changes to `MyApp.java`.

## API Reference

### Basic Components

- `Jt.title(String)` - Display a title
- `Jt.text(String)` - Display text
- `Jt.write(Object)` - Display any object

### Interactive Widgets

- `Jt.button(String)` - Button widget (returns boolean)
- `Jt.slider(String, min, max, default)` - Slider widget (returns int)

### State Management

- `Jt.sessionState()` - Get typed session state (extends Map<String, Object>)
- `state.getInt(key, defaultValue)` - Get integer value with default
- `state.getString(key, defaultValue)` - Get string value with default
- `state.getBoolean(key, defaultValue)` - Get boolean value with default
- `state.computeInt(key, (k, v) -> ...)` - Compute integer value (handles null/initialization)
- `state.put(key, value)` - Set any value (full Map interface available)
- All standard Map operations: `get()`, `containsKey()`, `remove()`, `compute()`, etc.

## CLI Options

```bash
# Run an app
java -jar target/jeamlit-1.0-SNAPSHOT.jar run MyApp.java

# Specify custom port
java -jar target/jeamlit-1.0-SNAPSHOT.jar run MyApp.java --port 3000

# Don't open browser automatically
java -jar target/jeamlit-1.0-SNAPSHOT.jar run MyApp.java --no-browser

# Run with full path
java -jar target/jeamlit-1.0-SNAPSHOT.jar run src/main/java/MyApp.java

# Add custom classpath
java -jar target/jeamlit-1.0-SNAPSHOT.jar run MyApp.java --classpath /path/to/libs
```

## Development

### Building from Source

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package JAR
./mvnw package
```

### Running Tests

```bash
./mvnw test
```

### Development Workflow

1. Build the project: `./mvnw package`
2. Create your Java app file
3. Run: `java -jar target/jeamlit-1.0-SNAPSHOT.jar run MyApp.java`

**Note**: The framework automatically compiles your Java file and handles file watching with recompilation. Optional Java Agent provides enhanced hot-reload capabilities.

## Architecture

- **Web Server**: Undertow for HTTP/WebSocket communication
- **Frontend**: Lit web components with hybrid reactivity
- **Hot Reload**: File watching with automatic recompilation (optional Java Agent for enhanced class reloading)
- **State Management**: Session-based state similar to Streamlit
- **CLI**: Picocli-based command-line interface

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

[Add license information]
