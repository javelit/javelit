package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;

public class TitleComponent extends JtComponent<String> {
    // protected to be visible to the template engine
    protected final String anchor;
    protected final String help;
    protected final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/TitleComponent.register.html.mustache");
        renderTemplate = mf.compile("components/TitleComponent.render.html.mustache");
    }
    
    private TitleComponent(final Builder builder) {
        super(builder.generateKey());
        this.currentValue = builder.body;
        this.anchor = builder.anchor;
        this.help = builder.help;
        this.width = builder.width;
    }
    
    @SuppressWarnings("unused")
    public static class Builder implements JtComponentBuilder<TitleComponent> {
        private String body;
        private String anchor;
        private String help;
        private String width = "stretch";
        
        public Builder(final String body) {
            this.body = body;
        }
        
        public Builder anchor(final @Nullable String anchor) {
            this.anchor = anchor;
            return this;
        }
        
        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }
        
        public Builder width(final @Nonnull String width) {
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
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    public String render() {
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
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