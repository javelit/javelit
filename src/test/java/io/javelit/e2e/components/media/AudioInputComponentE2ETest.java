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
package io.javelit.e2e.components.media;

import java.util.List;

import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_10_MS_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_CLICK;

public class AudioInputComponentE2ETest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testAudioInputDisplaysProperly(final boolean proxied, final TestInfo testInfo) {
        Assumptions.assumeFalse(
                System.getenv("CI") != null,
                "AudioInput tests require headed browser with microphone - skipping in CI"
        );

        JtRunnable app = () -> Jt.audioInput("Say something").use();

        PlaywrightUtils.runInBrowser(testInfo, app, false, proxied, page -> {
            // Grant microphone permission
            page.context().grantPermissions(List.of("microphone"));
            // Verify component is rendered
            assertThat(page.locator("jt-audio-input")).isVisible(WAIT_1_SEC_MAX);
            // Verify label is displayed
            assertThat(page.locator("jt-audio-input .label")).containsText("Say something");
            // Verify record button is present
            assertThat(page.locator("jt-audio-input .record-button")).isVisible(WAIT_1_SEC_MAX);
            // Verify visualizer container is present
            assertThat(page.locator("jt-audio-input .visualizer-container")).isVisible(WAIT_1_SEC_MAX);
            // Verify time display shows 00:00 initially
            assertThat(page.locator("jt-audio-input .time-display")).containsText("00:00");


            // test recording
            // Click record button
            page.locator("jt-audio-input .record-button").click(WAIT_1_SEC_MAX_CLICK);
            // Verify stop button appears (recording state)
            assertThat(page.locator("jt-audio-input .stop-button")).isVisible(WAIT_1_SEC_MAX);
            // Verify visualizer bars have recording class
            assertThat(page.locator("jt-audio-input .visualizer-bar.recording").first()).isVisible(WAIT_1_SEC_MAX);
            // Wait a bit for time to update
            page.waitForTimeout(1500);
            // Verify time display shows non-zero time
            String timeText = page.locator("jt-audio-input .time-display").textContent();
            assert !"00:00".equals(timeText) : "Time should have updated during recording";
            // Stop recording
            page.locator("jt-audio-input .stop-button").click(WAIT_1_SEC_MAX_CLICK);
            // Wait for upload to complete
            page.waitForTimeout(1000);
            // Verify record button reappears
            assertThat(page.locator("jt-audio-input .record-button")).isVisible(WAIT_1_SEC_MAX);
            // Verify play button appears (has value)
            assertThat(page.locator("jt-audio-input .play-button")).isVisible(WAIT_1_SEC_MAX);

            // play the recording
            // Verify progress bar is visible (has value)
            assertThat(page.locator("jt-audio-input .progress-bar-container")).isVisible(WAIT_1_SEC_MAX);
            // Get initial time display
            String initialTime = page.locator("jt-audio-input .time-display").textContent();
            // Click play button
            page.locator("jt-audio-input .play-button").click();
            // Verify pause button appears
            assertThat(page.locator("jt-audio-input .play-button .icon")).containsText("pause");
            // Wait for playback to progress
            page.waitForTimeout(500);
            // Verify time is updating (decreased from duration)
            String playingTime = page.locator("jt-audio-input .time-display").textContent();
            assert !playingTime.equals(initialTime) : "Time should update during playback";
            // Click pause
            page.locator("jt-audio-input .play-button").click();
            // Verify play icon returns
            assertThat(page.locator("jt-audio-input .play-button .icon")).containsText("play_circle");
            // Verify progress bar remains visible when paused
            assertThat(page.locator("jt-audio-input .progress-bar-container")).isVisible(WAIT_1_SEC_MAX);


            // Record again
            page.locator("jt-audio-input .record-button").click(WAIT_1_SEC_MAX_CLICK);
            // Verify stop button appears
            assertThat(page.locator("jt-audio-input .stop-button")).isVisible(WAIT_1_SEC_MAX);
            // Verify play button is gone during recording
            assertThat(page.locator("jt-audio-input .play-button")).not().isVisible(WAIT_10_MS_MAX);
            // Verify visualizer shows recording state
            assertThat(page.locator("jt-audio-input .visualizer-bar.recording").first()).isVisible(WAIT_1_SEC_MAX);
            // Stop second recording
            page.locator("jt-audio-input .stop-button").click(WAIT_1_SEC_MAX_CLICK);
            page.waitForTimeout(1000);
            // Verify can play the new recording
            assertThat(page.locator("jt-audio-input .play-button")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("jt-audio-input .progress-bar-container")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
