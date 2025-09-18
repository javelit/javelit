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
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import io.jeamlit.core.JtContainer;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import static io.jeamlit.core.utils.Preconditions.checkArgument;

public final class ButtonComponent extends JtComponent<Boolean> {
  // the following fields are protected to be visible to the template engine - see render function
  final @Nonnull String label;
  final String type;
  final String icon;
  final String help;
  final boolean disabled;
  final boolean useContainerWidth;

  private static final Mustache registerTemplate;
  private static final Mustache renderTemplate;

  static {
    final MustacheFactory mf = new DefaultMustacheFactory();
    registerTemplate = mf.compile("components/input/ButtonComponent.register.html.mustache");
    renderTemplate = mf.compile("components/input/ButtonComponent.render.html.mustache");
  }

  private ButtonComponent(final Builder builder) {
    super(builder.generateKeyForInteractive(), false, builder.onClick);

    this.label = markdownToHtml(builder.label, true);
    this.type = builder.type;
    this.icon = builder.icon;
    this.help = builder.help;
    this.disabled = builder.disabled;
    this.useContainerWidth = builder.useContainerWidth;
  }

  public static class Builder extends JtComponentBuilder<Boolean, ButtonComponent, Builder> {
    @Language("markdown")
    private final @Nonnull String label;
    private String type = "secondary";
    private String icon;
    private String help;
    private boolean disabled;
    private boolean useContainerWidth;
    private Consumer<Boolean> onClick;

    public Builder(final @Language("markdown") @Nonnull String label) {
      // Validate required parameters
      if (label.trim().isEmpty()) {
        throw new IllegalArgumentException("Button label cannot be null or empty");
      }
      this.label = label;
    }

    public Builder type(final @Nonnull String type) {
      if (!"primary".equals(type) && !"secondary".equals(type) && !"tertiary".equals(type)) {
        throw new IllegalArgumentException(
            "Button type must be 'primary', 'secondary', or 'tertiary'. Got: " + type);
      }
      this.type = type;
      return this;
    }

    public Builder icon(final String icon) {
      ensureIsValidIcon(icon);
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

    // FIXME CYRIL - DEPRECATED - implement width instead
    public Builder useContainerWidth(final boolean useContainerWidth) {
      this.useContainerWidth = useContainerWidth;
      return this;
    }

    public Builder onClick(final Consumer<Boolean> onClick) {
      this.onClick = onClick;
      return this;
    }

    @Override
    public ButtonComponent build() {
      return new ButtonComponent(this);
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
    return new TypeReference<>() {
    };
  }

  @Override
  protected void resetIfNeeded() {
    // Button truthy value is momentary - reset to false after reading
    currentValue = false;
  }

  @Override
  protected void beforeUse(@NotNull JtContainer container) {
    final String parentFormComponentKey = container.getParentFormComponentKey();
    checkArgument(parentFormComponentKey == null,
                  "Attempting to create a button inside a form. %s. A button cannot be added to a form. Please use a form submit button instead with Jt.formSubmitButton.",
                  parentFormComponentKey,
                  parentFormComponentKey);
  }
}
