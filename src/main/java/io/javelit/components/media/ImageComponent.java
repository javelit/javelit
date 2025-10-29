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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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

import static com.google.common.base.Preconditions.checkArgument;

public final class ImageComponent extends JtComponent<JtComponent.NONE> {

    private static final Map<String, String> MIME_CONVERSIONS = Map.of();

    // visible to the template engine
    final @Nonnull String url;
    final @Language("html") @Nullable String caption;
    final @Nonnull String width;

    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/media/ImageComponent.register.html.mustache");
        renderTemplate = mf.compile("components/media/ImageComponent.render.html.mustache");
    }


    private ImageComponent(final @Nonnull Builder builder) {
        super(builder, NONE.NONE_VALUE, null);
        if (builder.url != null) {
            this.url = builder.url;
        } else if (builder.bytes != null) {
            this.url = registerMedia(new MediaEntry(builder.bytes, builder.format));
        } else {
            this.url = convertSvgToUrl(builder.svg);
        }
        this.caption = markdownToHtml(builder.caption, true);
        this.width = builder.width;
    }

    private static String convertSvgToUrl(final @Language("html") String svgString) {
        // this encoding should work and is lighter than base64 (~30%)
        // if it does not for some edge cases, replace with
        // Base64.getEncoder().encodeToString(svgString.getBytes(StandardCharsets.UTF_8));
        // return "data:image/svg+xml;base64," + base64;
        final String encodedSvg = URLEncoder.encode(svgString, StandardCharsets.UTF_8).replace("+", "%20");
        return "data:image/svg+xml," + encodedSvg;

    }

    public static class Builder extends JtComponentBuilder<NONE, ImageComponent, Builder> {
        // url supports both local from static folder and distant
        private @Nullable final String url;
        private @Nullable final byte[] bytes;
        private @Language("html") @Nullable final String svg;
        // mimeType
        private @Nullable String format;
        private @Language("markdown") @Nullable String caption;
        private String width = "content";

        public Builder(final @Nullable String url,
                       @Language("html") final @Nullable String svg,
                       final @Nullable byte[] bytes,
                       final @org.jetbrains.annotations.Nullable String format) {
            this.url = url;
            this.svg = svg;
            this.bytes = bytes;
            // not used
            this.format = format;
        }

        public static Builder of(final @Nonnull String url) {
            return new Builder(url, null, null, null);
        }

        public static Builder of(final @Nonnull byte[] data) {
            try (final InputStream in = new ByteArrayInputStream(data)) {
                final String format = URLConnection.guessContentTypeFromStream(in);
                return new Builder(null, null, data, format);
            } catch (IOException e) {
                throw new RuntimeException("Failed to infer format (MIME type) from the first bytes of the input data",
                                           e);
            }
        }

        public static Builder of(final @Nonnull Path localFile) {
            try {
                final byte[] bytes = Files.readAllBytes(localFile);
                checkArgument(bytes.length > 0, "File " + localFile + " is empty");
                final String format = Files.probeContentType(localFile);
                return new Builder(null, null, bytes, format);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read bytes from file" + e);
            }
        }

        public static Builder ofSvg(final @Language("html") @Nonnull String svg) {
            return new Builder(null, svg, null, null);
        }

        public static Builder of(final @Nonnull JtUploadedFile uploadedFile) {
            return of(uploadedFile.content());
        }

        /**
         * Image caption. If this is {@code null} (default), no caption is displayed.
         * Markdown is supported, see {@link io.javelit.core.Jt#markdown(String)} for more details.
         */
        public Builder caption(@Language("markdown") @Nullable final String caption) {
            this.caption = caption;
            return this;
        }

        /**
         * The width of the element. This can be one of the following:
         * <ul>
         * <li>{@code content} (default): The width of the element matches the width of its content, but doesn't exceed the width of the parent container.</li>
         * <li>{@code stretch}: The width of the element matches the width of the parent container.</li>
         * <li>An integer specifying the width in pixels: The element has a fixed width. If the specified width is greater than the width of the parent container, the width of the element matches the width of the parent container.</li>
         * </ul>
         */
        public Builder width(final @Nullable String width) {
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
        public Builder width(final int widthPixels) {
            if (widthPixels < 0) {
                throw new IllegalArgumentException("Width in pixels must be non-negative. Got: " + widthPixels);
            }
            this.width = String.valueOf(widthPixels);
            return this;
        }

        @Override
        public ImageComponent build() {
            if (format != null) {
                this.format = MIME_CONVERSIONS.getOrDefault(format, format);
            }
            return new ImageComponent(this);
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
