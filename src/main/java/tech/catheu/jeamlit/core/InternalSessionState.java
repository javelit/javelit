package tech.catheu.jeamlit.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In streamlit, userState (free key-value storage in session_state) and
 * componentsState (widget states) are made available via the same map.
 * This can cause confusion because in most cases the components state should not be edited
 * via the map.
 * In Jeamlit, only the user state will be made available. It will have the name sessionState in
 * the public API ({@link Jt}) so that user are not confused.
 * The componentsState will be made available in another method.
 * */
public class InternalSessionState {
    // readable/writable by users
    private final Map<String, Object> userState = new ConcurrentHashMap<>();
    // not writable by users
    private final Map<String, Object> componentsState = new ConcurrentHashMap<>();

    public InternalSessionState() {
    }

    public Map<String, Object> getUserState() {
        return userState;
    }

    public Map<String, Object> getComponentsState() {
        return componentsState;
    }
}
