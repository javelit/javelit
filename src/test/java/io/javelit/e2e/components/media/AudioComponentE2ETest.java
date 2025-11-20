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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;
import io.javelit.e2e.helpers.PlaywrightUtils;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.javelit.e2e.helpers.PlaywrightUtils.TEST_PROXY_PREFIX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AudioComponentE2ETest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testAudioVariations(final boolean proxied, final TestInfo testInfo) {
    JtRunnable app = () -> {
      // Test 1: Public URL with autoplay
      Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
        .autoplay(true)
        .use();

      // Test 2: Public URL without autoplay
      Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
        .use();

      // Test 3: Local file
      Jt.audio(Path.of("examples/audio/piano-chords.mp3"))
        .use();

      // Test 4: With loop
      Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
        .loop(true)
        .use();

      // Test 5: With start and end time
      Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
        .startTime(3)
        .endTime(6)
        .use();

      // Test 6: With start, end, and loop
      Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
        .startTime(3)
        .endTime(6)
        .loop(true)
        .use();
    };

    PlaywrightUtils.runInBrowser(testInfo, app, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

      // Verify all 6 audio components are rendered
      assertThat(page.locator("#app jt-audio").nth(0)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio").nth(1)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio").nth(2)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio").nth(3)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio").nth(4)).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio").nth(5)).isVisible(WAIT_1_SEC_MAX);

      // Test 1: Public URL with autoplay
      assertThat(page.locator("#app jt-audio").nth(0).locator("audio[controls]")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio").nth(0).locator("audio[autoplay]")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio").nth(0).locator("audio source")).hasAttribute("src",
                                                                                            "https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3");

      // Test 2: Public URL without autoplay
      assertThat(page.locator("#app jt-audio").nth(1).locator("audio[controls]")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio").nth(1).locator("audio")).not().hasAttribute("autoplay", "");

      // Test 3: Local file (media hash)
      String src3 = page.locator("#app jt-audio").nth(2).locator("audio source").getAttribute("src");
      assertTrue(src3.startsWith(pathPrefix + "/_/media/"),
                 "Audio src should start with " + pathPrefix + "/_/media/, got: " + src3);

      // Test 4: With loop
      assertThat(page.locator("#app jt-audio").nth(3).locator("audio[loop]")).isVisible(WAIT_1_SEC_MAX);

      // Test 5: With start and end time
      assertThat(page.locator("#app jt-audio").nth(4)).hasAttribute("start-time-millis", "3000");
      assertThat(page.locator("#app jt-audio").nth(4)).hasAttribute("end-time-millis", "6000");
      assertThat(page.locator("#app jt-audio").nth(4).locator("audio")).not().hasAttribute("loop", "");

      // Test 6: With start, end, and loop
      assertThat(page.locator("#app jt-audio").nth(5)).hasAttribute("start-time-millis", "3000");
      assertThat(page.locator("#app jt-audio").nth(5)).hasAttribute("end-time-millis", "6000");
      assertThat(page.locator("#app jt-audio").nth(5)).hasAttribute("loop", "");
      assertThat(page.locator("#app jt-audio").nth(5).locator("audio")).not().hasAttribute("loop", "");
    });
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testStaticFolder(final boolean proxied, final TestInfo testInfo) throws IOException {
    final Path tempDir = Files.createTempDirectory("javelit-audio-test-");
    copyResourceDirectory("audio-test", tempDir);
    final Path appFile = tempDir.resolve("AudioStaticTestApp.java");

    PlaywrightUtils.runInBrowser(testInfo, appFile, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX + "/" : "";

      assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio audio[controls]")).isVisible(WAIT_1_SEC_MAX);

      var audioSource = page.locator("#app jt-audio audio source");
      assertThat(audioSource).hasAttribute("src", pathPrefix + "app/static/piano-chords.mp3", WAIT_1_SEC_MAX_ATTRIBUTE);
      assertThat(audioSource).hasAttribute("type", "audio/mpeg", WAIT_1_SEC_MAX_ATTRIBUTE);

      page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
      double duration = (double) page.evaluate(
          "document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
      assertEquals(9.0, duration, 0.5, "Audio duration should be approximately 9 seconds");
    });
  }

  private static byte[] generateBeepWavBytes(int seconds) throws Exception {
    final float sampleRate = 44100;
    final int numSamples = (int) (seconds * sampleRate);
    final int numChannels = 1;

    byte[] data = new byte[numSamples];
    int period = (int) (sampleRate / 440);
    for (int i = 0; i < numSamples; i++) {
      data[i] = i % period < period / 2 ? (byte) 127 : (byte) -128;
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    AudioFormat format = new AudioFormat(sampleRate, 8, numChannels, true, false);

    try (AudioInputStream ais = new AudioInputStream(
        new ByteArrayInputStream(data), format, numSamples)) {
      AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
    }

    return baos.toByteArray();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testGeneratedBytes(final boolean proxied, final TestInfo testInfo) {
    JtRunnable app = () -> {
      byte[] beepWav = generateBeepWavBytes(2);
      Jt.audio(beepWav)
        .format("audio/wav")
        .use();
    };

    PlaywrightUtils.runInBrowser(testInfo, app, true, proxied, page -> {
      final String pathPrefix = proxied ? TEST_PROXY_PREFIX : "";

      assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);
      assertThat(page.locator("#app jt-audio audio[controls]")).isVisible(WAIT_1_SEC_MAX);

      var audioSource = page.locator("#app jt-audio audio source");
      String src = audioSource.getAttribute("src", new Locator.GetAttributeOptions().setTimeout(1000));
      assertTrue(src.startsWith(pathPrefix + "/_/media/"),
                 "Audio src should start with " + pathPrefix + "/_/media/, got: " + src);
      assertThat(audioSource).hasAttribute("type", "audio/wav", WAIT_1_SEC_MAX_ATTRIBUTE);

      page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1",
                           null, new Page.WaitForFunctionOptions().setTimeout(1000));
      int duration = (int) page.evaluate(
          "document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
      assertEquals(2, duration, "Audio duration should be approximately 2 seconds");
    });
  }
}
