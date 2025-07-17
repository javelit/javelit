package tech.catheu.jeamlit.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionState {
    private final String sessionId;
    private final Map<String, Object> userState = new ConcurrentHashMap<>();
    private final Map<String, Object> widgetState = new ConcurrentHashMap<>();

    public SessionState(String sessionId) {
        this.sessionId = sessionId;
    }

    public Object getWidgetState(String widgetId) {
        return widgetState.get(widgetId);
    }

    public void updateWidgetStates(Map<String, Object> newStates) {
        widgetState.putAll(newStates);
    }

    public Map<String, Object> getUserState() {
        return userState;
    }

    public Map<String, Object> getWidgetStates() {
        return widgetState;
    }
}
