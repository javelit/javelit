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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javelit.core.JtComponent;
import io.javelit.core.JtComponentBuilder;
import io.javelit.core.JtUploadedFile;
import io.javelit.core.MediaEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkArgument;

public class AudioInputComponent extends JtComponent<JtUploadedFile> {

    protected final @NotNull String label;
    protected final @Nullable Integer sampleRate;
    protected final @Nullable String help;
    protected final boolean disabled;
    protected final @Nonnull LabelVisibility labelVisibility;
    protected final @Nonnull String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/media/AudioInputComponent.register.html.mustache");
        renderTemplate = mf.compile("components/media/AudioInputComponent.render.html.mustache");
    }

    private AudioInputComponent(final Builder builder) {
        super(builder, null, builder.onChange);
        this.label = markdownToHtml(builder.label, true);
        this.sampleRate = builder.sampleRate;
        this.help = builder.help;
        this.disabled = builder.disabled;
        this.labelVisibility = builder.labelVisibility;
        this.width = builder.width;
    }


    public static class Builder extends JtComponentBuilder<JtUploadedFile, AudioInputComponent, Builder> {
        public static final Set<Integer> VALID_SAMPLE_RATES = Set.of(8000,
                                                                     11025,
                                                                     16000,
                                                                     22050,
                                                                     24000,
                                                                     32000,
                                                                     44100,
                                                                     48000);
        @Language("markdown") private final @NotNull String label;
        private @Nullable Integer sampleRate = 16000;
        private @Nullable Consumer<JtUploadedFile> onChange;
        private @Nullable String help;
        private boolean disabled;
        private LabelVisibility labelVisibility = LabelVisibility.VISIBLE;
        private String width = "stretch";

        public Builder(final @Language("markdown") @NotNull String label) {
            checkArgument(!label.isBlank(), "FileUploader label cannot be null or empty.");
            this.label = label;
        }

        /**
         * The target sample rate for the audio recording in Hz. This defaults to 16000 Hz, which is optimal for
         * speech recognition.
         * <p>
         * The following sample rates are supported: 8000, 11025, 16000, 22050, 24000, 32000, 44100, or 48000.
         * If this is set to {@code null}, the widget uses the browser's default sample rate
         * (typically 44100 or 48000 Hz).
         */
        public Builder sampleRate(final @Nullable Integer sampleRate) {
            checkArgument(sampleRate == null
                          || VALID_SAMPLE_RATES.contains(sampleRate),
                          "Invalid sample rate value %s. Sample rate must be in %s or set to null to use the browser default sample rate.",
                          sampleRate,
                          VALID_SAMPLE_RATES);
            this.sampleRate = sampleRate;
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
        public Builder onChange(final @Nullable Consumer<JtUploadedFile> onChange) {
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
        public AudioInputComponent build() {
            return new AudioInputComponent(this);
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
    protected TypeReference<JtUploadedFile> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    @Override
    protected JtUploadedFile convert(final Object rawValue) {
        if (rawValue instanceof List<?> files
            && files.size() == 1 && files.getFirst() instanceof JtUploadedFile jtF) {
            // upload from the PUT upload route
            return jtF;
        } else {
            throw new RuntimeException(
                    "Failed to parse input widget value coming from the app. FileUpload value is not a list. Please reach out to support.");
        }
    }

    // used in the template
    @SuppressWarnings("unused")
    protected String getCurrentValueUrl() {
        if (currentValue == null) {
            return null;
        }
        return registerMedia(new MediaEntry(currentValue.content(), currentValue.contentType()));
    }
}
