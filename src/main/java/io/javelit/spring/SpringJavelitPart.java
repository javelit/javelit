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
package io.javelit.spring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.javelit.http.JavelitPart;
import jakarta.annotation.Nonnull;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;

class SpringJavelitPart implements JavelitPart {
  private final String fieldName;
  private final Part part;

  private SpringJavelitPart(final @Nonnull String fieldName, final @Nonnull Part part) {
    this.fieldName = fieldName;
    this.part = part;
  }

  static JavelitPart of(final @Nonnull String fieldName, final @Nonnull Part part) {
    return new SpringJavelitPart(fieldName, part);
  }

  @Override
  public String name() {
    return fieldName;
  }

  @Override
  public boolean isFile() {
    return part instanceof FilePart;
  }

  @Override
  public String filename() {
    if (part instanceof FilePart filePart) {
      return filePart.filename();
    }
    return null;
  }

  @Override
  public String contentType() {
    return part.headers().getContentType() != null ?
        part.headers().getContentType().toString() : null;
  }

  @Override
  public byte[] bytes() throws IOException {
    if (!isFile()) {
      // only compatible with files - this is ok for the moment as this is the only kind that is used in Javelit
      throw new IllegalStateException("Unsupported operation. Cannot get bytes from non-file form field: " + fieldName);
    }

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    part.content()
        .doOnNext(dataBuffer -> {
          try {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            outputStream.write(bytes);
            DataBufferUtils.release(dataBuffer);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .doOnComplete(() -> {
          try {
            outputStream.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .blockLast();

    return outputStream.toByteArray();
  }
}
