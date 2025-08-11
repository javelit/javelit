package tech.catheu.jeamlit.components.text;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;

public class TextComponent extends JtComponent<JtComponent.NONE> {
    // protected to be visible to the template engine
    protected final String body;
    protected final String help;
    protected final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/TextComponent.register.html.mustache");
        renderTemplate = mf.compile("components/TextComponent.render.html.mustache");
    }

    @SuppressWarnings("unused")
    public static class Builder extends JtComponentBuilder<NONE, TextComponent, Builder> {
        private @Language("Markdown") String body;
        private String help;
        private String width = "content";

        public Builder(final @Nonnull @Language("Markdown") String body) {
            this.body = body;
        }

        public Builder body(final @Nullable @Language("Markdown") String body) {
            this.body = body;
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
        public TextComponent build() {
            return new TextComponent(this);
        }
    }

    private TextComponent(Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE, null);
        this.body = builder.body;
        this.help = builder.help;
        this.width = builder.width;
    }

    @Override
    protected String register() {
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected TypeReference<NONE> getTypeReference() {
        return new TypeReference<>() {};
    }
}