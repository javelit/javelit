package tech.catheu.jeamlit.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.core.Jt;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.SessionState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static tech.catheu.jeamlit.components.JsConstants.*;

public class JeamlitServer {
    private static final Logger logger = LoggerFactory.getLogger(JeamlitServer.class);
    private final int port;
    private final String headersFile;
    private Undertow server;
    private final Map<String, WebSocketChannel> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionRegisteredTypes = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AppRunner appRunner;

    public interface AppRunner {
        void run(String sessionId) throws Exception;
    }

    public JeamlitServer(int port, String headersFile) {
        this.port = port;
        this.headersFile = headersFile;
    }

    public void setAppRunner(AppRunner runner) {
        this.appRunner = runner;
    }

    public void start() {
        PathHandler pathHandler = Handlers.path()
                .addPrefixPath("/ws", Handlers.websocket(new WebSocketHandler()))
                .addPrefixPath("/static", new ResourceHandler(
                        new ClassPathResourceManager(getClass().getClassLoader(), "static")
                ))
                .addExactPath("/", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                        exchange.getResponseSender().send(getIndexHtml());
                    }
                });

        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(pathHandler)
                .build();

        server.start();
        logger.info("Jeamlit server started on http://localhost:{}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public void notifyReload() {
        // Trigger re-run for all connected sessions
        for (String sessionId : sessions.keySet()) {
            try {
                runApp(sessionId);
            } catch (Exception e) {
                logger.error("Error reloading session " + sessionId, e);
            }
        }
    }

    private class WebSocketHandler implements WebSocketConnectionCallback {
        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, channel);

            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                @Override
                protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                    try {
                        final String data = message.getData();
                        final Map<String, Object> msg = objectMapper.readValue(data, Map.class);
                        handleMessage(sessionId, msg);
                    } catch (Exception e) {
                        logger.error("Error handling message", e);
                    }
                }

                @Override
                protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
                    sessions.remove(sessionId);
                    sessionRegisteredTypes.remove(sessionId);
                    Jt.clearSession(sessionId);
                }
            });

            channel.resumeReceives();

            // Send initial render
            try {
                runApp(sessionId);
            } catch (Exception e) {
                logger.error("Error in initial render", e);
                sendError(sessionId, e.getMessage());
            }
        }
    }

    private void handleMessage(String sessionId, Map<String, Object> message) throws Exception {
        String type = (String) message.get("type");

        if ("component_update".equals(type)) {
            String componentId = (String) message.get("componentId");
            // TODO IMPLEMENT - mark callback here
            Object value = message.get("value");

            // Now componentId is the same as the key - this is the fix!
            SessionState session = getSessionState(sessionId);
            if (session != null) {
                session.getWidgetStates().put(componentId, value);
            }

            // Re-run app
            runApp(sessionId);
        }

    }

    private void runApp(String sessionId) throws Exception {
        if (appRunner == null) {
            throw new IllegalStateException("No app runner configured");
        }

        Jt.ExecutionResult result = null;
        Jt.beginExecution(sessionId);
        try {
            appRunner.run(sessionId);
        } catch (Exception e) {
            // TODO CYRIL IMPROVE ERROR MANAGEMENT
            throw new RuntimeException("Failed to run ");
        } finally {
            result = Jt.endExecution();
        }
        if (result == null) {
            // TODO CYRIL IMPROVE ERROR MANAGEMENT
            throw new RuntimeException("Failed to run ");
        }

        // Collect component registrations
        final Set<String> sessionRegisteredTypesForThisSession = sessionRegisteredTypes.computeIfAbsent(sessionId, k -> new HashSet<>());
        final List<String> registrations = new ArrayList<>();

        for (JtComponent<?> component : result.jtComponents) {
            String componentType = component.getClass().getSimpleName();
            if (!sessionRegisteredTypesForThisSession.contains(componentType)) {
                registrations.add(component.register());
                sessionRegisteredTypesForThisSession.add(componentType);
            }
        }

        // Collect component HTML
        final List<String> componentHtml = new ArrayList<>();
        for (JtComponent<?> component : result.jtComponents) {
            componentHtml.add(component.render());
        }

        // Send to frontend
        final Map<String, Object> response = new HashMap<>();
        response.put("type", "render");
        response.put("html", String.join("\n", componentHtml));
        if (!registrations.isEmpty()) {
            response.put("registrations", registrations);
        }

        // Debug output
        logger.debug("Sending response:");
        logger.debug("  Components HTML: " + componentHtml.size());
        logger.debug("  Registrations: " + registrations.size());
        logger.debug("  HTML content: " + String.join("\n", componentHtml));

        sendMessage(sessionId, response);
    }

    private void sendMessage(String sessionId, Map<String, Object> message) {
        WebSocketChannel channel = sessions.get(sessionId);
        if (channel != null) {
            try {
                String json = objectMapper.writeValueAsString(message);
                WebSockets.sendText(json, channel, null);
            } catch (Exception e) {
                logger.error("Error sending message", e);
            }
        }
    }

    private void sendError(String sessionId, String error) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        message.put("error", error);
        sendMessage(sessionId, message);
    }

    private SessionState getSessionState(String sessionId) {
        try {
            final java.lang.reflect.Field field = Jt.class.getDeclaredField("sessions");
            field.setAccessible(true);
            final Map<String, SessionState> sessions = (Map<String, SessionState>) field.get(null);
            return sessions.get(sessionId);
        } catch (Exception e) {
            logger.error("Error accessing session state", e);
            return null;
        }
    }

    private String getIndexHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Jeamlit App</title>
                    <link rel="stylesheet" href="%s">
                    <style>
                        %s
                        
                        body {
                            font-family: var(--jt-font-family);
                            margin: 0;
                            padding: var(--jt-spacing-xl);
                            background-color: var(--jt-bg-secondary);
                            color: var(--jt-text-primary);
                            line-height: var(--jt-line-height-normal);
                        }
                        .container {
                            max-width: 800px;
                            margin: 0 auto;
                            background: var(--jt-bg-primary);
                            padding: var(--jt-spacing-xl);
                            border-radius: var(--jt-border-radius-lg);
                            box-shadow: var(--jt-shadow);
                        }
                        .error {
                            background-color: #fee;
                            border: 1px solid #fcc;
                            color: var(--jt-danger-color);
                            padding: var(--jt-spacing-md);
                            border-radius: var(--jt-border-radius);
                            margin: var(--jt-spacing-md) 0;
                        }
                    </style>
                    %s
                    <script type="module">
                      import { LitElement, html, css } from '%s';
                      
                      class JtTooltip extends LitElement {
                          static styles = css`
                              :host {
                                  position: relative;
                                  display: inline-block;
                              }
                              
                              .tooltip-trigger {
                                  cursor: help;
                                  color: var(--jt-text-secondary);
                                  font-size: var(--jt-font-size-sm);
                                  margin-left: var(--jt-spacing-xs);
                              }
                              
                              .tooltip-content {
                                  position: absolute;
                                  bottom: 100%%;
                                  left: 50%%;
                                  transform: translateX(-50%%);
                                  background: var(--jt-text-primary);
                                  color: var(--jt-text-white);
                                  padding: var(--jt-spacing-sm) var(--jt-spacing-md);
                                  border-radius: var(--jt-border-radius-sm);
                                  font-size: var(--jt-font-size-sm);
                                  white-space: nowrap;
                                  z-index: 1000;
                                  opacity: 0;
                                  visibility: hidden;
                                  transition: opacity var(--jt-transition-fast), visibility var(--jt-transition-fast);
                                  margin-bottom: var(--jt-spacing-xs);
                              }
                              
                              .tooltip-content::after {
                                  content: '';
                                  position: absolute;
                                  top: 100%%;
                                  left: 50%%;
                                  transform: translateX(-50%%);
                                  border: 4px solid transparent;
                                  border-top-color: var(--jt-text-primary);
                              }
                              
                              :host(:hover) .tooltip-content {
                                  opacity: 1;
                                  visibility: visible;
                              }
                          `;
                          
                          static properties = {
                              text: { type: String }
                          };
                          
                          render() {
                              return html`
                                  <span class="tooltip-trigger">?</span>
                                  <div class="tooltip-content">${this.text}</div>
                              `;
                          }
                      }
                      
                      customElements.define('jt-tooltip', JtTooltip);
                    </script>
                </head>
                <body>
                    <div id="app" class="container">
                        <p>Connecting to Jeamlit server...</p>
                    </div>
                    <script>
                        const ws = new WebSocket('ws://localhost:%d/ws');
                        const app = document.getElementById('app');
                
                        // Store websocket reference for components
                        window.ws = ws;
                
                        // Create emit function for new component system
                        window.jeamlit = {
                            emit: function(componentId, value) {
                                ws.send(JSON.stringify({
                                    type: 'component_update',
                                    componentId: componentId,
                                    value: value
                                }));
                            }
                        };
                
                        ws.onmessage = (event) => {
                            const message = JSON.parse(event.data);
                
                            if (message.type === 'render') {
                                // Handle new component system
                                if (message.registrations) {
                                    // Ensure a hidden container is present
                                    let container = document.getElementById('jeamlit-registrations');
                                    if (!container) {
                                      container = document.createElement('div');
                                      container.id = 'jeamlit-registrations';
                                      container.style.display = 'none';
                                      document.body.appendChild(container);
                                    }
                
                                    message.registrations.forEach((registration) => {
                                        // Parse the registration HTML safely
                                        const template = document.createElement('template');
                                        template.innerHTML = registration;
                                        const fragment = template.content;
                
                                        // Execute all <script> tags inside the fragment
                                        fragment.querySelectorAll('script').forEach((script) => {
                                          const newScript = document.createElement('script');
                                          if (script.type) newScript.type = script.type;
                                          if (script.src) {
                                            newScript.src = script.src;
                                          } else {
                                            newScript.textContent = script.textContent;
                                          }
                                          document.head.appendChild(newScript);
                                        });
                
                                        // Inject the rest of the HTML (custom elements etc.)
                                        container.appendChild(fragment);
                                    });
                                }
                
                                // Clear app content
                                app.innerHTML = '';
                
                                // Render HTML components
                                if (message.html && message.html.trim() !== '') {
                                    const htmlDiv = document.createElement('div');
                                    htmlDiv.innerHTML = message.html;
                                    // Append each child element individually to maintain proper DOM structure
                                    while (htmlDiv.firstChild) {
                                        app.appendChild(htmlDiv.firstChild);
                                    }
                                }
                            } else if (message.type === 'error') {
                                showError(message.error);
                            }
                        };
                
                        ws.onerror = () => {
                            showError('WebSocket connection error');
                        };
                
                        ws.onclose = () => {
                            showError('Connection to server lost');
                        };
                
                        function showError(error) {
                            const errorDiv = document.createElement('div');
                            errorDiv.className = 'error';
                            errorDiv.textContent = 'Error: ' + error;
                            app.appendChild(errorDiv);
                        }
                    </script>
                </body>
                </html>
                """.formatted(MATERIAL_SYMBOLS_CDN, DESIGN_SYSTEM_CSS, loadCustomHeaders(), LIT_DEPENDENCY, port);
    }

    private String loadCustomHeaders() {
        if (headersFile == null) {
            return "";
        }

        final Path headerPath = Paths.get(headersFile);
        if (!Files.exists(headerPath)) {
            logger.debug("Headers file {} does not exist, skipping custom headers", headersFile);
            return "";
        }

        try {
            String content = Files.readString(headerPath);
            logger.info("Loaded custom headers from {}", headersFile);
            return content;
        } catch (Exception e) {
            logger.warn("Failed to load headers file {}: {}", headersFile, e.getMessage());
            return "";
        }
    }
}