package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import static tech.catheu.jeamlit.components.JsConstants.*;

public class TitleComponent extends JtComponent<String> {
    private final String body;
    private final String anchor;
    private final String help;
    private final String width;
    
    private TitleComponent(Builder builder) {
        this.body = builder.body;
        this.anchor = builder.anchor;
        this.help = builder.help;
        this.width = builder.width;
    }
    
    public static class Builder implements JtComponentBuilder<TitleComponent> {
        private String body;
        private String anchor;
        private String help;
        private String width;
        
        public Builder(String body) {
            this.body = body;
        }
        
        public Builder anchor(String anchor) {
            this.anchor = anchor;
            return this;
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
        public TitleComponent build() {
            return new TitleComponent(this);
        }
    }
    
    @Override
    public String register() {
        return
                """
              <script type="module">
              import { LitElement, html, css } from '%s';""".formatted(LIT_DEPENDENCY) +
                        """
                          class JtTitle extends LitElement {
                               static styles = css`
                                   :host {
                                       display: block;
                                       margin: var(--jt-spacing-lg) 0;
                                   }
                                   
                                   .title-container {
                                       display: flex;
                                       align-items: center;
                                       gap: var(--jt-spacing-sm);
                                   }
                                   
                                   .title {
                                       margin: 0;
                                       font-size: var(--jt-font-size-4xl);
                                       font-weight: var(--jt-font-weight-bold);
                                       line-height: var(--jt-line-height-tight);
                                       color: var(--jt-text-primary);
                                       flex: 1;
                                       display: flex;
                                       align-items: center;
                                       gap: var(--jt-spacing-sm);
                                   }
                                   
                                   .title.width-content {
                                       flex: 0 0 auto;
                                       display: inline-flex;
                                   }
                                   
                                   .title.width-stretch {
                                       flex: 1;
                                   }
                                   
                                   .title.width-custom {
                                       flex: 0 0 auto;
                                   }
                                   
                                   .anchor-link {
                                       opacity: 0;
                                       transition: opacity var(--jt-transition-fast);
                                       color: var(--jt-text-secondary);
                                       text-decoration: none;
                                       font-size: var(--jt-font-size-lg);
                                       flex-shrink: 0;
                                   }
                                   
                                   .title:hover .anchor-link {
                                       opacity: 1;
                                   }
                                   
                                   .anchor-link:hover {
                                       color: var(--jt-primary-color);
                                   }
                                   
                                   /* Markdown-style formatting */
                                   .title em {
                                       font-style: italic;
                                   }
                                   
                                   .title strong {
                                       font-weight: var(--jt-font-weight-bold);
                                   }
                                   
                                   .title code {
                                       background-color: var(--jt-bg-tertiary);
                                       padding: 0.2em 0.4em;
                                       border-radius: var(--jt-border-radius-sm);
                                       font-family: var(--jt-font-family-mono);
                                       font-size: 0.9em;
                                   }
                               `;
                               
                               static properties = {   
                                   body: { type: String }, 
                                   anchor: { type: String },
                                   help: { type: String },
                                   width: { type: String }
                               };
                      
                               formatText(body) {
                                   if (!body) return '';
                                   
                                   // Enhanced Markdown formatting
                                   return body
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
                               
                               generateAnchor(body) {
                                   if (!body) return '';
                                   return body
                                       .toLowerCase()
                                       .replace(/[^a-z0-9\s-]/g, '')
                                       .replace(/\s+/g, '-')
                                       .replace(/-+/g, '-')
                                       .replace(/^-|-$/g, '');
                               }
                               
                               getValidatedWidth() {
                                   if (!this.width) return '';
                                   
                                   // Check for valid width values
                                   if (this.width === 'stretch' || this.width === 'content') {
                                       return `width-${this.width}`;
                                   }
                                   
                                   // Check for integer (pixels)
                                   if (/^\\d+$/.test(this.width)) {
                                       return 'width-custom';
                                   }
                                   
                                   // Invalid width, default to stretch
                                   return 'width-stretch';
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
                                   const widthClass = this.getValidatedWidth();
                                   const widthStyle = this.getWidthStyle();
                                   const formattedText = this.formatText(this.body || '');
                                   
                                   // Handle anchor logic
                                   let anchorId = '';
                                   let showAnchorLink = false;
                                   
                                   if (this.anchor === 'false' || this.anchor === false) {
                                       // Explicitly disabled
                                       anchorId = '';
                                       showAnchorLink = false;
                                   } else if (this.anchor) {
                                       // Explicitly set
                                       anchorId = this.anchor;
                                       showAnchorLink = true;
                                   } else {
                                       // Auto-generate from body
                                       anchorId = this.generateAnchor(this.body);
                                       showAnchorLink = !!anchorId;
                                   }
                                   
                                   return html`
                                       <div class="title-container">
                                           <h1 class="title ${widthClass}" 
                                               id="${anchorId}"
                                               style="${widthStyle}">
                                               <span .innerHTML="${formattedText}"></span>
                                               ${showAnchorLink ? html`
                                                   <a href="#${anchorId}" class="anchor-link" title="Link to this section">
                                                       #
                                                   </a>
                                               ` : ''}
                                           </h1>
                                           ${this.help ? html`
                                               <jt-tooltip text="${this.help}"></jt-tooltip>
                                           ` : ''}
                                       </div>
                                   `;
                               }
                          }
                          
                          customElements.define('jt-title', JtTitle);
                          </script>
                          """;
    }
    
    @Override
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("<jt-title");
        sb.append(" body=\"").append(escapeHtml(body)).append("\"");
        
        if (anchor != null && !anchor.isEmpty()) {
            sb.append(" anchor=\"").append(escapeHtml(anchor)).append("\"");
        }
        
        if (help != null && !help.isEmpty()) {
            sb.append(" help=\"").append(escapeHtml(help)).append("\"");
        }
        
        if (width != null && !width.isEmpty()) {
            sb.append(" width=\"").append(escapeHtml(width)).append("\"");
        }
        
        sb.append("></jt-title>");
        return sb.toString();
    }
    
    @Override
    protected TypeReference<String> getTypeReference() {
        return new TypeReference<>() {
        };
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