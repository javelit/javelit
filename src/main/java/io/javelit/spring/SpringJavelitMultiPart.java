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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.javelit.http.JavelitMultiPart;
import io.javelit.http.JavelitPart;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.server.ServerWebExchange;

class SpringJavelitMultiPart implements JavelitMultiPart {

  private final Map<String, List<Part>> formData;

  private SpringJavelitMultiPart(final @Nonnull Map<String, List<Part>> formData) {
    this.formData = formData;
  }

  static @Nullable JavelitMultiPart of(final @Nonnull ServerWebExchange exchange) throws IOException {
    final String contentType = exchange.getRequest().getHeaders().getContentType() != null ?
        exchange.getRequest().getHeaders().getContentType().toString() : "";

    if (!contentType.startsWith("multipart/form-data")) {
      return null;
    }

    try {
      final Map<String, List<Part>> multipartData = exchange.getMultipartData().block();
      if (multipartData == null || multipartData.isEmpty()) {
        return null;
      }
      return new SpringJavelitMultiPart(multipartData);
    } catch (Exception e) {
      throw new IOException("Failed to parse multipart data", e);
    }
  }

  @Override
  public List<JavelitPart> parts() {
    final ArrayList<JavelitPart> res = new ArrayList<>();
    for (final Map.Entry<String, List<Part>> entry : formData.entrySet()) {
      final String fieldName = entry.getKey();
      for (final Part part : entry.getValue()) {
        res.add(SpringJavelitPart.of(fieldName, part));
      }
    }
    return res;
  }

  @Override
  public void close() throws Exception {
    // Spring manages cleanup automatically
  }
}
