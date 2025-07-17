package tech.catheu.jeamlit.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import tech.catheu.jeamlit.components.ButtonComponent;
import tech.catheu.jeamlit.components.SliderComponent;
import tech.catheu.jeamlit.components.TitleComponent;
import tech.catheu.jeamlit.components.TextComponent;

// main interface for developers - should only contain syntactic sugar, no core logic
public class Jt {
    private static final ThreadLocal<ExecutionContext> currentContext = new ThreadLocal<>();
    private static final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public static void beginExecution(String sessionId) {
        ExecutionContext context = new ExecutionContext(sessionId);
        currentContext.set(context);
        
        if (!sessions.containsKey(sessionId)) {
            sessions.put(sessionId, new SessionState(sessionId));
        }
    }

    static Map<String, SessionState> getSessions() {
        return sessions;
    }
    
    public static ExecutionResult endExecution() {
        ExecutionContext context = currentContext.get();
        if (context == null) {
            throw new IllegalStateException("No active execution context");
        }
        
        SessionState session = sessions.get(context.getSessionId());
        session.updateWidgetStates(context.getWidgetStates());
        
        ExecutionResult result = new ExecutionResult(
            context.getJtComponents(),
            context.getSessionId()
        );
        
        currentContext.remove();
        return result;
    }
    
    public static class ExecutionResult {
        public final List<JtComponent<?>> jtComponents;
        public final String sessionId;
        
        public ExecutionResult(List<JtComponent<?>> jtComponents, String sessionId) {
            this.jtComponents = jtComponents;
            this.sessionId = sessionId;
        }
    }
    
    private static ExecutionContext getContext() {
        ExecutionContext context = currentContext.get();
        if (context == null) {
            throw new IllegalStateException("Jeamlit methods must be called within an execution context");
        }
        return context;
    }
    
    public static String text(final String text) {
        ExecutionContext context = getContext();
        TextComponent textComponent = new TextComponent.Builder(text).build();
        context.addJtComponent(textComponent);
        return textComponent.returnValue();
    }
    
    public static String title(final String text) {
        ExecutionContext context = getContext();
        TitleComponent titleComponent = new TitleComponent.Builder(text).build();
        context.addJtComponent(titleComponent);
        return titleComponent.returnValue();
    }
    
    public static boolean button(String label) {
        ExecutionContext context = getContext();
        String key = context.generateKey("button", label);
        return button(label, key);
    }
    
    public static boolean button(String label, String key) {
        ExecutionContext context = getContext();
        
        // Use new component system with explicit key collision detection
        ButtonComponent button = context.getComponent(key, 
            () -> new ButtonComponent.Builder(label).build(), true);
        
        context.addJtComponent(button);
        return button.returnValue();
    }
    
    public static int slider(String label) {
        return slider(label, 0, 100, 50);
    }
    
    public static int slider(String label, int min, int max) {
        return slider(label, min, max, min);
    }
    
    public static int slider(String label, int min, int max, int defaultValue) {
        ExecutionContext context = getContext();
        String key = context.generateKey("slider", label, String.valueOf(min), String.valueOf(max), String.valueOf(defaultValue));
        return slider(label, min, max, defaultValue, key);
    }

    public static int slider(String label, int min, int max, int defaultValue, String key) {
        ExecutionContext context = getContext();

        // Use new component system with explicit key collision detection
        SliderComponent slider = context.getComponent(key,
            () -> new SliderComponent.Builder(label).min(min).max(max).value(defaultValue).help(null).disabled(false).build(), true);

        context.addJtComponent(slider);
        return slider.returnValue();
    }
    
    public static TypedSessionState sessionState() {
        ExecutionContext context = getContext();
        SessionState session = sessions.get(context.getSessionId());
        return new TypedSessionState(session.getUserState());
    }
    
    public static void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    // Package-private method for server to access context
    static ExecutionContext getCurrentContext() {
        return currentContext.get();
    }
}