package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

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
        return "";
    }
    
    @Override
    public String render() {
        return String.format("<h1>%s</h1>", escapeHtml(text));
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