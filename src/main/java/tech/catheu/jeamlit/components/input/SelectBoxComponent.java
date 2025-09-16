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
package tech.catheu.jeamlit.components.input;

import java.io.StringWriter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.catheu.jeamlit.core.JtComponent;
import tech.catheu.jeamlit.core.JtComponentBuilder;

import static tech.catheu.jeamlit.core.utils.Preconditions.checkArgument;
import static tech.catheu.jeamlit.core.utils.Preconditions.checkState;

public class SelectBoxComponent<T> extends JtComponent<@Nullable T> {

    final String label;
    private final List<T> options;
    final Integer index;
    private final Function<T, String> formatFunction;
    final String help;
    final @jakarta.annotation.Nullable String placeholder;
    final boolean disabled;
    final LabelVisibility labelVisibility;
    final boolean acceptNewOptions;
    final String width;


    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/input/SelectBoxComponent.register.html.mustache");
        renderTemplate = mf.compile("components/input/SelectBoxComponent.render.html.mustache");
    }

    public static class Builder<T> extends JtComponentBuilder<T, SelectBoxComponent<T>, Builder<T>> {
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
        private @jakarta.annotation.Nullable String placeholder;
        private @jakarta.annotation.Nullable Consumer<@NotNull T> onChange;
        private boolean disabled;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private boolean acceptNewOptions;
        private String width = "content";

        public Builder(final @Language("markdown") @NotNull String label, final @Nonnull List<T>  options) {
            this.label = label;
            this.options = options;
        }

        public Builder<T> index(final @jakarta.annotation.Nullable Integer index) {
            if (index != null) {
                checkArgument(index >= 0, "Index is %s. Index must be a positive integer.", index);
                checkArgument(index < options.size(), "Index is %s. Index must be strictly smaller than the options list size: %s", index, options.size());
            }
            this.index = index;
            return this;
        }

        public Builder<T> formatFunction(final @Nonnull Function<@Nullable T, String> formatFunction) {
            this.formatFunction = formatFunction;
            return this;
        }

        public Builder<T> help(final @jakarta.annotation.Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder<T> onChange(final @jakarta.annotation.Nullable Consumer<@NotNull T> onChange) {
            this.onChange = onChange;
            return this;
        }

        public Builder<T> disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder<T> labelVisibility(final @Nonnull LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        public Builder<T> width(final @jakarta.annotation.Nullable String width) {
            if (width != null && !"stretch".equals(width) && !"content".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "width must be 'stretch', 'content', or a pixel value (integer). Got: " + width);
            }
            this.width = width;
            return this;
        }

        public Builder<T> width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        public Builder<T> placeholder(final @jakarta.annotation.Nullable String placeholder) {
            this.placeholder = placeholder;
            List.of();
            return this;
        }

        public Builder<T> acceptNewOptions(final boolean acceptNewOptions) {
            if (acceptNewOptions) {
                final boolean allStrings = this.options.stream().allMatch(e -> e instanceof  String);
                checkArgument(allStrings, "To enable acceptNewOptions, the options values must all be of type String. Update the options or do not enable acceptNewOptions.");
            }
            this.acceptNewOptions = acceptNewOptions;
            return this;
        }

        @Override
        public SelectBoxComponent<T> build() {
            return new SelectBoxComponent<>(this);
        }
    }


    private SelectBoxComponent(final Builder<T> builder) {
        super(builder.generateKeyForInteractive(), builder.index == null || builder.options.isEmpty() ? null : builder.options.get(builder.index), builder.onChange);
        this.label = markdownToHtml(builder.label, true);
        this.options = builder.options;
        this.index = builder.index;
        this.formatFunction = builder.formatFunction;
        this.help = builder.help;
        this.placeholder = builder.placeholder;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.acceptNewOptions = builder.acceptNewOptions;
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
        return new TypeReference<T>() {};
    }

    @Override
    protected @Nullable T convert(Object rawValue) {
        final int index;
        if (rawValue instanceof Integer i ) {
            index = i;
        } else if (rawValue instanceof Long i) {
            checkState(i < Integer.MAX_VALUE, "index integer returned by radio component is bigger than maximum integer. Please reach out to support.");
            index = i.intValue();
        } else if (rawValue instanceof String s && this.acceptNewOptions) {
            int foundIndex = -1;
            for (int i = 0; i < this.options.size(); i++) {
                if (s.equalsIgnoreCase(this.formatFunction.apply(this.options.get(i)))) {
                    foundIndex = i;
                    break;
                }
            }
            if (foundIndex == -1) {
                // no existing options match - this is a new option
                // this casting is safe - the builder ensures that when acceptNewOptions is true, the type of options is String
                return (T) s;
            }
            index = foundIndex;
        } else {
            throw new RuntimeException("Unsupported value type send by radio component frontend: " + rawValue.getClass());
        }
        checkState(index >= 0, "Index is %s. Index must be a positive integer. Invalid value sent by the radio component frontend. Please reach out to support.", index);
        checkState(index < options.size(), "Index is %s. Index must be strictly smaller than the options list size %s. Invalid value sent by the radio component frontend. Please reach out to support.", index, options.size());

        return options.get(index);
    }


    String optionsJson() {
        return toJson(this.options.stream().map(this.formatFunction).toList());
    }
}
