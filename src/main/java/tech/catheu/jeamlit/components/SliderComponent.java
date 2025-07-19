package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import java.util.function.Consumer;
import static tech.catheu.jeamlit.components.JsConstants.*;

public class SliderComponent extends JtComponent<Double> {
    private final String label;
    private final double min;
    private final double max;
    private final double value;
    private final double rangeEndValue; // For range sliders
    private final double step;
    private final String format;
    private final String help;
    private final boolean disabled;
    private final String labelVisibility;
    private final boolean range;
    private final String key;
    private final Consumer<SliderComponent> onChange;
    private final String width;
    
    private SliderComponent(Builder builder) {
        this.label = builder.label;
        this.min = builder.min;
        this.max = builder.max;
        this.value = builder.value;
        this.rangeEndValue = builder.rangeEndValue;
        this.step = builder.step;
        this.format = builder.format;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.range = builder.range;
        this.key = builder.key;
        this.onChange = builder.onChange;
        this.width = builder.width;
        this.currentValue = builder.value;
    }
    
    @SuppressWarnings("unused")
    public static class Builder implements JtComponentBuilder<SliderComponent> {
        private final String label;
        private double min = 0.0;
        private double max = 100.0;
        private double value = 0.0; // Will be set to min if not specified
        private double rangeEndValue = 0.0; // For range sliders
        private double step = 1.0;
        private String format = null;
        private String help = null;
        private boolean disabled = false;
        private String labelVisibility = "visible";
        private boolean range = false;
        private String key;
        private Consumer<SliderComponent> onChange;
        private String width;
        
        public Builder(String label) {
            this.label = label;
        }
        
        public Builder min(double min) {
            this.min = min;
            return this;
        }
        
        public Builder max(double max) {
            this.max = max;
            return this;
        }
        
        public Builder value(double value) {
            this.value = value;
            return this;
        }
        
        public Builder value(double startValue, double endValue) {
            this.value = startValue;
            this.rangeEndValue = endValue;
            this.range = true; // Automatically set range to true
            return this;
        }
        
        public Builder step(double step) {
            this.step = step;
            return this;
        }
        
