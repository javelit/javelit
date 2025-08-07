package tech.catheu.jeamlit.components.layout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import tech.catheu.jeamlit.core.JtContainer;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;
import java.util.function.Consumer;

import static tech.catheu.jeamlit.core.utils.Preconditions.checkState;

public class FormSubmitButtonComponent extends JtComponent<Boolean> {
    // the following fields are protected to be visible to the template engine - see render function
    protected final String label;
    protected final String type;
    protected final String icon;
    protected final String help;
    protected final boolean disabled;
    protected final boolean useContainerWidth;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/layout/FormSubmitButtonComponent.register.html.mustache");
        renderTemplate = mf.compile("components/layout/FormSubmitButtonComponent.render.html.mustache");
    }
    
    private FormSubmitButtonComponent(final Builder builder) {
        super(builder.generateKeyForInteractive(), false, builder.onClick);

        this.label = builder.label;
        this.type = builder.type;
        this.icon = builder.icon;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.useContainerWidth = builder.useContainerWidth;
    }
    
    public static class Builder extends JtComponentBuilder<Boolean, FormSubmitButtonComponent, Builder> {
        private final String label;
        private String type = "secondary";
        private String icon;
        private String help;
        private boolean disabled = false;
        private boolean useContainerWidth = false;
        private Consumer<Boolean> onClick;

        public Builder(final @Nonnull String label) {
            if (label.trim().isEmpty()) {
                throw new IllegalArgumentException("FormSubmitButton label cannot be null or empty");
            }
            this.label = label;
        }
        
        public Builder type(final @Nonnull String type) {
            if (!type.equals("primary") && !type.equals("secondary") && !type.equals("tertiary")) {
                throw new IllegalArgumentException("Button type must be 'primary', 'secondary', or 'tertiary'. Got: " + type);
            }
            this.type = type;
            return this;
        }
        
        public Builder icon(final String icon) {
            this.icon = icon;
            return this;
        }
        
        public Builder help(final String help) {
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

        public Builder onClick(final Consumer<Boolean> onClick) {
            this.onClick = onClick;
            return this;
        }
        
        @Override
        public FormSubmitButtonComponent build() {
            return new FormSubmitButtonComponent(this);
        }
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
    protected TypeReference<Boolean> getTypeReference() {
        return new TypeReference<Boolean>() {};
    }

    @Override
    protected void resetIfNeeded() {
        // Button truthy value is momentary - reset to false after reading
        currentValue = false;
    }

    @Override
    public void beforeUse(@NotNull JtContainer container) {
        final @Nullable String formParentKey = container.getParentFormComponentKey();
        checkState(formParentKey != null, "FormSubmitButton must be inside a form container. Please pass a valid container that is a form or in a form.");
    }
}