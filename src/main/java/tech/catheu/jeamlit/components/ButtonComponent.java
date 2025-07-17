package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

public class ButtonComponent extends JtComponent<Boolean> {
    private final String label;
    
    private ButtonComponent(final Builder builder) {
        this.label = builder.label;
        this.currentValue = false;
    }
    
    public static class Builder implements JtComponentBuilder<ButtonComponent> {
        private String label;
        
        public Builder(String label) {
            this.label = label;
        }
        
        @Override
        public ButtonComponent build() {
            return new ButtonComponent(this);
        }
    }
    
    @Override
    public String register() {
        // Only register the jeamlit.emit function once
        return """
            <script>
            if (!window.jeamlit) {
                window.jeamlit = {};
            }
            if (!window.jeamlit.emit) {
                window.jeamlit.emit = function(componentId, value) {
                    console.log('Button clicked:', componentId, value);
                    console.log('WebSocket state:', window.ws ? window.ws.readyState : 'no ws');
                    if (window.ws && window.ws.readyState === WebSocket.OPEN) {
                        console.log('Sending component_update message');
                        window.ws.send(JSON.stringify({
                            type: 'component_update',
                            componentId: componentId,
                            value: value
                        }));
                    } else {
                        console.error('WebSocket not available or not open');
                    }
                };
            }
            </script>
            """;
    }
    
    @Override
    public String render() {
        return String.format(
            "<button onclick=\"window.jeamlit.emit('%s', true)\">%s</button>",
            getId(), 
            escapeHtml(label)
        );
    }
    
    @Override
    protected TypeReference<Boolean> getTypeReference() {
        return new TypeReference<Boolean>() {};
    }
    
    @Override
    protected void resetIfNeeded() {
        // Button is momentary - reset to false after reading
        currentValue = false;
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}