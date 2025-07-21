package tech.catheu.jeamlit.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionState {
    private final Map<String, Object> userState = new ConcurrentHashMap<>();
    private final Map<String, Object> widgetState = new ConcurrentHashMap<>();

    public SessionState() {
    }

    public Map<String, Object> getUserState() {
        return userState;
    }

    public Object getWidgetState(String widgetId) {
        return widgetState.get(widgetId);
    }

    public void updateWidgetStates(Map<String, Object> newStates) {
        widgetState.putAll(newStates);
    }

    public Map<String, Object> getWidgetStates() {
        return widgetState;
    }

    // FIXME CYRIL TOO MANY function to do the same thing getWidgetState getWidgetStates updateWidgetStates
    //   also, userState and widgetState is actually the same thing for streamlint --> let's make sure it stays that way
    //   note that Modifying the value of a widget via the Session state API, after instantiating it, is not allowed and will raise a StreamlitAPIException.
    //      slider = st.slider(
    //          label='My Slider', min_value=1,
    //          max_value=10, value=5, key='my_slider')
    //
    //      st.session_state.my_slider = 7
    // https://docs.streamlit.io/develop/api-reference/caching-and-state/st.session_state
}
