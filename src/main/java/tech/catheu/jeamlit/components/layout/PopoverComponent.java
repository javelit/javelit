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

public class PopoverComponent extends JtComponent<Container> {

    protected final @Nonnull String label;
    protected final @Nullable String help;
    protected final boolean disabled;
    protected final boolean useContainerWidth;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/layout/PopoverComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/PopoverComponent.render.html.mustache");
    }

    private PopoverComponent(final Builder builder) {
        // the currentValue is set when use() is called
        super(builder.generateKeyForInteractive(), null, null);
        this.label = builder.label;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.useContainerWidth = builder.useContainerWidth;
    }

    public static class Builder extends JtComponentBuilder<Container, PopoverComponent, Builder> {
        private final @Nonnull String label;
        private @Nullable String help;
        private boolean disabled = false;
        private boolean useContainerWidth = false;

        public Builder(final @Nonnull String key, final @Nonnull String label) {
            this.key = key;
            this.label = label;
        }

        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder useContainerWidth(final boolean useContainerWidth) {
            this.useContainerWidth = useContainerWidth;
            return this;
        }

        @Override
        public PopoverComponent build() {
            if (Container.RESERVED_PATHS.contains(this.key)) {
                throw new IllegalArgumentException("Component " + this.key + " is a reserved value. Please use another key value.");
            }
            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("Popover label cannot be null or empty");
            }
            return new PopoverComponent(this);
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