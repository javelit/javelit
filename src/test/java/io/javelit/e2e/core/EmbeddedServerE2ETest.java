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
package io.javelit.e2e.core;

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_100_MS_MAX_CLICK;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

public class EmbeddedServerE2ETest {

    // do not convert to local variable
    private final String instanceString = "Hello";

    @Deprecated
    @Test
    public void testEmbeddedServerFromClassE2E(final TestInfo testInfo) {
        PlaywrightUtils.runInBrowser(testInfo, TestApp.class, page -> {
            assertThat(page.locator("jt-button")).isVisible(WAIT_1_SEC_MAX);
            page.locator("jt-button button").click(WAIT_100_MS_MAX_CLICK);
            assertThat(page.getByText("I was clicked!")).isVisible(WAIT_1_SEC_MAX);
        });
    }


    @Deprecated
    public static class TestApp {
        public static void main(String[] args) {
            if (Jt.button("Click me").use()) {
                Jt.text("I was clicked!").use();
            }
        }
    }

    @Test
    public void testEmbeddedServerE2E(final TestInfo testInfo) {
        final JtRunnable app = () -> {
            if (Jt.button("Click me").use()) {
                Jt.text("I was clicked!").use();
            }
        };
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            assertThat(page.locator("jt-button")).isVisible(WAIT_1_SEC_MAX);
            page.locator("jt-button button").click(WAIT_100_MS_MAX_CLICK);
            assertThat(page.getByText("I was clicked!")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    public static void test() {
        Jt.text("I was clicked!").use();
    }

    @Test
    public void testEmbeddedServerWithParametrizedStaticRunnableE2E(final TestInfo testInfo) {
        PlaywrightUtils.runInBrowser(testInfo, () -> app1("Hello"), page -> {
            assertThat(page.getByText("Hello")).isVisible(WAIT_1_SEC_MAX);
        });
    }


    public static void app1(String text) {
        Jt.text(text).use();
    }

    @Test
    public void testEmbeddedServerWithParametrizedNonStaticRunnableE2E(final TestInfo testInfo) {
        PlaywrightUtils.runInBrowser(testInfo, this::app2, page -> {
            assertThat(page.getByText("Hello")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    public void app2() {
        Jt.text(instanceString).use();
    }

}
