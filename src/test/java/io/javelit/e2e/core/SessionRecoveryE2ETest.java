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

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_10_MS_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_5_SEC_MAX;

public class SessionRecoveryE2ETest {

  // ensure connection recovery works at a very basic level
  // this test takes around 10 seconds
  @Test
  void testReconnectionQueuedMessages(TestInfo testInfo) {
    final @Language("java") String app = """
        import io.javelit.core.Jt;
        
        public class TestApp {
            public static void main(String[] args) {
              Jt.sessionState().put("VALUE", "HAHA");
              for (int i=0; i<10;i++) {
                  Jt.text("text " + i).use();
                  try {
                    Thread.sleep(200);
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                }
              Jt.text(Jt.sessionState().getString("VALUE")).use();
            }
        }
        """;

    PlaywrightUtils.runInBrowser(testInfo, app, page -> {
      // 1. Wait for initial connection
      assertThat(page.getByText("text 0")).isVisible(WAIT_5_SEC_MAX);
      // 2. Close WebSocket from JavaScript
      page.evaluate("() => { if (window.javelit.debug_ws) window.javelit.debug_ws.close(); }");
      // 3. disconnected modal appears
      assertThat(page.getByText("Unable to connect to the Javelit server.")).isVisible(WAIT_5_SEC_MAX);
      // the app is still running, messages are queued
      for (int i = 0; i < 10; i++) {
        assertThat(page.getByText("text " + i)).isVisible(WAIT_5_SEC_MAX);
      }
      assertThat(page.getByText("HAHA")).isVisible(WAIT_5_SEC_MAX);
      assertThat(page.getByText("Unable to connect to the Javelit server.")).not().isVisible(WAIT_10_MS_MAX);
    });
  }
}
