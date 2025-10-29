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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

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

import static com.google.common.base.Preconditions.checkArgument;

public final class PdfComponent extends JtComponent<JtComponent.NONE> {

    // visible to the template engine
    final @Nonnull String url;
    final @Nonnull String height;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/media/PdfComponent.register.html.mustache");
        renderTemplate = mf.compile("components/media/PdfComponent.render.html.mustache");
    }


    private PdfComponent(final @Nonnull Builder builder) {
        super(builder, NONE.NONE_VALUE, null);
        if (builder.url != null) {
            this.url = builder.url;
        } else {
            this.url = registerMedia(new MediaEntry(builder.bytes, "application/pdf"));
        }
        this.height = builder.height;
    }

    public static class Builder extends JtComponentBuilder<NONE, PdfComponent, Builder> {
        // url supports both local from static folder and distant
        private @Nullable final String url;
        private @Nullable final byte[] bytes;
        private String height = "500";

        public Builder(final @Nullable String url, final @Nullable byte[] bytes) {
            this.url = url;
            this.bytes = bytes;
        }

        public static Builder of(final @Nonnull String url) {
            return new Builder(url, null);
        }

        public static Builder of(final @Nonnull byte[] data) {
            return new Builder(null, data);
        }

        public static Builder of(final @Nonnull Path localFile) {
            try {
                final byte[] bytes = Files.readAllBytes(localFile);
                checkArgument(bytes.length > 0, "File " + localFile + " is empty");
                return new Builder(null, bytes);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read bytes from file" + e);
            }
        }

        public static Builder of(final @Nonnull JtUploadedFile uploadedFile) {
            return new Builder(null, uploadedFile.content());
        }

        /**
         * The height of the PDF viewer element. This can be one of the following:
         * <ul>
         * <li>{@code stretch}: The height of the element matches the height of its content or the height of the parent container, whichever is larger. If the element is not in a parent container, the height of the element matches the height of its content.</li>
         * <li>An integer specifying the height in pixels (default: 500): The element has a fixed height.</li>
         * </ul>
         */
        public Builder height(final @Nullable String height) {
            if (height != null && !"stretch".equals(height) && !height.matches("\\d+")) {
                throw new IllegalArgumentException(
                        "height must be 'stretch' or a pixel value (integer). Got: " + height);
            }
            this.height = height;
            return this;
        }

        /**
         * The height of the element in pixels. The element will have a fixed height.
         */
        public Builder height(final int heightPixels) {
            if (heightPixels < 0) {
                throw new IllegalArgumentException("Height in pixels must be non-negative. Got: " + heightPixels);
            }
            this.height = String.valueOf(heightPixels);
            return this;
        }

        @Override
        public PdfComponent build() {
            return new PdfComponent(this);
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
    protected TypeReference<NONE> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
