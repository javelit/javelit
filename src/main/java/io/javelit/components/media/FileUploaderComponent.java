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
package io.javelit.components.media;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javelit.core.JtComponent;
import io.javelit.core.JtComponentBuilder;
import io.javelit.core.JtUploadedFile;
import io.javelit.core.Shared;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkArgument;

public class FileUploaderComponent extends JtComponent<List<JtUploadedFile>> {

  public static final String FRONTEND_FILENAME_KEY = "filename";

  public enum MultipleFiles {
    FALSE,
    TRUE,
    DIRECTORY
  }

  protected final @NotNull String label;
  protected final List<String> types;
  protected final @Nonnull MultipleFiles acceptMultipleFiles;
  protected final @Nullable String help;
  protected final boolean disabled;
  protected final LabelVisibility labelVisibility;
  protected final String width;


  private static final Mustache registerTemplate;
  private static final Mustache renderTemplate;

  static {
    final MustacheFactory mf = new DefaultMustacheFactory();
    registerTemplate = mf.compile("components/media/FileUploaderComponent.register.html.mustache");
    renderTemplate = mf.compile("components/media/FileUploaderComponent.render.html.mustache");
  }

  private FileUploaderComponent(final Builder builder) {
    super(builder, List.of(), builder.onChange);
    this.label = markdownToHtml(builder.label, true);
    this.types = builder.types;
    this.acceptMultipleFiles = builder.acceptMultipleFiles;
    this.help = builder.help;
    this.disabled = builder.disabled;
    this.labelVisibility = builder.labelVisibility;
    this.width = builder.width;
  }

  public static class Builder extends JtComponentBuilder<List<JtUploadedFile>, FileUploaderComponent, Builder> {

    @Language("markdown") private final @NotNull String label;
    private @Nullable Consumer<List<JtUploadedFile>> onChange;
    private List<String> types;
    private @Nonnull MultipleFiles acceptMultipleFiles = MultipleFiles.FALSE;
    private @Nullable String help;
    private boolean disabled;
    private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
    private String width = "stretch";

    public Builder(final @Language("markdown") @NotNull String label) {
      checkArgument(!label.isBlank(), "FileUploader label cannot be null or empty.");
      this.label = label;
    }

    /**
     * The allowed file extensions or MIME types. If {@code null}, all file types are allowed. Use file extensions like {@code ".pdf"} or MIME types like {@code "image/png"}.
     */
    public Builder type(final @Nullable List<String> types) {
      if (types != null) {
        checkArgument(!types.isEmpty(),
                      "FileUploader types cannot be an empty list. Use null to allow all types.");
        types.forEach(t -> checkArgument(t.startsWith(".") || t.contains("/"),
                                         "Types must be file extension (.pdf) or MIME type (image/png): %s",
                                         t));
      }

      this.types = types;
      return this;
    }

    /**
     * Whether to accept more than one file in a submission. This can be one of the following values:
     * <ul>
     *     <li>{@code MultipleFiles.FALSE} (default): The user can only submit one file at a time.</li>
     *     <li>{@code MultipleFiles.TRUE}: The user can upload multiple files at the same time.</li>
     *     <li>{@code MultipleFiles.DIRECTORY}: The user can select a directory to upload all files in the directory and its subdirectories. If {@code type} is set, only files matching those type(s) will be uploaded.</li>
     * </ul>
     */
    public Builder acceptMultipleFiles(final MultipleFiles acceptMultipleFiles) {
      this.acceptMultipleFiles = acceptMultipleFiles;
      return this;
    }

    /**
     * A tooltip that gets displayed next to the text. If this is {@code null} (default), no tooltip is displayed.
     */
    public Builder help(final @Nullable String help) {
      this.help = help;
      return this;
    }

    /**
     * Disable the file uploader if set to true. When disabled, users cannot interact with the widget.
     */
    public Builder disabled(final boolean disabled) {
      this.disabled = disabled;
      return this;
    }

    /**
     * An optional callback invoked when the file uploader's value changes.
     */
    public Builder onChange(final @Nullable Consumer<List<JtUploadedFile>> onChange) {
      this.onChange = onChange;
      return this;
    }

    /**
     * The visibility of the label. The default is {@code VISIBLE}.
     * If this is {@code HIDDEN}, Javelit displays an empty spacer instead of the label, which can help keep the
     * widget aligned with other widgets. If this is {@code COLLAPSED}, Javelit displays no label or spacer.
     */
    public Builder labelVisibility(final LabelVisibility labelVisibility) {
      this.labelVisibility = labelVisibility;
      return this;
    }

    /**
     * The width of the element. This can be one of the following:
     * <ul>
     * <li>{@code stretch}: The width of the element matches the width of the parent container.</li>
     * <li>An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.</li>
     * </ul>
     */
    public Builder width(final @Nonnull String width) {
      if (!"stretch".equals(width) && !width.matches("\\d+")) {
        throw new IllegalArgumentException("width must be 'stretch' or a pixel value (integer). Got: " + width);
      }
      this.width = width;
      return this;
    }

    /**
     * The width of the text element in pixels. The element will have a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.
     */
    public Builder width(final int widthPixels) {
      if (widthPixels < 0) {
        throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
      }
      this.width = String.valueOf(widthPixels);
      return this;
    }

    @Override
    public FileUploaderComponent build() {
      return new FileUploaderComponent(this);
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
  protected TypeReference<List<JtUploadedFile>> getTypeReference() {
    return new TypeReference<>() {
    };
  }

  @Override
  protected List<JtUploadedFile> convert(final Object rawValue) {
    if (rawValue instanceof List<?> files) {
      if (files.isEmpty()) {
        return List.of();
      }
      if (files.getFirst() instanceof JtUploadedFile) {
        // update is coming from the PUT operation
        return (List<JtUploadedFile>) files;
      }
      // update is coming from the frontend - it is a remove operation - need to convert frontend data to backend value
      final Set<String> filesToKeep = new HashSet<>(files.size());
      for (final Object file : files) {
        if (!(file instanceof Map<?, ?>)) {
          throw new RuntimeException(
              "Failed to parse input widget value coming from the app. UploadedFile element is not a map.");
        }
        filesToKeep.add(((Map<String, String>) file).get(FRONTEND_FILENAME_KEY));
      }

      return currentValue.stream().filter(e -> filesToKeep.contains(e.filename())).collect(Collectors.toList());
    } else {
      throw new RuntimeException(
          "Failed to parse input widget value coming from the app. FileUpload value is not a list. Please reach out to support.");
    }
  }

  // used in the template
  @SuppressWarnings("unused")
  protected String getCurrentValueJson() {
    if (currentValue == null || currentValue.isEmpty()) {
      return "[]";
    }
    final List<Map<String, Object>> frontendData = currentValue
        .stream()
        .map(FileUploaderComponent::toFrontendValue)
        .toList();
    try {
      return Shared.OBJECT_MAPPER.writeValueAsString(frontendData);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize file uploader value", e);
    }
  }

  // used in the template
  @SuppressWarnings("unused")
  protected String getTypesJson() {
    if (types == null || types.isEmpty()) {
      return "[]";
    }
    try {
      return Shared.OBJECT_MAPPER.writeValueAsString(types);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize types", e);
    }
  }

  public static Map<String, Object> toFrontendValue(final @Nonnull JtUploadedFile file) {
    return Map.of(FRONTEND_FILENAME_KEY,
                  file.filename(),
                  "contentType",
                  file.contentType() != null ? file.contentType() : "",
                  "size",
                  file.content().length);
  }
}
