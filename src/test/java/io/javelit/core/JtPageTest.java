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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JtPageTest {

  private static Stream<Arguments> pathToTitleTestCases() {
    return Stream.of(
        // Simple paths with hyphens
        Arguments.of("/my-dashboard", "My Dashboard"),
        Arguments.of("/user-profile", "User Profile"),
        Arguments.of("/api-endpoint", "Api Endpoint"),

        // Simple paths with underscores
        Arguments.of("/user_profile", "User Profile"),
        Arguments.of("/my_page", "My Page"),

        // Mixed separators
        Arguments.of("/my-awesome_page", "My Awesome Page"),
        Arguments.of("/user_profile-settings", "User Profile Settings"),

        // With numbers
        Arguments.of("/page-2", "Page 2"),
        Arguments.of("/api-v2", "Api V2"),
        Arguments.of("/version-123-beta", "Version 123 Beta"),

        // Multiple consecutive separators
        Arguments.of("/my--page", "My Page"),
        Arguments.of("/user__profile", "User Profile"),
        Arguments.of("/mixed-_-separators", "Mixed Separators"),

        // Single word
        Arguments.of("/dashboard", "Dashboard"),
        Arguments.of("/settings", "Settings"),

        // Without leading slash
        Arguments.of("my-page", "My Page"),
        Arguments.of("dashboard", "Dashboard"),

        // Empty or root path
        Arguments.of("/", "Home"),
        Arguments.of("", "Home"),

        // Already capitalized
        Arguments.of("/MyPage", "MyPage"),
        Arguments.of("/ALLCAPS", "ALLCAPS"),

        // Long paths
        Arguments.of("/this-is-a-very-long-page-title", "This Is A Very Long Page Title"),

        // Nested path
        Arguments.of("/nested/path", "Nested Path"),

        // Edge cases
        Arguments.of("/-", ""),
        Arguments.of("/_", ""),
        Arguments.of("/--", ""));
  }

  @ParameterizedTest
  @MethodSource("pathToTitleTestCases")
  public void testPathToTitle(final String input, final String expected) {
    assertThat(JtPage.Builder.pathToTitle(input)).isEqualTo(expected);
  }

  public static Stream<Arguments> reservedPathCases() {
    return Stream.of(Arguments.of("_"),
                     Arguments.of("/_"),
                     Arguments.of("/_/"),
                     Arguments.of("app"),
                     Arguments.of("/app"),
                     Arguments.of("/app/")
    );
  }

  @ParameterizedTest
  @MethodSource("reservedPathCases")
  public void testReservedPathThrows(final String input) {
    assertThatThrownBy(() -> JtPage.builder(input, () -> {
    })).isInstanceOf(IllegalArgumentException.class);
  }
}
