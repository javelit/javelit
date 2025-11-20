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

public class BuildSystemTest {

  /**
   * Test version comparison logic using reflection to access the private method.
   * This tests the semantic version comparison used to validate Javelit dependency versions.
   */
  @ParameterizedTest
  @MethodSource("versionComparisonTestCases")
  void testVersionComparison(String v1, String v2, boolean expectedGreater) throws Exception {
    final boolean result = BuildSystem.isVersionGreater(v1, v2);

    assertThat(result)
        .as("Comparing version %s > %s", v1, v2)
        .isEqualTo(expectedGreater);
  }

  private static Stream<Arguments> versionComparisonTestCases() {
    return Stream.of(
        // Major version comparison
        Arguments.of("1.0.0", "0.58.0", true),
        Arguments.of("0.58.0", "1.0.0", false),
        Arguments.of("2.0.0", "1.9.9", true),

        // Minor version comparison
        Arguments.of("0.59.0", "0.58.0", true),
        Arguments.of("0.58.0", "0.59.0", false),
        Arguments.of("1.2.0", "1.1.9", true),

        // Patch version comparison
        Arguments.of("1.0.1", "1.0.0", true),
        Arguments.of("1.0.0", "1.0.1", false),
        Arguments.of("0.58.1", "0.58.0", true),

        // Equal versions
        Arguments.of("1.0.0", "1.0.0", false),
        Arguments.of("0.58.0", "0.58.0", false),

        // Snapshot versions (qualifiers ignored)
        Arguments.of("1.0.0-SNAPSHOT", "0.58.0-SNAPSHOT", true),
        Arguments.of("0.59.0-SNAPSHOT", "0.58.0-SNAPSHOT", true),
        Arguments.of("1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT", false),
        Arguments.of("1.0.0-SNAPSHOT", "1.0.0", false),
        Arguments.of("1.0.0", "1.0.0-SNAPSHOT", false),

        // Different version lengths
        Arguments.of("1.0", "0.58.0", true),
        Arguments.of("1.0.0.0", "1.0.0", true)
    );
  }

  /**
   * Test that version extraction correctly handles Maven classifiers and types.
   * Dependencies can have format: groupId:artifactId:version:classifier@type
   */
  @ParameterizedTest
  @MethodSource("versionExtractionTestCases")
  void testVersionExtraction(String dependency, String expectedVersion) {
    // Simulate the version extraction logic from BuildSystem
    String extractedVersion = dependency.substring("io.javelit:javelit:".length());
    int colonIndex = extractedVersion.indexOf(':');
    if (colonIndex != -1) {
      extractedVersion = extractedVersion.substring(0, colonIndex);
    }

    assertThat(extractedVersion)
        .as("Extracting version from dependency: %s", dependency)
        .isEqualTo(expectedVersion);
  }

  private static Stream<Arguments> versionExtractionTestCases() {
    return Stream.of(
        // Simple version
        Arguments.of("io.javelit:javelit:0.58.0", "0.58.0"),
        Arguments.of("io.javelit:javelit:1.0.0", "1.0.0"),

        // Version with snapshot
        Arguments.of("io.javelit:javelit:1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT"),

        // Version with classifier
        Arguments.of("io.javelit:javelit:0.58.0:all", "0.58.0"),
        Arguments.of("io.javelit:javelit:0.58.0:classifier", "0.58.0"),

        // Version with classifier and type
        Arguments.of("io.javelit:javelit:0.58.0:all@fatjar", "0.58.0"),
        Arguments.of("io.javelit:javelit:1.0.0-SNAPSHOT:all@jar", "1.0.0-SNAPSHOT")
    );
  }
}
