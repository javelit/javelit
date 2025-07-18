package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import static tech.catheu.jeamlit.components.JsConstants.LIT_DEPENDENCY;

public class TitleComponent extends JtComponent<String> {
    private final String text;
    
    private TitleComponent(Builder builder) {
        this.text = builder.text;
    }
    
    public static class Builder implements JtComponentBuilder<TitleComponent> {
        private String text;
        
        public Builder(String text) {
            this.text = text;
        }
        
        @Override
        public TitleComponent build() {
            return new TitleComponent(this);
        }
    }
    
    @Override
    public String register() {
        // No JavaScript needed for title component
        return
               """
               <script type="module">
               import { LitElement, html, css } from '%s';
               
               class JtTitle extends LitElement {
                    static styles = css`
                     p {
                       color: blue;
                     }
                   `;
               
                    static properties = {   
                      text: {type: String}, 
                      };
           
                   render() {
                     return html`<h1>${this.text}!</h1>`;
                   }
               }
               
               customElements.define('jt-title', JtTitle);
               </script>
               """.formatted(LIT_DEPENDENCY);
    }
    
    @Override
    public String render() {
        return String.format("<jt-title text=\"%s\"></jt-title>", escapeHtml(text));
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