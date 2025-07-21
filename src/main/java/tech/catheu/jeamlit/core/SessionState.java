package tech.catheu.jeamlit.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// should just become a plain map where it's forbidden to update keys of components ?
public class SessionState {
    private final Map<String, Object> userState = new ConcurrentHashMap<>();
    private final Map<String, Object> widgetState = new ConcurrentHashMap<>();

    public SessionState() {}

    public Map<String, Object> getUserState() {
        return userState;
    }

    public Map<String, Object> getWidgetStates() {
        return widgetState;
    }
}
