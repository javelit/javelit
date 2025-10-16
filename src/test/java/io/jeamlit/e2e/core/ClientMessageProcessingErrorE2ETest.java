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
package io.jeamlit.e2e.core;

import io.jeamlit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for client message processing error handling.
 * Tests that errors thrown during component validation are properly displayed to the user.
 */
public class ClientMessageProcessingErrorE2ETest {

    @Test
    void testValidateErrorShowsModal(TestInfo testInfo) {
        // App with a minimal component that throws an error in validate()
        final @Language("java") String app = """
                import io.jeamlit.core.Jt;
                import io.jeamlit.core.JtComponent;
                import io.jeamlit.core.JtComponentBuilder;
                import com.fasterxml.jackson.core.type.TypeReference;

                public class TestApp {

                    // Minimal component with a button that triggers validation error
                    static class MinimalButtonComponent extends JtComponent<Boolean> {
                        private final String key;

                        static class Builder extends JtComponentBuilder<Boolean, MinimalButtonComponent, Builder> {
                            @Override
                            public MinimalButtonComponent build() {
                                return null; // Not used
                            }
                        }

                        public MinimalButtonComponent(String userKey) {
                            super(new Builder().key(userKey), false, null);
                            this.key = userKey;
                        }

                        @Override
                        protected Boolean validate(Boolean value) {
                            throw new RuntimeException("Validation failed!");
                        }

                        @Override
                        protected String register() {
                            return null; // No web component needed
                        }

                        @Override
                        protected String render() {
                            return "<button onclick=\\"window.jeamlit.sendComponentUpdate('" + getInternalKey() + "', true)\\">Click me</button>";
                        }

                        @Override
                        protected TypeReference<Boolean> getTypeReference() {
                            return new TypeReference<>() {};
                        }

                        @Override
                        protected void resetIfNeeded() {
                            currentValue = false;
                        }
                    }

                    public static void main(String[] args) {
                        Jt.title("Test App").use();
                        new MinimalButtonComponent("test-button").use();
                    }
                }
                """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Wait for the app to load
            assertThat(page.getByText("Test App")).isVisible(WAIT_1_SEC_MAX);

            // Click the button
            page.getByText("Click me").click();

            // Assert that the error modal appears with the expected text
            assertThat(page.getByText("Client message processing error")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