        public Builder format(String format) {
            this.format = format;
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
        
        public Builder labelVisibility(String labelVisibility) {
            if (labelVisibility != null && !labelVisibility.equals("visible") && 
                !labelVisibility.equals("hidden") && !labelVisibility.equals("collapsed")) {
                throw new IllegalArgumentException("label_visibility must be 'visible', 'hidden', or 'collapsed'. Got: " + labelVisibility);
            }
            this.labelVisibility = labelVisibility;
            return this;
        }
        
        public Builder range(boolean range) {
            this.range = range;
            return this;
        }
        
        public Builder key(String key) {
            this.key = key;
            return this;
        }
        
        public Builder onChange(Consumer<SliderComponent> onChange) {
            this.onChange = onChange;
            return this;
        }
        
        public Builder width(String width) {
            if (width != null && !width.equals("stretch") && !width.matches("\\d+")) {
                throw new IllegalArgumentException("width must be 'stretch' or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }
        
        @Override
        public SliderComponent build() {
            // Add comment about args/kwargs not being implemented
            // Note: args/kwargs equivalent (varargs and Map parameters) not implemented
            
            // Set value to min if not explicitly set (matching Streamlit behavior)
            if (this.value == 0.0 && this.min != 0.0) {
                this.value = this.min;
            }
            
            // Set range end value for range sliders
            if (this.range && this.rangeEndValue == 0.0) {
                this.rangeEndValue = this.max;
            }
            
            // Validate parameters
            if (this.label == null || this.label.trim().isEmpty()) {
                throw new IllegalArgumentException("Label cannot be null or empty");
            }
            if (this.min >= this.max) {
                throw new IllegalArgumentException("min_value must be less than max_value");
            }
            if (this.step <= 0) {
                throw new IllegalArgumentException("step must be positive");
            }
            if (this.value < this.min || this.value > this.max) {
                throw new IllegalArgumentException("value must be between min_value and max_value");
            }
            
            // Additional validation for range sliders
            if (this.range) {
                if (this.rangeEndValue < this.min || this.rangeEndValue > this.max) {
                    throw new IllegalArgumentException("range end value must be between min_value and max_value");
                }
                if (this.value > this.rangeEndValue) {
                    throw new IllegalArgumentException("range start value must be less than or equal to range end value");
                }
            }
            
            return new SliderComponent(this);
        }
    }
    
    @Override
    public String register() {
        return """
                <script type="module">
                import { LitElement, html, css } from '%s';""".formatted(LIT_DEPENDENCY) +
                """
                // WebSocket communication setup
                if (!window.jeamlit) {
                    window.jeamlit = {};
                }
                if (!window.jeamlit.emit) {
                    window.jeamlit.emit = function(componentId, value) {
                        console.log('Slider changed:', componentId, value);
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
                
                class JtSlider extends LitElement {
                    static styles = css`
                        :host {
                            display: block;
                            margin: var(--jt-spacing-lg) 0;
                        }
                        
                        .slider-container {
                            display: flex;
                            align-items: center;
                            gap: var(--jt-spacing-sm);
                        }
                        
                        .slider-content {
                            flex: 1;
                            display: flex;
                            flex-direction: column;
                            gap: var(--jt-spacing-sm);
                        }
                        
                        .label {
                            font-family: var(--jt-font-family);
                            font-size: var(--jt-font-size-sm);
                            font-weight: var(--jt-font-weight-medium);
                            color: var(--jt-text-primary);
                            margin: 0;
                        }
                        
                        .label.hidden {
                            display: none;
                        }
                        
                        /* Markdown formatting in label */
                        .label em {
                            font-style: italic;
                        }
                        
                        .label strong {
                            font-weight: var(--jt-font-weight-bold);
                        }
                        
                        .label code {
                            background-color: var(--jt-bg-tertiary);
                            padding: 0.1em 0.3em;
                            border-radius: var(--jt-border-radius-sm);
                            font-family: var(--jt-font-family-mono);
                            font-size: 0.9em;
                        }
                        
                        .label a {
                            color: var(--jt-theme-color);
                            text-decoration: none;
                        }
                        
                        .label a:hover {
                            text-decoration: underline;
                        }
                        
                        .label del {
                            text-decoration: line-through;
                        }
                        
                        .slider-track-container {
                            position: relative;
                            display: block;
                            width: 100%;
                        }
                        
                        .slider-track {
                            position: relative;
                            height: 6px;
                            width: 100%;
                            background: var(--jt-bg-tertiary);
                            border-radius: 3px;
                            overflow: visible;
                            margin: 20px 0 0px 0;
                        }
                        
                        .slider-progress {
                            position: absolute;
                            top: 0;
                            left: 0;
                            height: 100%;
                            background: var(--jt-theme-color);
                            border-radius: 3px;
                        }
                        
                        .slider-input:hover ~ .slider-progress {
                            background: var(--jt-theme-hover);
                        }
                        
                        .slider-input:active ~ .slider-progress {
                            background: var(--jt-theme-active);
                        }
                        
                        .slider-input {
                            position: absolute;
                            top: 50%;
                            left: 0;
                            width: 100%;
                            height: 30px;
                            transform: translateY(-50%);
                            opacity: 0;
                            cursor: pointer;
                            margin: 0;
                            padding: 0;
                            border: none;
                            background: transparent;
                            z-index: 5;
                            -webkit-appearance: none;
                            appearance: none;
                        }
                        
                        .slider-input::-webkit-slider-thumb {
                            -webkit-appearance: none;
                            appearance: none;
                            width: 30px;
                            height: 30px;
                            background: transparent;
                            cursor: pointer;
                        }
                        
                        .slider-input::-moz-range-thumb {
                            width: 30px;
                            height: 30px;
                            background: transparent;
                            cursor: pointer;
                            border: none;
                        }
                        
                        .slider-input:disabled {
                            cursor: not-allowed;
                        }
                        
                        .slider-thumb {
                            position: absolute;
                            top: 50%;
                            width: 18px;
                            height: 18px;
                            background: var(--jt-theme-color);
                            border: 2px solid var(--jt-bg-primary);
                            border-radius: 50%;
                            transform: translate(-50%, -50%);
                            box-shadow: var(--jt-shadow-sm);
                            pointer-events: none;
                        }
                        
                        .slider-input:hover ~ .slider-thumb {
                            transform: translate(-50%, -50%) scale(1.1);
                            background: var(--jt-theme-hover);
                            box-shadow: var(--jt-shadow);
                        }
                        
                        .slider-input:active ~ .slider-thumb {
                            transform: translate(-50%, -50%) scale(1.2);
                            background: var(--jt-theme-active);
                            box-shadow: var(--jt-shadow-lg);
                        }
                        
                        .slider-input:disabled ~ .slider-thumb {
                            opacity: 0.5;
                            cursor: not-allowed;
                        }
                        
                        .value-display {
                            position: absolute;
                            top: -26px;
                            font-family: var(--jt-font-family-mono);
                            font-size: var(--jt-font-size-sm);
                            color: var(--jt-theme-color);
                            text-align: center;
                            white-space: nowrap;
                            transform: translateX(-50%);
                            z-index: 10;
                        }
                        
                        .min-max-labels {
                            display: flex;
                            justify-content: space-between;
                            font-size: var(--jt-font-size-s);
                            color: var(--jt-text-muted);
                            padding: 0 4px;
                        }
                    `;
                    
                    static properties = {
                        label: { type: String },
                        min: { type: Number },
                        max: { type: Number },
                        value: { type: Number },
                        step: { type: Number },
                        format: { type: String },
                        help: { type: String },
                        disabled: { type: Boolean },
                        labelVisibility: { type: String, attribute: 'label-visibility' },
                        range: { type: Boolean },
                        rangeEndValue: { type: Number, attribute: 'range-end-value' },
                        componentId: { type: String, attribute: 'component-id' }
                    };
                    
                    constructor() {
                        super();
                        this.min = 0;
                        this.max = 100;
                        this.value = 50;
                        this.step = 1;
                        this.disabled = false;
                        this.labelVisibility = 'visible';
                        this.range = false;
                        this.rangeEndValue = 100;
                        
                        // Local state for smooth dragging
                        this.dragging = false;
                        this.tempValue = this.value;
                    }
                    
                    updated(changedProperties) {
                        super.updated(changedProperties);
                        
                        // Update tempValue when value changes from backend (but not during dragging)
                        if (changedProperties.has('value') && !this.dragging) {
                            this.tempValue = this.value;
                        }
                    }
                    
                    formatText(label) {
                        if (!label) return '';
                        
                        // Enhanced Markdown formatting
                        return label
                            // Bold
                            .replace(/\\*\\*(.*?)\\*\\*/g, '<strong>$1</strong>')
                            // Italic
                            .replace(/\\*(.*?)\\*/g, '<em>$1</em>')
                            // Code
                            .replace(/`(.*?)`/g, '<code>$1</code>')
                            // Links
                            .replace(/\\[(.*?)\\]\\((.*?)\\)/g, '<a href="$2">$1</a>')
                            // Basic color support :color[text]
                            .replace(/:([a-zA-Z]+)\\[(.*?)\\]/g, '<span style="color: $1">$2</span>')
                            // Strikethrough
                            .replace(/~~(.*?)~~/g, '<del>$1</del>');
                    }
                    
                    formatValue(value) {
                        if (this.format) {
                            // Simple format processing
                            if (this.format === '%') {
                                return `${(value * 100).toFixed(0)}%`;
                            }
                            if (this.format.startsWith('.')) {
                                const decimals = parseInt(this.format.slice(1)) || 0;
                                return value.toFixed(decimals);
                            }
                            return this.format.replace('%s', value.toString());
                        }
                        
                        // Auto-format based on step
                        if (this.step < 1) {
                            const decimals = this.step.toString().split('.')[1]?.length || 1;
                            return value.toFixed(decimals);
                        }
                        
                        return Math.round(value).toString();
                    }
                    
                    getThumbPosition(value) {
                        const percentage = ((value - this.min) / (this.max - this.min)) * 100;
                        return Math.max(0, Math.min(100, percentage));
                    }
                    
                    handleInput(e) {
                        if (this.disabled) return;
                        
                        const newValue = parseFloat(e.target.value);
                        this.dragging = true;
                        this.tempValue = newValue;
                        
                        // Update visual state only, no WebSocket communication
                        this.requestUpdate();
                    }
                    
                    handleChange(e) {
                        if (this.disabled) return;
                        
                        const newValue = parseFloat(e.target.value);
                        this.dragging = false;
                        this.tempValue = newValue;
                        this.value = newValue;
                        
                        // Emit value change only when drag ends
                        if (window.jeamlit && window.jeamlit.emit) {
                            window.jeamlit.emit(this.componentId, newValue);
                        }
                    }
                    
                    render() {
                        // Use tempValue during dragging for smooth visual feedback
                        const currentValue = this.dragging ? this.tempValue : this.value;
                        const thumbPosition = this.getThumbPosition(currentValue);
                        const progressWidth = thumbPosition;
                        const formattedLabel = this.formatText(this.label || '');
                        
                        // TODO: Implement dual-thumb range slider UI
                        // For now, range sliders will display as single sliders with the start value
                        // Full range slider implementation requires:
                        // - Dual thumb rendering and positioning
                        // - Range progress bar between thumbs
                        // - Independent thumb dragging
                        // - Range value emission to WebSocket
                        
                        return html`
                            <div class="slider-container">
                                <div class="slider-content">
                                    ${this.labelVisibility !== 'hidden' ? html`
                                        <div class="label ${this.labelVisibility === 'collapsed' ? 'hidden' : ''}" 
                                             .innerHTML="${formattedLabel}">
                                        </div>
                                    ` : ''}
                                    
                                    <div class="slider-track-container">
                                        <div class="slider-track">
                                            <div class="slider-progress" style="width: ${progressWidth}%"></div>
                                            <input
                                                type="range"
                                                class="slider-input"
                                                min="${this.min}"
                                                max="${this.max}"
                                                step="${this.step}"
                                                .value="${currentValue}"
                                                ?disabled="${this.disabled}"
                                                @input="${this.handleInput}"
                                                @change="${this.handleChange}"
                                            />
                                            <div class="slider-thumb" style="left: ${thumbPosition}%"></div>
                                            <div class="value-display" style="left: ${thumbPosition}%">
                                                ${this.formatValue(currentValue)}
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <div class="min-max-labels">
                                        <span>${this.formatValue(this.min)}</span>
                                        <span>${this.formatValue(this.max)}</span>
                                    </div>
                                </div>
                                
                                ${this.help ? html`
                                    <jt-tooltip text="${this.help}"></jt-tooltip>
                                ` : ''}
                            </div>
                        `;
                    }
                }
                
                customElements.define('jt-slider', JtSlider);
                </script>
                """;
    }
    
    @Override
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("<jt-slider");
        sb.append(" label=\"").append(escapeHtml(label)).append("\"");
        sb.append(" component-id=\"").append(getId()).append("\"");
        sb.append(" min=\"").append(min).append("\"");
        sb.append(" max=\"").append(max).append("\"");
        sb.append(" value=\"").append(currentValue).append("\"");
        sb.append(" step=\"").append(step).append("\"");
        
        if (format != null && !format.isEmpty()) {
            sb.append(" format=\"").append(escapeHtml(format)).append("\"");
        }
        
        if (help != null && !help.isEmpty()) {
            sb.append(" help=\"").append(escapeHtml(help)).append("\"");
        }
        
        if (disabled) {
            sb.append(" disabled");
        }
        
        if (labelVisibility != null && !labelVisibility.equals("visible")) {
            sb.append(" label-visibility=\"").append(escapeHtml(labelVisibility)).append("\"");
        }
        
        if (range) {
            sb.append(" range");
            sb.append(" range-end-value=\"").append(rangeEndValue).append("\"");
        }
        
        sb.append("></jt-slider>");
        return sb.toString();
    }
    
    @Override
    protected TypeReference<Double> getTypeReference() {
        return new TypeReference<>() {};
    }
    
    @Override
    protected Double castAndValidate(Object rawValue) {
        Double value = super.castAndValidate(rawValue);
        // Clamp to valid range
        return Math.max(min, Math.min(max, value));
    }
    
    @Override
    protected void resetIfNeeded() {
        // Slider keeps its value - no reset needed
    }
    
    public void executeCallback() {
        if (onChange != null) {
            onChange.accept(this);
        }
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