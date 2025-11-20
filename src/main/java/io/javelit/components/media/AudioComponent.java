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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
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

import static com.google.common.base.Preconditions.checkArgument;

public final class AudioComponent extends JtComponent<JtComponent.NONE> {

  private static final Map<String, String> MIME_CONVERSIONS = Map.of(
      "audio/x-wav", "audio/wav",
      "audio/vnd.wave", "audio/wav",
      "audio/mpeg3", "audio/mpeg",
      "audio/x-mpeg-3", "audio/mpeg"
  );

  // visible to the template engine
  final @Nonnull String url;
  final @Nonnull String format;
  final @Nullable Long startTimeMillis;
  final @Nullable Long endTimeMillis;
  final boolean loop;
  final boolean autoplay;
  final @Nonnull String width;

  private static final Mustache registerTemplate;
  private static final Mustache renderTemplate;

  static {
    final MustacheFactory mf = new DefaultMustacheFactory();
    registerTemplate = mf.compile("components/media/AudioComponent.register.html.mustache");
    renderTemplate = mf.compile("components/media/AudioComponent.render.html.mustache");
  }

  private AudioComponent(final @Nonnull Builder builder) {
    super(builder, NONE.NONE_VALUE, null);
    if (builder.url != null) {
      this.url = builder.url;
    } else {
      this.url = registerMedia(new MediaEntry(builder.bytes, builder.format));
    }
    this.format = builder.format;
    this.loop = builder.loop;
    this.autoplay = builder.autoplay;
    this.width = builder.width;
    this.startTimeMillis = builder.startTime == null ? null : builder.startTime.toMillis();
    this.endTimeMillis = builder.endTime == null ? null : builder.endTime.toMillis();
  }

  public static class Builder extends JtComponentBuilder<NONE, AudioComponent, Builder> {
    // url supports both local from static folder and distant
    private @Nullable final String url;
    private @Nullable final byte[] bytes;
    // mimeType
    private @Nullable String format;
    private @Nullable Duration startTime;
    private @Nullable Duration endTime;
    private boolean loop;
    private boolean autoplay;
    private String width = "stretch";

    public Builder(final @Nonnull String url) {
      this.url = url;
      this.bytes = null;
      this.format = URLConnection.guessContentTypeFromName(url);
    }

    public Builder(final @Nonnull byte[] data) {
      this.url = null;
      this.bytes = data;
      String result;
      try (final InputStream in = new ByteArrayInputStream(this.bytes)) {
        result = URLConnection.guessContentTypeFromStream(in);
      } catch (IOException e) {
        throw new RuntimeException("Failed to infer format (MIME type) from the first bytes of the input data",
                                   e);
      }
      this.format = result;
    }

    public Builder(final @Nonnull Path localFile) {
      this.url = null;
      try {
        this.bytes = Files.readAllBytes(localFile);
        checkArgument(bytes.length > 0, "File " + localFile + " is empty");
        this.format = Files.probeContentType(localFile);
      } catch (IOException e) {
        throw new RuntimeException("Failed to read bytes from file" + e);
      }
    }

    public static Builder of(final @Nonnull JtUploadedFile uploadedFile) {
      return new Builder(uploadedFile.content());
    }

    /**
     * The MIME type for the audio file. For URLs, this defaults to "audio/wav".
     * For files and raw data, Javelit attempts to infer the MIME type.
     * For more information about MIME types, see https://www.iana.org/assignments/media-types/media-types.xhtml.
     */
    public Builder format(final @Nonnull String format) {
      checkArgument(format.toLowerCase(Locale.ROOT).startsWith("audio/"),
                    "Invalid format: %s. The format should start with `audio/`.", format);
      this.format = format;
      return this;
    }

    /**
     * The time from which the element should start playing as a {@code java.time.Duration},
     * rounded to the milliseconds.
     * If {@code null} (default), the element plays from the beginning.
     */
    public Builder startTime(final @Nullable Duration startTime) {
      this.startTime = startTime;
      return this;
    }

    /**
     * The time from which the element should start playing in seconds.
     * By default, the element plays from the beginning.
     */
    public Builder startTime(final int seconds) {
      this.startTime = Duration.ofSeconds(seconds);
      return this;
    }

    /**
     * The time at which the element should stop playing as a {@code java.time.Duration},
     * rounded to the milliseconds.
     * If {@code null} (default), the element plays through to the end.
     */
    public Builder endTime(final @Nonnull Duration endTime) {
      this.endTime = endTime;
      return this;
    }

    /**
     * The time at which the element should stop playing in seconds.
     * By default, the element plays through to the end.
     */
    public Builder endTime(final int seconds) {
      this.endTime = Duration.ofSeconds(seconds);
      return this;
    }

    /**
     * Whether the audio should loop playback. {@code False} by default.
     */
    public Builder loop(final boolean loop) {
      this.loop = loop;
      return this;
    }

    /**
     * Whether the audio file should start playing automatically. This is {@code False} by default.
     * Browsers will not autoplay audio files if the user has not interacted with the page
     * by clicking somewhere.
     */
    public Builder autoplay(final boolean autoplay) {
      this.autoplay = autoplay;
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
    public AudioComponent build() {
      checkArgument(format != null,
                    "Could not infer format (MIME type) from the input data. Please provide the format explicitly with `.format()` or make sure the input is valid audio data.");
      this.format = MIME_CONVERSIONS.getOrDefault(format, format);
      if (startTime != null && endTime != null) {
        checkArgument(startTime.toMillis() < endTime.toMillis(),
                      "Start time %s must be strictly smaller than end time %s. Note that both times are rounded to the milliseconds.",
                      startTime,
                      endTime);
      }
      return new AudioComponent(this);
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
