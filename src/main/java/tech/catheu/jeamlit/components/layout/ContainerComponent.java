package tech.catheu.jeamlit.components.layout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;
import tech.catheu.jeamlit.core.Layout;

import java.io.StringWriter;

public class ContainerComponent extends JtComponent<Layout> {

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

    public static class Builder extends JtComponentBuilder<Layout, ContainerComponent,  Builder> {
        private @Nullable Integer height;
        private @Nullable Boolean border;

        public Builder(final @Nonnull String key) {
            this.key = key;
        }

        public void height(final Integer height) {
            this.height = height;
        }

        public void border(final @Nullable Boolean border) {
            this.border = border;
        }

        @Override
        public ContainerComponent build() {
            if (Layout.RESERVED_PATHS.contains(this.key)) {
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

    protected TypeReference<Layout> getTypeReference() {
        return new TypeReference<>() {};
    }

    /// Add the component to the app in the provided layout and return the container layout.
    /// for instance, if the layout is "main", returns a layout \["main", $key\]
    @Override
    public void beforeUse(final Layout layout) {
        this.currentValue = layout.with(getKey());
    }
}
