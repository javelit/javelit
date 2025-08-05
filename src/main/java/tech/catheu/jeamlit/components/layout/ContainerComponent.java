package tech.catheu.jeamlit.components.layout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tech.catheu.jeamlit.core.Container;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;

public class ContainerComponent extends JtComponent<Container> {

    protected final Integer height;
    protected final Boolean border;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/layout/ContainerComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/ContainerComponent.render.html.mustache");
    }

    private ContainerComponent(final Builder builder) {
        // the currentValue is set when use() is called
        super(builder.generateKeyForInteractive(), null, null);
        this.height = builder.height;
        this.border = builder.border;
    }

    public static class Builder extends JtComponentBuilder<Container, ContainerComponent,  Builder> {
        private @Nullable Integer height;
        private @Nullable Boolean border;

        public Builder(final @Nonnull String key) {
            this.key = key;
        }

        public Builder height(final Integer height) {
            this.height = height;
            return this;
        }

        public Builder border(final @Nullable Boolean border) {
            this.border = border;
            return this;
        }

        @Override
        public ContainerComponent build() {
            if (Container.RESERVED_PATHS.contains(this.key)) {
                throw new IllegalArgumentException("Component " + this.key + " is a reserved value. Please use another key value.");
            }
            if (border == null) {
                border = height != null;
            }
            return new ContainerComponent(this);
        }
    }

    @Override
    protected String register() {
        if (currentValue == null) {
            throw new IllegalStateException("Component has not been fully initialized yet. use() should be called before register().");
        }
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        if (currentValue == null) {
            throw new IllegalStateException("Component has not been fully initialized yet. use() should be called before register().");
        }
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    protected TypeReference<Container> getTypeReference() {
        return new TypeReference<>() {};
    }

    /// Add the component to the app in the provided [Container] and return this component's [Container].
    /// for instance, if the container is "main", returns a container \["main", $key\]
    @Override
    public void beforeUse(final Container container) {
        this.currentValue = container.child(getKey());
    }
}
