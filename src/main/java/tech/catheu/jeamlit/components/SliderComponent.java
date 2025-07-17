package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

public class SliderComponent extends JtComponent<Integer> {
    private final String label;
    private final int min;
    private final int max;
    private final int defaultValue;
    private final String help;
    private final boolean disabled;
    
    private SliderComponent(Builder builder) {
        this.label = builder.label;
        this.min = builder.min;
        this.max = builder.max;
        this.defaultValue = builder.defaultValue;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.currentValue = builder.defaultValue;
    }
    
    public static class Builder implements JtComponentBuilder<SliderComponent> {
        private final String label;
        private int min = 0;
        private int max = 100;
        private int defaultValue = 50;
        private String help = null;
        private boolean disabled = false;
        
        public Builder(String label) {
            this.label = label;
        }
        
        public Builder min(int min) {
            this.min = min;
            return this;
        }
        
        public Builder max(int max) {
            this.max = max;
            return this;
        }
        
        public Builder value(int value) {
            this.defaultValue = value;
            return this;
        }
        
        public Builder help(String help) {
            this.help = help;
            return this;
        }
        
        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }
        
        @Override
        public SliderComponent build() {
            return new SliderComponent(this);
        }
    }
    
    @Override
    public String register() {
        // Share the same registration as ButtonComponent - make sure emit function is available
        return """
            <script>
            if (!window.jeamlit) {
                window.jeamlit = {};
            }
            if (!window.jeamlit.emit) {
                window.jeamlit.emit = function(componentId, value) {
                    console.log('Component action:', componentId, value);
                    if (window.ws && window.ws.readyState === WebSocket.OPEN) {
                        window.ws.send(JSON.stringify({
                            type: 'component_update',
                            componentId: componentId,
                            value: value
                        }));
                    } else {
                        console.error('WebSocket not available');
                    }
                };
            }
            </script>
            """;
    }
    
    @Override
    public String render() {
        String helpText = help != null ? String.format("<div style=\"font-size: 0.9em; color: #666; margin-top: 5px;\">%s</div>", escapeHtml(help)) : "";
        String disabledAttr = disabled ? "disabled" : "";
        
        return String.format("""
            <div style="margin: 10px 0;">
                <label>%s: <span id="slider-value-%s">%d</span></label>
                <input type="range" 
                       min="%d" 
                       max="%d" 
                       value="%d" 
                       %s
                       oninput="document.getElementById('slider-value-%s').textContent = this.value; console.log('Slider input:', this.value)"
                       onchange="console.log('Slider changed:', this.value); window.jeamlit.emit('%s', parseInt(this.value))">
                %s
            </div>
            """, 
            escapeHtml(label), getId(), currentValue,
            min, max, currentValue,
            disabledAttr,
            getId(),
            getId(),
            helpText
        );
    }
    
    @Override
    protected TypeReference<Integer> getTypeReference() {
        return new TypeReference<>() {};
    }
    
    @Override
    protected Integer castAndValidate(Object rawValue) {
        Integer value = super.castAndValidate(rawValue);
        // Clamp to valid range
        return Math.max(min, Math.min(max, value));
    }
    
    @Override
    protected void resetIfNeeded() {
        // Slider keeps its value - no reset needed
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