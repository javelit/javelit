# Servlet Migration Plan

## Overview
Migrate Javelit to support both standalone Undertow server and servlet containers (Tomcat, Jetty, etc.) by creating a servlet abstraction layer.

## Architecture

### Module Structure
```
javelit-parent/
├── javelit-core/          # Core framework (components, Jt, StateManager)
├── javelit-servlet/       # Servlet abstraction layer (NEW)
└── javelit/              # CLI + Undertow server (existing, refactored)
```

### Abstractions Created (Phase 3.1 - COMPLETED)

Located in `javelit-core/src/main/java/io/javelit/core/`:

1. **HttpRequest.java** - HTTP request abstraction
2. **HttpResponse.java** - HTTP response abstraction
3. **HttpSession.java** - Session abstraction
4. **HttpCookie.java** - Cookie value object
5. **MultipartFormData.java** - Multipart form data abstraction
6. **WebSocketSession.java** - WebSocket session abstraction

## Implementation Phases

### Phase 1: Create Module Structure ✅ COMPLETED
- Created parent POM with 3 modules
- Set up dependency management
- Configured build plugins

### Phase 2: Extract javelit-core ✅ COMPLETED
- Moved all components to javelit-core
- Moved Jt, StateManager, BuildSystem, etc.
- Kept Server, AppRunner, Reloader classes in javelit
- Fixed Surefire working directory for E2E tests

### Phase 3: Create JavelitHandler 🚧 IN PROGRESS

#### Phase 3.1: Create Core Abstractions ✅ COMPLETED
All abstraction interfaces created in javelit-core.

#### Phase 3.2: Extract JavelitHandler Business Logic 🔄 CURRENT
Create `javelit-servlet/src/main/java/io/javelit/servlet/JavelitHandler.java`

**Logic to Extract from Server.java**:
- Lines 312-321: IndexHandler → `handleIndex()`
- Lines 461-548: MediaHandler → `handleMedia()`
- Lines 390-459: UploadHandler → `handleUpload()`
- Lines 323-387: XsrfValidationHandler → `validateXsrf()` / `createXsrfToken()`
- Lines 551-632: WebSocketHandler → `handleWebSocketConnect/Message/Close()`
- Lines 641-697: handleMessage() → business logic
- Lines 699-750: RenderServer implementation → WebSocket message sending
- Lines 816-827: XSRF token generation

**Key Features**:
- Framework-agnostic HTTP handling
- XSRF token validation
- WebSocket message routing
- StateManager integration
- Media serving with range request support
- Multipart file upload handling

### Phase 4: Create JavelitServlet ⏳ PENDING

#### Phase 4.1: Create Servlet Adapters
Implement adapters in `javelit-servlet/`:
- `ServletHttpRequest.java` - wraps HttpServletRequest
- `ServletHttpResponse.java` - wraps HttpServletResponse
- `ServletHttpSession.java` - wraps javax.servlet.http.HttpSession
- `ServletMultipartFormData.java` - wraps servlet Part API
- `ServletWebSocketSession.java` - wraps javax.websocket.Session

#### Phase 4.2: Implement JavelitServlet
Create `JavelitServlet.java` with:
- Builder pattern matching Server.Builder
- Route mapping: /, /_/media/, /_/upload, /_/static/
- XSRF validation for state-changing requests
- Delegates to JavelitHandler

### Phase 5: Create JavelitWebSocketEndpoint ⏳ PENDING
Create `JavelitWebSocketEndpoint.java`:
- @ServerEndpoint annotation for "/_/ws"
- Configurator to pass HTTP session
- Delegates to JavelitHandler for message processing

### Phase 6: Refactor Server.java ⏳ PENDING

#### Step 6.1: Create Undertow Adapters
Implement adapters in `javelit/`:
- `UndertowHttpRequest.java` - wraps HttpServerExchange
- `UndertowHttpResponse.java` - wraps HttpServerExchange
- `UndertowHttpSession.java` - wraps io.undertow.server.session.Session
- `UndertowMultipartFormData.java` - wraps FormData
- `UndertowWebSocketSession.java` - wraps WebSocketChannel

#### Step 6.2: Refactor Server.java
Replace handler inner classes with JavelitHandler:
- Create JavelitHandler instance in Server constructor
- Wrap Undertow objects in adapters
- Delegate to JavelitHandler methods
- Keep Undertow-specific server setup (Undertow.Builder, PathHandler)

### Phase 7: Testing & Verification ⏳ PENDING
1. Run full E2E test suite with refactored Server
2. Test servlet deployment in embedded Tomcat
3. Verify Spring Boot integration
4. Verify Quarkus integration

## Route Mapping (PRESERVED)

All existing routes maintained:
- `/` → Index page
- `/_/ws` → WebSocket endpoint
- `/_/media/{hash}?sid={sessionId}` → Media files
- `/_/upload` → File upload (PUT)
- `/_/static/*` → Static resources

## Key Design Decisions

1. **Abstractions in javelit-core**: Minimal dependencies, shared by both implementations
2. **Builder pattern preserved**: JavelitServlet.Builder matches Server.Builder API
3. **XSRF handling extracted**: Same security model in both implementations
4. **StateManager.RenderServer**: JavelitHandler implements this interface
5. **No MultipartConfig annotation**: Consumers configure via servlet container
6. **Working directory fix**: Tests run from project root (surefire config)

## Benefits

1. **Servlet Container Support**: Deploy to Tomcat, Jetty, Wildfly, etc.
2. **Framework Integration**: Spring Boot, Quarkus, Ktor can embed Javelit
3. **Backward Compatible**: Standalone Undertow server unchanged from user perspective
4. **Shared Business Logic**: No code duplication between implementations
5. **Testability**: Mock HTTP abstractions for unit testing

## Distribution Artifacts (UNCHANGED)

- `io.javelit:javelit-core:0.59.0` - Core library
- `io.javelit:javelit-servlet:0.59.0` - Servlet for framework integration (NEW)
- `io.javelit:javelit:0.59.0` - Thin JAR with Server + CLI
- `io.javelit:javelit:0.59.0:all` - Fat JAR (CLI usage)
