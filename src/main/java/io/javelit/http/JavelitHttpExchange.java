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
package io.javelit.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface JavelitHttpExchange {
  String method();
  String path();            // path excluding query
  Map<String, Deque<String>> queryParameters();
  String firstHeader(String name);
  List<String> headers(String name);
  @Nullable JavelitCookie cookie(String name);
  InputStream inputBody() throws IOException;
  // full url - encoded - this api may change
  String getRequestURL();
  // request host IP address string in textual presentation
  String getRemoteHostAddress();
  // Get the request relative path.
  String getRelativePath();
  // returns null if the request is not multipart/form-data
  @Nullable JavelitMultiPart getMultiPartFormData() throws IOException;


  void setStatus(int status);
  void setHeader(String name, String value);
  void addHeader(String name, String value);
  void setCookie(@Nonnull JavelitCookie cookie);
  List<JavelitCookie> responseCookies();
  OutputStream outputBody() throws IOException;

  // write should close the stream
  void write(String data);
  void write(ByteBuffer buffer);

  JavelitSession getOrCreateSession();

  @Nullable JavelitSession getSession();


  default void addHeader(String name, int value) {
    addHeader(name, String.valueOf(value));
  }

}
