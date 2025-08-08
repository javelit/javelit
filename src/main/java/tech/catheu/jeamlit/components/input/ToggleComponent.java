package tech.catheu.jeamlit.components.input;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * Toggle component that displays a toggle switch widget.
 * Similar to a checkbox but with different UI styling (toggle switch vs checkbox).
 * 
 * Streamlit API Reference: https://docs.streamlit.io/develop/api-reference/widgets/st.toggle
 */
public class ToggleComponent extends JtComponent<Boolean> {
    // Fields are protected to be visible to the template engine - see render function
    protected final String label;
    protected final boolean value;
    protected final String help;
    protected final boolean disabled;
    protected final LabelVisibility labelVisibility;
    protected final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/ToggleComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/ToggleComponent.render.html.mustache");
    }
    
    private ToggleComponent(final Builder builder) {
        super(builder.generateKeyForInteractive(), builder.value, builder.onChange);
        
        this.label = builder.label;
        this.value = builder.value;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
    }
    
    public static class Builder extends JtComponentBuilder<Boolean, ToggleComponent, Builder> {
        private final String label;
        private boolean value = false;
        private String help;
        private Consumer<Boolean> onChange;
        private boolean disabled = false;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private String width = "content";
        
        public Builder(final @Nonnull String label) {
            // Validate required parameters
            if (label == null || label.trim().isEmpty()) {
                throw new IllegalArgumentException("Toggle label cannot be null or empty - required for accessibility reasons");
            }
            this.label = label;
        }
        
        /**
         * Preselects the toggle when first rendered.
         * 
         * @param value Initial toggle state. Defaults to false.
         * @return this builder
         */
        public Builder value(final boolean value) {
            this.value = value;
            return this;
        }
        
        /**
         * A tooltip that gets displayed next to the widget label.
         * Streamlit only displays the tooltip when label_visibility="visible".
         * 
         * @param help Tooltip text. If null (default), no tooltip is displayed.
         * @return this builder
         */
        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }
        
        /**
         * An optional callback invoked when this toggle's value changes.
         * 
         * @param onChange Callback function
         * @return this builder
         */
        public Builder onChange(final @Nullable Consumer<Boolean> onChange) {
            this.onChange = onChange;
            return this;
        }
        
        /**
         * An optional boolean that disables the toggle if set to true.
         * 
         * @param disabled Whether the toggle should be disabled. Default is false.
         * @return this builder
         */
        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }
        
        /**
         * The visibility of the label.
         * 
         * @param labelVisibility The visibility of the label. Default is "visible".
         *                       If "hidden", displays an empty spacer instead of the label.
         *                       If "collapsed", displays no label or spacer.
         * @return this builder
         */
        public Builder labelVisibility(final @Nonnull LabelVisibility labelVisibility) {
            if (labelVisibility == null) {
                throw new IllegalArgumentException("Label visibility cannot be null");
            }
            this.labelVisibility = labelVisibility;
            return this;
        }
        
        /**
         * The width of the toggle widget.
         * 
         * @param width Can be "content" (default - width matches content),
         *              "stretch" (width matches parent container),
         *              or an integer specifying width in pixels
         * @return this builder
         */
        public Builder width(final @Nonnull String width) {
            if (width == null || width.trim().isEmpty()) {
                throw new IllegalArgumentException("Width cannot be null or empty");
            }
            
            // Validate width values
            if (!width.equals("content") && !width.equals("stretch")) {
                try {
                    // Try to parse as integer for pixel width
                    int pixelWidth = Integer.parseInt(width);
                    if (pixelWidth < 0) {
                        throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + pixelWidth);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Width must be 'content', 'stretch', or a non-negative integer. Got: " + width);
                }
            }
            
            this.width = width;
            return this;
        }
        
        /**
         * Convenience method for setting width as integer pixels.
         * 
         * @param widthPixels Width in pixels (must be non-negative)
         * @return this builder
         */
        public Builder width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }
        
        @Override
        public ToggleComponent build() {
            return new ToggleComponent(this);
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
    protected Boolean castAndValidate(final Object rawValue) {
        if (rawValue instanceof Boolean) {
            return (Boolean) rawValue;
        }
        if (rawValue instanceof String) {
            return Boolean.parseBoolean((String) rawValue);
        }
        return super.castAndValidate(rawValue);
    }
}