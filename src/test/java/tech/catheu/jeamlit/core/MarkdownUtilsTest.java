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
package tech.catheu.jeamlit.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkdownUtilsTest {

    private static Stream<Arguments> testCases() {

        return Stream.of(Arguments.of("# This is a title",
                                      "<h1>This is a title</h1>",
                                      "This is a title"),
                         Arguments.of("A basic text", "<p>A basic text</p>", "A basic text"),
                         Arguments.of("_Jeamlit_ is cool :sunglasses:",
                                      "<p><em>Jeamlit</em> is cool \uD83D\uDE0E</p>",
                                      "<em>Jeamlit</em> is cool \uD83D\uDE0E"),
                         Arguments.of("""
                                              Breaking stuff
                                              ## A title
                                              With some content
                                              """, """
                                              <p>Breaking stuff</p>
                                              <h2>A title</h2>
                                              <p>With some content</p>""", """
                                              <p>Breaking stuff</p>
                                              <h2>A title</h2>
                                              <p>With some content</p>"""));
    }


    @ParameterizedTest
    @MethodSource("testCases")
    public void testMarkdownConversion(final String input, final String keepWrap, final String removeWrap) {
        assertThat(MarkdownUtils.markdownToHtml(input)).isEqualTo(keepWrap);
        assertThat(MarkdownUtils.markdownToHtml(input, false)).isEqualTo(keepWrap);
        assertThat(MarkdownUtils.markdownToHtml(input, true)).isEqualTo(removeWrap);
    }
}
