package tech.catheu.jeamlit.components.input;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import tech.catheu.jeamlit.core.JtContainer;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;

import static tech.catheu.jeamlit.core.utils.Preconditions.checkArgument;

public class LinkButtonComponent extends JtComponent<JtComponent.NONE> {
    // the following fields are protected to be visible to the template engine - see render function
    protected final String label;
    protected final String url;
    protected final String type;
    protected final String icon;
    protected final String help;
    protected final boolean disabled;
    protected final boolean useContainerWidth; // deprecated but still supported
    protected final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/LinkButtonComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/LinkButtonComponent.render.html.mustache");
    }
    
    private LinkButtonComponent(final Builder builder) {
        super(builder.generateKeyForInteractive(), JtComponent.NONE.NONE, null);

        this.label = builder.label;
        this.url = builder.url;
        this.type = builder.type;
        this.icon = builder.icon;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.useContainerWidth = builder.useContainerWidth;
        this.width = builder.width;
    }
    
    public static class Builder extends JtComponentBuilder<JtComponent.NONE, LinkButtonComponent, Builder> {
        private final String label;
        private final String url;
        private String type = "secondary";
        private String icon;
        private String help;
        private boolean disabled = false;
        private boolean useContainerWidth = false; // deprecated
        private String width = "content";
        
        public Builder(final @Nonnull String label, final @Nonnull String url) {
            // Validate required parameters
            if (label == null || label.trim().isEmpty()) {
                throw new IllegalArgumentException("Link button label cannot be null or empty");
            }
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("Link button URL cannot be null or empty");
            }
            this.label = label;
            this.url = url;
        }
        
        public Builder type(final @Nonnull String type) {
            if (!type.equals("primary") && !type.equals("secondary") && !type.equals("tertiary")) {
                throw new IllegalArgumentException("Link button type must be 'primary', 'secondary', or 'tertiary'. Got: " + type);
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
            // Deprecated parameter - for backward compatibility, maps to width
            this.useContainerWidth = useContainerWidth;
            if (useContainerWidth) {
                this.width = "stretch";
            }
            return this;
        }

        public Builder width(final String width) {
            if (width != null && !width.equals("content") && !width.equals("stretch")) {
                // Try to parse as integer for pixel width
                try {
                    Integer.parseInt(width);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Width must be 'content', 'stretch', or a valid integer for pixel width. Got: " + width);
                }
            }
            this.width = width != null ? width : "content";
            return this;
        }

        public Builder width(final int widthPixels) {
            if (widthPixels <= 0) {
                throw new IllegalArgumentException("Width in pixels must be positive. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }
        
        @Override
        public LinkButtonComponent build() {
            return new LinkButtonComponent(this);
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
    protected TypeReference<JtComponent.NONE> getTypeReference() {
        return new TypeReference<>() {};
    }

    @Override
    protected void resetIfNeeded() {
        // Link button doesn't need reset behavior
    }

    @Override
    public void beforeUse(@NotNull JtContainer container) {
        final String parentFormComponentKey = container.getParentFormComponentKey();
        checkArgument(parentFormComponentKey == null,
                      "Attempting to create a link button inside a form. %s. A link button cannot be added to a form. Please use a form submit button instead with Jt.formSubmitButton.",
                      parentFormComponentKey,
                      parentFormComponentKey);
    }
}