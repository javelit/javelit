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
package io.javelit.core;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class JtPageTest {

    private static Stream<Arguments> camelCaseToTitleTestCases() {
        return Stream.of(
                // Simple camelCase
                Arguments.of("SimpleName", "Simple Name"),

                // With numbers
                Arguments.of("Page1", "Page 1"),
                Arguments.of("MyPage2", "My Page 2"),
                Arguments.of("App123", "App 123"),
                Arguments.of("Test2Page", "Test 2 Page"),

                // Consecutive caps (acronyms)
                Arguments.of("HTTPServer", "HTTP Server"),

                // Mixed patterns
                Arguments.of("MyHTTPServer", "My HTTP Server"),
                Arguments.of("HTTPServer2", "HTTP Server 2"),
                Arguments.of("MyXMLParser123", "My XML Parser 123"),

                // Edge cases - single word
                Arguments.of("Page", "Page"),
                Arguments.of("ABC", "ABC"),
                Arguments.of("a", "a"),

                // Empty string
                Arguments.of("", ""),

                // Lowercase start
                Arguments.of("myClass", "my Class"),

                // Multiple consecutive numbers
                Arguments.of("Version123Beta", "Version 123 Beta")
        );
    }

    @ParameterizedTest
    @MethodSource("camelCaseToTitleTestCases")
    public void testCamelCaseToTitle(final String input, final String expected) {
        assertThat(JtPage.Builder.camelCaseToTitle(input)).isEqualTo(expected);
    }
}
