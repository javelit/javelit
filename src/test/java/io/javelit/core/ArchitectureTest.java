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

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Architecture tests to enforce design rules and encapsulation.
 */
public class ArchitectureTest {

  private static final JavaClasses CORE_CLASSES = new ClassFileImporter()
      .importPackages("io.javelit.core");

  @Test
  void internalSessionStateShouldOnlyBeAccessedByStateManager() {
    ArchRule rule = classes()
        .that().belongToAnyOf(InternalSessionState.class)
        .should().onlyBeAccessed().byClassesThat().belongToAnyOf(InternalSessionState.class, StateManager.class);

    rule.check(CORE_CLASSES);
  }
}
