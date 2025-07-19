package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import static tech.catheu.jeamlit.components.JsConstants.*;

public class TextComponent extends JtComponent<String> {
    private final String body;
    private final String help;
    private final String width;

    @SuppressWarnings("unused")
    public static class Builder implements JtComponentBuilder<TextComponent> {
        private String body;
        private String help;
        private String width;

        public Builder(String body) {
            this.body = body;
        }

        public Builder help(String help) {
            this.help = help;
            return this;
        }

        public Builder width(String width) {
            this.width = width;
            return this;
        }

        @Override
        public TextComponent build() {
            return new TextComponent(this);
        }
    }

    private TextComponent(Builder builder) {
        this.body = builder.body;
        this.help = builder.help;
        this.width = builder.width;
    }

    @Override
    public String returnValue() {
        return body;
    }

    @Override
    public String register() {
        return
             """
             <script type="module">
             import { LitElement, html, css } from '%s';
             
             class JtText extends LitElement {
                 static styles = css`
                     :host {
                         display: block;
                         margin: var(--jt-spacing-md) 0;
                     }
                     
                     .text-wrapper {
                         position: relative;
                         display: inline-block;
                         width: 100%%;
                     }
                     
                     .text-content {
                         font-family: var(--jt-font-family);
                         font-size: var(--jt-font-size-base);
                         line-height: var(--jt-line-height-normal);
                         color: var(--jt-text-primary);
                         margin: 0;
                         padding: 0;
                         white-space: normal;
                         word-wrap: break-word;
                         display: block;
                         width: 100%%;
                     }
                     
                     
                     .text-content.width-content {
                         width: auto;
                         display: inline-block;
                     }
                     
                     .text-content.width-stretch {
                         width: 100%%;
                     }
                     
                     .tooltip {
                         position: absolute;
                         top: 0;
                         right: -24px;
                         z-index: 10;
                     }
                 `;
                 
                 static properties = {   
                     text: { type: String }, 
                     help: { type: String },
                     width: { type: String }
                 };
                 
                 getWidthClass() {
                     if (!this.width) return '';
                     if (this.width === 'content') return 'width-content';
                     if (this.width === 'stretch') return 'width-stretch';
                     return '';
                 }
                 
                 getWidthStyle() {
                     if (!this.width) return '';
                     
                     // Handle numeric width as pixels
                     if (/^\\d+$/.test(this.width)) {
                         return `width: ${this.width}px;`;
                     }
                     
                     return '';
                 }
                 
                 render() {
                     const widthClass = this.getWidthClass();
                     const style = this.getWidthStyle();
                     
                     return html`
                         <div class="text-wrapper">
                             <div class="text-content ${widthClass}" 
                                  style="${style}">
                                 ${this.text || ''}
                             </div>
                             ${this.help ? html`
                                 <div class="tooltip">
                                     <jt-tooltip text="${this.help}"></jt-tooltip>
                                 </div>
                             ` : ''}
                         </div>
                     `;
                 }
             }
             
             customElements.define('jt-text', JtText);
             </script>
             """.formatted(LIT_DEPENDENCY);
    }

    @Override
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("<jt-text");
        sb.append(" text=\"").append(escapeHtml(body)).append("\"");

        if (help != null && !help.isEmpty()) {
            sb.append(" help=\"").append(escapeHtml(help)).append("\"");
        }

        if (width != null && !width.isEmpty()) {
            sb.append(" width=\"").append(escapeHtml(width)).append("\"");
        }

        sb.append("></jt-text>");
        return sb.toString();
    }

    @Override
    protected TypeReference<String> getTypeReference() {
        return new TypeReference<>() {};
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