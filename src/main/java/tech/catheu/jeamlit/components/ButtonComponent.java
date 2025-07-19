package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import java.util.function.Consumer;
import static tech.catheu.jeamlit.components.JsConstants.*;

public class ButtonComponent extends JtComponent<Boolean> {
    private final String label;
    private final String type;
    private final String icon;
    private final String help;
    private final boolean disabled;
    private final boolean useContainerWidth;
    private final String key;
    private final Consumer<ButtonComponent> onClick;
    
    private ButtonComponent(final Builder builder) {
        this.label = builder.label;
        this.type = builder.type;
        this.icon = builder.icon;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.useContainerWidth = builder.useContainerWidth;
        this.key = builder.key;
        this.onClick = builder.onClick;
        this.currentValue = false;
    }
    
    public static class Builder implements JtComponentBuilder<ButtonComponent> {
        private String label;
        private String type = "secondary";
        private String icon;
        private String help;
        private boolean disabled = false;
        private boolean useContainerWidth = false;
        private String key;
        private Consumer<ButtonComponent> onClick;
        
        public Builder(String label) {
            this.label = label;
        }
        
        public Builder type(String type) {
            if (type != null && !type.equals("primary") && !type.equals("secondary") && !type.equals("tertiary")) {
                throw new IllegalArgumentException("Button type must be 'primary', 'secondary', or 'tertiary'. Got: " + type);
            }
            this.type = type;
            return this;
        }
        
