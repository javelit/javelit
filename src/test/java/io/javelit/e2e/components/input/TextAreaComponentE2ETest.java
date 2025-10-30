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
package io.javelit.e2e.components.input;

import com.microsoft.playwright.Locator;
import io.javelit.e2e.helpers.OsUtils;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for TextAreaComponent.
 */
public class TextAreaComponentE2ETest {
    
    @Test
    void testTextAreaInput(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import io.javelit.components.input.TextAreaComponent;
            
            public class TestApp {
                public static void main(String[] args) {
                    String text = new TextAreaComponent.Builder("Enter your message")
                        .placeholder("Type here...")
                        .height(150)
                        .build()
                        .use();
                    Jt.text("Message: " + text).use();
                }
            }
            """;

        // text area input is visible
        // current message is empty
        // Type multi-line text in the textarea
        // Press Cmd/Ctrl+Enter to submit (TextArea's default submit behavior)
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // text area input is visible
            assertThat(page.locator("jt-text-area")).isVisible(WAIT_1_SEC_MAX);
            // current message is empty
            final Locator byText = page.getByText("Message: ");
            assertThat(byText).isVisible(WAIT_1_SEC_MAX);
            // Type multi-line text in the textarea
            final Locator textarea = page.locator("jt-text-area textarea");
            textarea.fill("Line 1\nLine 2\nLine 3");
            // Press Cmd/Ctrl+Enter to submit (TextArea's default submit behavior)
            final OsUtils.OS os = OsUtils.getOS();
            page.keyboard().down(os.modifier);
            page.keyboard().press("Enter");
            page.keyboard().up(os.modifier);
            assertThat(page.getByText("Message: Line 1\nLine 2\nLine 3")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
