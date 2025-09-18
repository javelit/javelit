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
package io.jeamlit.components.media;

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
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import io.jeamlit.core.JtUploadedFile;
import io.jeamlit.core.Shared;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.jeamlit.core.utils.Preconditions.checkArgument;

public class FileUploaderComponent extends JtComponent<List<JtUploadedFile>> {

    public static String FRONTEND_FILENAME_KEY = "filename";

    private static final Logger LOG = LoggerFactory.getLogger(FileUploaderComponent.class);

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
        super(builder.generateKeyForInteractive(), List.of(), builder.onChange);
        this.label = markdownToHtml(builder.label, true);
        this.types = builder.types;
        this.acceptMultipleFiles = builder.acceptMultipleFiles;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
    }

    public static class Builder extends JtComponentBuilder<List<JtUploadedFile>, FileUploaderComponent, Builder> {

        @Language("markdown")
        private final @NotNull String label;
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

        public Builder type(final @Nullable List<String> types) {
            if (types != null) {
                checkArgument(!types.isEmpty(),
                              "FileUploader types cannot be an empty list. Use null to allow all types.");
                types.forEach(t -> checkArgument(
                        t.startsWith(".") || t.contains("/"),
                        "Types must be file extension (.pdf) or MIME type (image/png): %s", t
                ));
            }

            this.types = types;
            return this;
        }

        public Builder acceptMultipleFiles(final MultipleFiles acceptMultipleFiles) {
            this.acceptMultipleFiles = acceptMultipleFiles;
            return this;
        }

        public Builder help(final @Nullable String help) {
            this.help = help;
            return this;
        }

        public Builder disabled(final boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder onChange(final @Nullable Consumer<List<JtUploadedFile>> onChange) {
            this.onChange = onChange;
            return this;
        }

        public Builder labelVisibility(final LabelVisibility labelVisibility) {
            this.labelVisibility = labelVisibility;
            return this;
        }

        public Builder width(final String width) {
            if (width != null && !"stretch".equals(width) && !width.matches("\\d+")) {
                throw new IllegalArgumentException("width must be 'stretch' or a pixel value (integer). Got: " + width);
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
                if (!(file instanceof Map<?, ?> m)) {
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
        final List<Map<String, Object>> frontendData = currentValue.stream()
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
        return Map.of(
                FRONTEND_FILENAME_KEY, file.filename(),
                "contentType", file.contentType() != null ? file.contentType() : "",
                "size", file.content().length
        );
    }
}
