package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

public class TextComponent extends JtComponent<String> {
    private final String text;

    public static class Builder implements JtComponentBuilder<TextComponent> {
        private String text;

        public Builder(String text) {
            this.text = text;
        }

        @Override
        public TextComponent build() {
            return new TextComponent(this);
        }
    }

    private TextComponent(Builder builder) {
        this.text = builder.text;
    }

    @Override
    public String returnValue() {
        return text;
    }
    
    @Override
    public String register() {
        // No JavaScript needed for text component
        return "";
    }
    
    @Override
    public String render() {
        return String.format("<p>%s</p>", escapeHtml(text));
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