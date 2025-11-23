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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.javelit.http.JavelitMultiPart;
import io.javelit.http.JavelitPart;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class UndertowJavelitMultiPart implements JavelitMultiPart {
  private static final FormParserFactory FORM_PARSER_FACTORY;

  static {
    final FormParserFactory.Builder parserBuilder = FormParserFactory.builder();
    parserBuilder.setDefaultCharset(StandardCharsets.UTF_8.name());
    FORM_PARSER_FACTORY = parserBuilder.build();
  }

  private final FormData formData;
  private final FormDataParser parser;

  private UndertowJavelitMultiPart(final @Nonnull FormData formData, final @Nonnull FormDataParser parser) {
    this.formData = formData;
    this.parser = parser;
  }

  static @Nullable JavelitMultiPart of(final @Nonnull HttpServerExchange exchange) throws IOException {
    final FormDataParser parser = FORM_PARSER_FACTORY.createParser(exchange);
    if (parser == null) {
      return null;
    }
    final FormData formData = parser.parseBlocking();
    if (formData == null) {
      return null;
    }
    return new UndertowJavelitMultiPart(formData, parser);
  }

  @Override
  public List<JavelitPart> parts() {
    final ArrayList<JavelitPart> res = new ArrayList<>();
    for (final @Nonnull String fieldName : formData) {
      formData.get(fieldName).forEach(formValue -> res.add(UndertowJavelitPart.of(fieldName, formValue)));
    }
    return res;
  }

  @Override
  public void close() throws Exception {
    parser.close();
  }

}
