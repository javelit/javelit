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
package io.javelit.undertow;

import java.io.IOException;

import io.javelit.http.JavelitPart;
import io.undertow.server.handlers.form.FormData;
import jakarta.annotation.Nonnull;

class UndertowJavelitPart implements JavelitPart {
  private final String fieldName;
  private final FormData.FormValue formValue;

  private UndertowJavelitPart(final @Nonnull String fieldName, final @Nonnull FormData.FormValue formValue) {
    this.fieldName = fieldName;
    this.formValue = formValue;
  }

  static JavelitPart of(final @Nonnull String fieldName, final @Nonnull FormData.FormValue formValue) {
    return new UndertowJavelitPart(fieldName, formValue);
  }

  @Override
  public String name() {
    return fieldName;
  }

  @Override
  public boolean isFile() {
    return formValue.isFile();
  }

  @Override
  public String filename() {
    return formValue.getFileName();
  }

  @Override
  public String contentType() {
    return formValue.getHeaders().getFirst("Content-Type");
  }

  @Override
  public byte[] bytes() throws IOException {
    if (!formValue.isFile()) {
      // only compatible with files - this is ok for the moment as this is the only kind that is used in Javelit
      throw new IllegalStateException("Unsupported operation. Cannot get bytes from non-file form field: " + fieldName);
    }
    return formValue.getFileItem().getInputStream().readAllBytes();
  }
}
