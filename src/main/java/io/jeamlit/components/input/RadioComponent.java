/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jeamlit.components.input;

import java.io.StringWriter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.jeamlit.core.utils.Preconditions.checkArgument;
import static io.jeamlit.core.utils.Preconditions.checkState;

public class RadioComponent<T> extends JtComponent<@Nullable T> {

    final String label;
    private final List<T> options;
    final Integer index;
    private final Function<T, String> formatFunction;
    final String help;
    final boolean disabled;
    final boolean horizontal;
    private final List<String> captions;
    final LabelVisibility labelVisibility;
    final String width;


    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/RadioComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/RadioComponent.render.html.mustache");
    }

    public static class Builder<T> extends JtComponentBuilder<T, RadioComponent<T>, Builder<T>> {
        private final @Language("markdown") @Nonnull String label;
        private final @Nonnull List<T> options;
        private @jakarta.annotation.Nullable Integer index = 0;
        private @Nonnull Function<@Nullable T, String> formatFunction = e -> {
            if (e == null) {
                return "null";
            } else if (e instanceof String s) {
                return markdownToHtml(s, true);
            } else {
                return Objects.toString(e, "null");
            }
        };
        private @jakarta.annotation.Nullable String help;
        private @jakarta.annotation.Nullable Consumer<@NotNull T> onChange;
        private boolean disabled;
        private boolean horizontal;
        private @jakarta.annotation.Nullable List<String> captions;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private String width = "content";

        public Builder(final @Language("markdown") @NotNull String label, final @Nonnull List<T> options) {
            this.label = label;
            checkArgument(!options.isEmpty(),
                          "No values in the options collection. Please provide a collection with at least one value.");
            this.options = options;
        }

        /**
         * The index of the preselected option on first render. If {@code null}, initializes empty and returns {@code null} until user selection.
         * Defaults to 0 (the first option).
         */
        public Builder<T> index(final @jakarta.annotation.Nullable Integer index) {
            if (index != null) {
                checkArgument(index >= 0, "Index is %s. Index must be a positive integer.", index);
                checkArgument(index < options.size(),
                              "Index is %s. Index must be strictly smaller than the options list size: %s",
                              index,
                              options.size());
            }
            this.index = index;
            return this;
        }

        /**
         * Function to modify the display of the radio options. The {@code Function} receives the raw option object
         * and returns a String that will be used as display label.
         * Does not impact the return value of the component.
         */
        public Builder<T> formatFunction(final @Nonnull Function<@Nullable T, String> formatFunction) {
            this.formatFunction = formatFunction;
            return this;
        }

        /**
         * A tooltip that gets displayed next to the widget label. If {@code null}, no tooltip is displayed.
         */
        public Builder<T> help(final @jakarta.annotation.Nullable String help) {
            this.help = help;
            return this;
        }

        /**
         * An optional callback invoked when the radio button's value changes.
         * The value passed to the callback is the previous value of the component.
         */
        public Builder<T> onChange(final @jakarta.annotation.Nullable Consumer<@NotNull T> onChange) {
            this.onChange = onChange;
            return this;
        }

        /**
         * Disables the radio buttons if set to true. When disabled, users cannot interact with the widget.
         */
        public Builder<T> disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * Orients the radio group horizontally instead of vertically when set to true.
         */
        public Builder<T> horizontal(final boolean horizontal) {
            this.horizontal = horizontal;
            return this;
        }

        /**
         * A list of captions to show below each radio button. If {@code null} (default), no captions are shown.
         * Must match the size of the options list.
         */
        public Builder<T> captions(final @Nonnull List<String> captions) {
            checkArgument(captions.size() == this.options.size(),
                          "Captions list has size %s. Options list has size %s. Captions should match the size of options.",
                          captions.size(),
                          options.size());
            this.captions = captions;
            return this;
        }

        /**
         * The visibility of the label. The default is {@code VISIBLE}.
         * If this is {@code HIDDEN}, Jeamlit displays an empty spacer instead of the label, which can help keep the
         * widget aligned with other widgets. If this is {@code COLLAPSED}, Jeamlit displays no label or spacer.
         */
        public Builder<T> labelVisibility(final @Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        /**
         * The width of the element. This can be one of the following:
         * <ul>
         * <li>"content" (default): The width of the element matches the width of its content, but doesn't exceed the width of the parent container.</li>
         * <li>"stretch": The width of the element matches the width of the parent container.</li>
         * <li>An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.</li>
         * </ul>
         */
        public Builder<T> width(final @jakarta.annotation.Nullable String width) {
            if (width != null && !"stretch".equals(width) && !"content".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "width must be 'stretch', 'content', or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        /**
         * The width of the radio group in pixels. The element will have a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.
         */
        public Builder<T> width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        @Override
        public RadioComponent<T> build() {
            return new RadioComponent<>(this);
        }
    }


    private RadioComponent(final Builder<T> builder) {
        super(builder.generateKeyForInteractive(),
              builder.index == null ? null : builder.options.get(builder.index),
              builder.onChange);
        this.label = markdownToHtml(builder.label, true);
        this.options = builder.options;
        this.index = builder.index;
        this.formatFunction = builder.formatFunction;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.horizontal = builder.horizontal;
        this.captions = builder.captions;
        this.labelVisibility = builder.labelVisibility;
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
    protected TypeReference<T> getTypeReference() {
        return new TypeReference<T>() {
        };
    }

    @Override
    protected @Nullable T convert(Object rawValue) {
        final int index;
        if (rawValue instanceof Integer i) {
            index = i;
        } else if (rawValue instanceof Long i) {
            checkState(i < Integer.MAX_VALUE,
                       "index integer returned by radio component is bigger than maximum integer. Please reach out to support.");
            index = i.intValue();
        } else {
            throw new RuntimeException("Unsupported value type send by radio component frontend: " + rawValue.getClass());
        }
        checkState(index >= 0,
                   "Index is %s. Index must be a positive integer. Invalid value sent by the radio component frontend. Please reach out to support.",
                   index);
        checkState(index < options.size(),
                   "Index is %s. Index must be strictly smaller than the options list size %s. Invalid value sent by the radio component frontend. Please reach out to support.",
                   index,
                   options.size());

        return options.get(index);
    }


    String optionsJson() {
        return toJson(this.options.stream().map(this.formatFunction).toList());
    }

    String captionsJson() {
        return toJson(this.captions);
    }
}
