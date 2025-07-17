# Jeamlit Design Decisions

## Project Goal

Implement a Streamlit-like framework in Java for building data apps with minimal code.

## Core Design Decisions

### File Watching

- **Library**: `io.methvin.directory-watcher`
- **Current Limitation**: Single file watching (MVP constraint)
- **Use Case**: Auto-reload when app file changes
- **Future Enhancement**: Multi-file/directory watching

### Architecture

#### Web Server: Undertow

- `undertow-core`: Core HTTP server
- `undertow-servlet`: Servlet support
- `undertow-websockets-jsr`: WebSocket support for real-time communication

#### Frontend Strategy: Lit Web Components with Hybrid Reactivity

**Lit Web Components** for rich, reusable UI components with a **hybrid approach**:

- **Backend-driven**: Main application flow (full re-run on file change)
- **Client-side reactivity**: UI-only updates (hover, expand/collapse, animations)
- **Selective communication**: Smart backend calls (e.g., slider updates on release vs. drag)

**WebSocket Communication**:
- Components send value changes via WebSocket
- Backend decides if change requires full re-run or partial update
- Enables optimizations like debouncing or batching updates

**Benefits**:
- Rich component library possibilities
- Smooth UX with local reactivity
- Reduced server load for UI-only interactions
#### State Management: Session-based

Similar to Streamlit, with the following characteristics:

- **Unique sessions**: Each browser tab/WebSocket connection gets unique session
- **Explicit state**: `Jeamlit.sessionState()` returns a Map for state storage
- **Widget persistence**: Widget values automatically persisted by component ID across reruns

**State Lifecycle**:
- Survives script reruns (file changes)
- Cleared on page refresh (new WebSocket connection)
- Isolated per user/tab

**Example Usage**:
```java
// Automatic widget state
int age = Jeamlit.slider("Age", 0, 100, 25); // Remembers value across reruns

// Explicit session state
Map<String, Object> state = Jeamlit.sessionState();
if (!state.containsKey("counter")) {
    state.put("counter", 0);
}
if (Jeamlit.button("Increment")) {
    state.put("counter", (int)state.get("counter") + 1);
}
```
#### Component API Design: Fluent API

**Design Principles**:
- No annotations - simple and intuitive
- Static methods for components: `Jeamlit.text("Hello")`, `Jeamlit.slider(0, 100)`
- Method chaining where appropriate
- Familiar to Streamlit users: `write()`, `slider()`, `button()`, `columns()`

**Example Usage**:
```java
Jeamlit.title("My App");
int value = Jeamlit.slider("Select value", 0, 100, 50);
if (Jeamlit.button("Submit")) {
    Jeamlit.write("You selected: " + value);
}
```

**Custom Components (Legacy Design)**:

Two-part structure: JavaScript file (template/behavior) + Java class (API/values)

- **Java class**: Exposes values as public Map (schemaless approach)
- **Convention-based**: Java class name maps to JavaScript file
- **Package-aware naming** to prevent conflicts

**New Component System (v2 Design)**:

**Core Design Principles:**
- Simple to create custom components
- Bidirectional communication handled by framework
- Component registration system
- Native and custom components use same pattern
- Production optimization (load once)
- Technology agnostic (Lit, vanilla JS, React, etc.)
- CSS management per component

**Component Pattern:**
```java
abstract class JeamlitComponent {
    abstract String register();  // Component definition (once per type)
    abstract String render();    // HTML instance with data (per render)
    void handleEvent(String event, Map<String, Object> props) { }
}
```

**Example Implementation:**
```java
class ButtonComponent extends JeamlitComponent {
    private String label;
    private boolean clicked = false;
    
    @Override
    String register() {
        return "<script>/* Custom element definition */</script>";
    }
    
    @Override
    String render() {
        return "<button onclick=\"jeamlit.emit('click', '" + getId() + "')\">" + label + "</button>";
    }
    
    @Override
    void handleEvent(String event, Map<String, Object> props) {
        if ("click".equals(event)) clicked = true;
    }
}
```

**Key Benefits:**
- Clean separation: definition vs instance data
- Universal pattern for all components
- Performance: register once, render many times
- Technology flexibility: any frontend framework

**Current Limitations Identified:**
1. ~~**String-based HTML generation**: Manual concatenation is error-prone, XSS risks~~ (DECISION: Keep string-based approach for simplicity)
2. **Global state management**: Static registries cause isolation issues
3. **Component lifecycle**: No clear create/destroy semantics
4. **Event handling**: Single method, no type safety
5. **Props serialization**: JSON-only limits complex data types

**Next Iteration Improvements:**
1. ~~Template system for safe HTML generation~~ (DECISION: Keep string-based approach)
2. Per-session component registries
3. Typed event handling with annotations
4. Props validation and schemas
5. Component lifecycle management

**Frontend Protocol:**
```json
// First render: registration + render
{"type": "render", "registrations": ["<script>..."], "html": "<button>..."}

// Subsequent renders: just HTML
{"type": "render", "html": "<button>..."}

// Events back to server
{"type": "component_event", "componentId": "...", "event": "click", "props": {...}}
```

### Development Experience

#### CLI Interface

**Command**: `jeamlit run Main.java`

- Watches the specified Java file for changes
- Automatically reruns on file save
- Opens browser to localhost:8080 (or specified port)

**Example Usage**:
```bash
jeamlit run MyApp.java
jeamlit run MyApp.java --port 3000
jeamlit run src/main/java/MyApp.java
```

#### Hot Reload Strategy

1. File watcher detects changes
2. Recompiles and reruns the Java file
3. WebSocket notifies browser to update
4. State preserved via session management

#### Build/Run Process: Hybrid Approach with Java Agent

**Initial Run**: Full Maven compile + launch JVM with agent
- Uses `-javaagent:jeamlit-agent.jar` for class hot-swapping
- File changes: Attempt hot-reload via Instrumentation API
- Falls back to full restart for structural changes

**Execution Flow**:
1. User's main() runs top to bottom
2. Jeamlit.* calls accumulate components in internal tree
3. Widget methods return previous run's state
4. After main() completes, entire UI tree sent via WebSocket
5. Frontend receives and renders complete state

**Jeamlit Class Role**:
- Maintains component tree for current execution
- Stores session state between runs
- Manages WebSocket connections
- Clears/resets context before each run

#### Error Handling

_To be decided_