package tech.catheu.jeamlit.components.layout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import tech.catheu.jeamlit.core.Container;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;

public class ExpanderComponent extends JtComponent<Container> {

    protected final @Nonnull String label;
    protected final boolean expanded;
    protected final @Nullable String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/layout/ExpanderComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/ExpanderComponent.render.html.mustache");
    }

    private ExpanderComponent(final Builder builder) {
        // the currentValue is set when use() is called
        super(builder.generateKeyForInteractive(), null, null);
        this.label = builder.label;
        this.expanded = builder.expanded;
        this.width = builder.width;
    }

    public static class Builder extends JtComponentBuilder<Container, ExpanderComponent, Builder> {
        private final @Nonnull String label;
        private boolean expanded = false;
        private @Nullable String width = "stretch";

        public Builder(final @Nonnull String key, final @Nonnull String label) {
            this.key = key;
            this.label = label;
        }

        public Builder expanded(final boolean expanded) {
            this.expanded = expanded;
            return this;
        }

        public Builder width(final @Nullable String width) {
            if (width != null && !width.equals("stretch") && !width.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "width must be 'stretch' or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        @Override
        public ExpanderComponent build() {
            if (Container.RESERVED_PATHS.contains(this.key)) {
                throw new IllegalArgumentException("Component " + this.key + " is a reserved value. Please use another key value.");
            }
            return new ExpanderComponent(this);
        }
    }

    @Override
    protected String register() {
        if (currentValue == null) {
            throw new IllegalStateException(
                    "Component has not been fully initialized yet. use() should be called before register().");
        }
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        if (currentValue == null) {
            throw new IllegalStateException(
                    "Component has not been fully initialized yet. use() should be called before register().");
        }
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    protected TypeReference<Container> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    public void beforeUse(final @NotNull Container container) {
        this.currentValue = container.child(getKey());
    }
}