        public Builder icon(String icon) {
            this.icon = icon;
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
        
        public Builder useContainerWidth(boolean useContainerWidth) {
            this.useContainerWidth = useContainerWidth;
            return this;
        }
        
        public Builder key(String key) {
            this.key = key;
            return this;
        }
        
        public Builder onClick(Consumer<ButtonComponent> onClick) {
            this.onClick = onClick;
            return this;
        }
        
        @Override
        public ButtonComponent build() {
            // Validate required parameters
            if (label == null || label.trim().isEmpty()) {
                throw new IllegalArgumentException("Button label cannot be null or empty");
            }
            return new ButtonComponent(this);
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
                    console.log('Button clicked:', componentId, value);
                    if (window.ws && window.ws.readyState === WebSocket.OPEN) {
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
            
            class JtButton extends LitElement {
                static styles = css`
                    :host {
                        display: block;
                        margin: var(--jt-spacing-md) 0;
                    }
                    
                    .button-container {
                        display: flex;
                        align-items: center;
                        gap: var(--jt-spacing-sm);
                    }
                    
                    .button {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        gap: var(--jt-spacing-sm);
                        
                        padding: var(--jt-spacing-sm) var(--jt-spacing-lg);
                        border: 1px solid transparent;
                        border-radius: var(--jt-border-radius);
                        
                        font-family: var(--jt-font-family);
                        font-size: var(--jt-font-size-base);
                        font-weight: var(--jt-font-weight-medium);
                        line-height: var(--jt-line-height-tight);
                        text-decoration: none;
                        white-space: nowrap;
                        
                        cursor: pointer;
                        transition: all var(--jt-transition-fast);
                        
                        position: relative;
                        overflow: hidden;
                    }
                    
                    :host([use-container-width]) .button {
                        width: 100%;
                    }
                    
                    .button:focus {
                        outline: 2px solid var(--jt-primary-color);
                        outline-offset: 2px;
                    }
                    
                    .button:disabled {
                        opacity: 0.5;
                        cursor: not-allowed;
                        pointer-events: none;
                    }
                    
                    /* Button Types */
                    .button.primary {
                        background-color: var(--jt-theme-color);
                        color: var(--jt-text-white);
                        border-color: var(--jt-theme-color);
                    }
                    
                    .button.primary:hover:not(:disabled) {
                        background-color: var(--jt-theme-hover);
                        border-color: var(--jt-theme-hover);
                        transform: translateY(-1px);
                        box-shadow: var(--jt-shadow);
                    }
                    
                    .button.primary:active:not(:disabled) {
                        background-color: var(--jt-theme-active);
                        border-color: var(--jt-theme-active);
                        transform: translateY(0);
                        box-shadow: var(--jt-shadow-sm);
                    }
                    
                    .button.secondary {
                        background-color: var(--jt-bg-primary);
                        color: var(--jt-text-primary);
                        border-color: var(--jt-border-color);
                    }
                    
                    .button.secondary:hover:not(:disabled) {
                        background-color: var(--jt-bg-primary);
                        border-color: var(--jt-theme-color);
                        color: var(--jt-theme-color);
                        transform: translateY(-1px);
                        box-shadow: var(--jt-shadow);
                    }
                    
                    .button.secondary:active:not(:disabled) {
                        background-color: var(--jt-bg-secondary);
                        border-color: var(--jt-theme-active);
                        color: var(--jt-theme-active);
                        transform: translateY(0);
                        box-shadow: var(--jt-shadow-sm);
                    }
                    
                    .button.tertiary {
                        background-color: transparent;
                        color: var(--jt-text-secondary);
                        border-color: transparent;
                    }
                    
                    .button.tertiary:hover:not(:disabled) {
                        background-color: transparent;
                        color: var(--jt-theme-color);
                        border-color: transparent;
                    }
                    
                    .button.tertiary:active:not(:disabled) {
                        background-color: var(--jt-bg-secondary);
                        color: var(--jt-theme-active);
                    }
                    
                    /* Icon styling */
                    .icon {
                        font-size: var(--jt-font-size-lg);
                        font-family: 'Material Symbols Rounded';
                        font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
                    }
                    
                    .emoji-icon {
                        font-size: var(--jt-font-size-base);
                    }
                    
                    /* Ripple effect */
                    .button::after {
                        content: '';
                        position: absolute;
                        top: 50%;
                        left: 50%;
                        width: 0;
                        height: 0;
                        border-radius: 50%;
                        background: rgba(255, 255, 255, 0.3);
                        transform: translate(-50%, -50%);
                        transition: width 0.3s, height 0.3s;
                    }
                    
                    .button:active::after {
                        width: 200px;
                        height: 200px;
                    }
                    
                    /* Markdown formatting in button text */
                    .button em {
                        font-style: italic;
                    }
                    
                    .button strong {
                        font-weight: var(--jt-font-weight-bold);
                    }
                    
                    .button code {
                        background-color: rgba(255, 255, 255, 0.2);
                        padding: 0.1em 0.3em;
                        border-radius: var(--jt-border-radius-sm);
                        font-family: var(--jt-font-family-mono);
                        font-size: 0.9em;
                    }
                    
                    .button.primary code {
                        background-color: rgba(255, 255, 255, 0.3);
                    }
                    
                    .button.secondary code, .button.tertiary code {
                        background-color: rgba(0, 0, 0, 0.1);
                    }
                    
                    .button a {
                        color: inherit;
                        text-decoration: underline;
                    }
                    
                    .button del {
                        text-decoration: line-through;
                    }
                `;
                
                static properties = {
                    label: { type: String },
                    type: { type: String },
                    icon: { type: String },
                    help: { type: String },
                    disabled: { type: Boolean },
                    useContainerWidth: { type: Boolean, attribute: 'use-container-width' },
                    componentId: { type: String, attribute: 'component-id' }
                };
                
                constructor() {
                    super();
                    this.type = 'secondary';
                    this.disabled = false;
                    this.useContainerWidth = false;
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
                
                isEmoji(str) {
                    // Simple emoji detection
                    return str && str.length <= 2 && /\\p{Emoji}/u.test(str);
                }
                
                handleClick(e) {
                    if (this.disabled) return;
                    
                    // Create ripple effect
                    const button = e.currentTarget;
                    const rect = button.getBoundingClientRect();
                    const size = Math.max(rect.width, rect.height);
                    const x = e.clientX - rect.left - size / 2;
                    const y = e.clientY - rect.top - size / 2;
                    
                    const ripple = document.createElement('span');
                    ripple.style.cssText = `
                        position: absolute;
                        border-radius: 50%;
                        background: rgba(255, 255, 255, 0.3);
                        transform: scale(0);
                        animation: ripple 0.6s linear;
                        left: ${x}px;
                        top: ${y}px;
                        width: ${size}px;
                        height: ${size}px;
                    `;
                    
                    button.appendChild(ripple);
                    setTimeout(() => ripple.remove(), 600);
                    
                    // Emit button click
                    if (window.jeamlit && window.jeamlit.emit) {
                        window.jeamlit.emit(this.componentId, true);
                    }
                }
                
                render() {
                    const iconContent = this.icon ? (
                        this.isEmoji(this.icon) ? 
                            html`<span class="emoji-icon">${this.icon}</span>` :
                            html`<span class="icon">${this.icon}</span>`
                    ) : null;
                    
                    const formattedLabel = this.formatText(this.label || '');
                    
                    return html`
                        <style>
                            @keyframes ripple {
                                to {
                                    transform: scale(4);
                                    opacity: 0;
                                }
                            }
                        </style>
                        <div class="button-container">
                            <button 
                                class="button ${this.type}"
                                ?disabled="${this.disabled}"
                                @click="${this.handleClick}"
                                type="button">
                                ${iconContent}
                                <span .innerHTML="${formattedLabel}"></span>
                            </button>
                            ${this.help ? html`
                                <jt-tooltip text="${this.help}"></jt-tooltip>
                            ` : ''}
                        </div>
                    `;
                }
            }
            
            customElements.define('jt-button', JtButton);
            </script>
            """;
    }
    
    @Override
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("<jt-button");
        sb.append(" label=\"").append(escapeHtml(label)).append("\"");
        sb.append(" component-id=\"").append(getId()).append("\"");
        
        if (type != null && !type.equals("secondary")) {
            sb.append(" type=\"").append(escapeHtml(type)).append("\"");
        }
        
        if (icon != null && !icon.isEmpty()) {
            sb.append(" icon=\"").append(escapeHtml(icon)).append("\"");
        }
        
        if (help != null && !help.isEmpty()) {
            sb.append(" help=\"").append(escapeHtml(help)).append("\"");
        }
        
        if (disabled) {
            sb.append(" disabled");
        }
        
        if (useContainerWidth) {
            sb.append(" use-container-width");
        }
        
        sb.append("></jt-button>");
        return sb.toString();
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
    
    public void executeCallback() {
        if (onClick != null) {
            onClick.accept(this);
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