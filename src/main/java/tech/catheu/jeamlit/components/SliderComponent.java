package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nullable;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import java.io.StringWriter;
import java.util.function.Consumer;

public class SliderComponent extends JtComponent<Double> {
    protected final String label;
    protected final double min;
    protected final double max;
    protected final double value;
    protected final double step;
    protected final String format;
    protected final String help;
    protected final boolean disabled;
    protected final String labelVisibility;
    protected final String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/SliderComponent.register.html.mustache");
        renderTemplate = mf.compile("components/SliderComponent.render.html.mustache");
    }
    
    private SliderComponent(Builder builder) {
        super(builder.generateKey(), builder.value, builder.onChange);

        this.label = builder.label;
        this.min = builder.min;
        this.max = builder.max;
        this.value = builder.value;
        this.step = builder.step;
        this.format = builder.format;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
    }
    
    @SuppressWarnings("unused")
    public static class Builder implements JtComponentBuilder<Double, SliderComponent> {
        private final String label;
        private double min = 0.0;
        private double max = 100.0;
        private double value = 0.0; // Will be set to min if not specified
        private double step = 1.0;
        private String format = null;
        private String help = null;
        private boolean disabled = false;
        private String labelVisibility = "visible";
        private String key;
        private Consumer<Double> onChange;
        private String width = "stretch";
        
        public Builder(String label) {
            this.label = label;
        }
        
        public Builder min(double min) {
            this.min = min;
            return this;
        }
        
        public Builder max(double max) {
            this.max = max;
            return this;
        }
        
        public Builder value(double value) {
            this.value = value;
            return this;
        }
        
        public Builder step(double step) {
            this.step = step;
            return this;
        }
        
        public Builder format(String format) {
            this.format = format;
            return this;
        }
        
        public Builder help(String help) {
            this.help = help;
            return this;
        }
        
        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }
        
        public Builder labelVisibility(String labelVisibility) {
            if (labelVisibility != null && !labelVisibility.equals("visible") && 
                !labelVisibility.equals("hidden") && !labelVisibility.equals("collapsed")) {
                throw new IllegalArgumentException("label_visibility must be 'visible', 'hidden', or 'collapsed'. Got: " + labelVisibility);
            }
            this.labelVisibility = labelVisibility;
            return this;
        }
        
        public Builder key(String key) {
            this.key = key;
            return this;
        }
        
        public Builder onChange(@Nullable Consumer<Double> onChange) {
            this.onChange = onChange;
            return this;
        }
        
        public Builder width(String width) {
            if (width != null && !width.equals("stretch") && !width.matches("\\d+")) {
                throw new IllegalArgumentException("width must be 'stretch' or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }
        
        @Override
        public SliderComponent build() {
            // Add comment about args/kwargs not being implemented
            // Note: args/kwargs equivalent (varargs and Map parameters) not implemented
            
            // Set value to min if not explicitly set (matching Streamlit behavior)
            if (this.value == 0.0 && this.min != 0.0) {
                this.value = this.min;
            }
            
            // Validate parameters
            if (this.label == null || this.label.trim().isEmpty()) {
                throw new IllegalArgumentException("Label cannot be null or empty");
            }
            if (this.min >= this.max) {
                throw new IllegalArgumentException("min_value must be less than max_value");
            }
            if (this.step <= 0) {
                throw new IllegalArgumentException("step must be positive");
            }
            if (this.value < this.min || this.value > this.max) {
                throw new IllegalArgumentException("value must be between min_value and max_value");
            }
            
            return new SliderComponent(this);
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
    protected TypeReference<Double> getTypeReference() {
        return new TypeReference<>() {};
    }
    
    @Override
    protected Double castAndValidate(Object rawValue) {
        Double value = super.castAndValidate(rawValue);
        // Clamp to valid range
        return Math.max(min, Math.min(max, value));
    }
    
    @Override
    protected void resetIfNeeded() {
        // Slider keeps its value - no reset needed
    }
}