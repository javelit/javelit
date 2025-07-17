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

public class JeamlitServer {
    private static final Logger logger = LoggerFactory.getLogger(JeamlitServer.class);
    private final int port;
    private Undertow server;
    private final Map<String, WebSocketChannel> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AppRunner appRunner;

    public interface AppRunner {
        void run(String sessionId) throws Exception;
    }

    public JeamlitServer(int port) {
        this.port = port;
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
                        String data = message.getData();
                        Map<String, Object> msg = objectMapper.readValue(data, Map.class);
                        handleMessage(sessionId, msg);
                    } catch (Exception e) {
                        logger.error("Error handling message", e);
                    }
                }

                @Override
                protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
                    sessions.remove(sessionId);
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

        Jt.beginExecution(sessionId);
        try {
            appRunner.run(sessionId);

            Jt.ExecutionResult result = Jt.endExecution();

            // Collect component registrations
            Set<String> registeredTypes = new HashSet<>();
            List<String> registrations = new ArrayList<>();

            for (JtComponent<?> component : result.jtComponents) {
                String componentType = component.getClass().getSimpleName();
                if (!registeredTypes.contains(componentType) && component.needsRegistration()) {
                    registrations.add(component.register());
                    registeredTypes.add(componentType);
                }
            }

            // Collect component HTML
            List<String> componentHtml = new ArrayList<>();
            for (JtComponent<?> component : result.jtComponents) {
                componentHtml.add(component.render());
            }

            // Send to frontend
            Map<String, Object> response = new HashMap<>();
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
        } catch (Exception e) {
            Jt.endExecution(); // Clean up context
            throw e;
        }
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
            java.lang.reflect.Field field = Jt.class.getDeclaredField("sessions");
            field.setAccessible(true);
            Map<String, SessionState> sessions = (Map<String, SessionState>) field.get(null);
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
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            margin: 0;
                            padding: 20px;
                            background-color: #f5f5f5;
                        }
                        .container {
                            max-width: 800px;
                            margin: 0 auto;
                            background: white;
                            padding: 20px;
                            border-radius: 8px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        .error {
                            background-color: #fee;
                            border: 1px solid #fcc;
                            color: #c00;
                            padding: 10px;
                            border-radius: 4px;
                            margin: 10px 0;
                        }
                    </style>
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
                                    message.registrations.forEach(registration => {
                                        if (!document.getElementById('jeamlit-registrations')) {
                                            const div = document.createElement('div');
                                            div.id = 'jeamlit-registrations';
                                            div.style.display = 'none';
                                            document.body.appendChild(div);
                                        }
                                        document.getElementById('jeamlit-registrations').innerHTML += registration;
                                        // Execute any scripts in the registration
                                        const scripts = document.getElementById('jeamlit-registrations').getElementsByTagName('script');
                                        for (let script of scripts) {
                                            if (!script.executed) {
                                                eval(script.innerHTML);
                                                script.executed = true;
                                            }
                                        }
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
                """.formatted(port);
    }
}