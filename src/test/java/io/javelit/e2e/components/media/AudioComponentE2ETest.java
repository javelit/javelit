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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.OsUtils.copyResourceDirectory;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AudioComponentE2ETest {

    @Test
    void testPublicUrlWithAutoplay(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
                        .autoplay(true)
                        .use();
                }
            }
            """;

        // Verify audio component is rendered
        // Verify audio element exists with controls
        // Verify autoplay attribute
        // Verify src attribute
        // Verify duration is 9 seconds (0:09)
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify audio element exists with controls
            assertThat(page.locator("#app jt-audio audio[controls]")).isVisible(WAIT_1_SEC_MAX);

            // Verify autoplay attribute
            assertThat(page.locator("#app jt-audio audio[autoplay]")).isVisible(WAIT_1_SEC_MAX);

            // Verify src attribute
            var audioSource = page.locator("#app jt-audio audio source");
            assertThat(audioSource).hasAttribute("src", "https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3");
            assertThat(audioSource).hasAttribute("type", "audio/mpeg");

            // Verify duration is 9 seconds (0:09)
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
            double duration = (double) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
            assertEquals(9.0, duration, 0.5, "Audio duration should be approximately 9 seconds");
        });
    }

    @Test
    void testPublicUrl(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
                        .use();
                }
            }
            """;

        // Verify audio component is rendered
        // Verify audio element exists with controls
        // Verify no autoplay
        // Verify src attribute
        // Verify duration is 9 seconds (0:09)
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify audio element exists with controls
            assertThat(page.locator("#app jt-audio audio[controls]")).isVisible(WAIT_1_SEC_MAX);

            // Verify no autoplay
            assertThat(page.locator("#app jt-audio audio")).not().hasAttribute("autoplay", "");

            // Verify src attribute
            var audioSource = page.locator("#app jt-audio audio source");
            assertThat(audioSource).hasAttribute("src", "https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3");
            assertThat(audioSource).hasAttribute("type", "audio/mpeg");

            // Verify duration is 9 seconds (0:09)
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
            double duration = (double) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
            assertEquals(9.0, duration, 0.5, "Audio duration should be approximately 9 seconds");
        });
    }

    @Test
    void testStaticFolder(TestInfo testInfo) throws IOException {
        final Path tempDir = Files.createTempDirectory("javelit-audio-test-");
        copyResourceDirectory("audio-test", tempDir);
        final Path appFile = tempDir.resolve("AudioStaticTestApp.java");

        // Verify audio component is rendered
        // Verify audio element exists with controls
        // Verify src points to static folder
        // Verify duration is 9 seconds (0:09)
        PlaywrightUtils.runInBrowser(testInfo, appFile, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify audio element exists with controls
            assertThat(page.locator("#app jt-audio audio[controls]")).isVisible(WAIT_1_SEC_MAX);

            // Verify src points to static folder
            var audioSource = page.locator("#app jt-audio audio source");
            assertThat(audioSource).hasAttribute("src", "app/static/piano-chords.mp3", WAIT_1_SEC_MAX_ATTRIBUTE);
            assertThat(audioSource).hasAttribute("type", "audio/mpeg", WAIT_1_SEC_MAX_ATTRIBUTE);

            // Verify duration is 9 seconds (0:09)
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
            double duration = (double) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
            assertEquals(9.0, duration, 0.5, "Audio duration should be approximately 9 seconds");
        });
    }

    @Test
    void testLocalFile(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.nio.file.Path;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio(Path.of("examples/audio/piano-chords.mp3"))
                        .use();
                }
            }
            """;

        // Verify audio component is rendered
        // Verify audio element exists with controls
        // Verify src contains media hash (starts with /_/media/)
        // Verify duration is 9 seconds (0:09)
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify audio element exists with controls
            assertThat(page.locator("#app jt-audio audio[controls]")).isVisible(WAIT_1_SEC_MAX);

            // Verify src contains media hash (starts with /_/media/)
            var audioSource = page.locator("#app jt-audio audio source");
            String src = audioSource.getAttribute("src");
            assertTrue(src.startsWith("/_/media/"), "Audio src should start with /_/media/, got: " + src);
            assertThat(audioSource).hasAttribute("type", "audio/mpeg");

            // Verify duration is 9 seconds (0:09)
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
            double duration = (double) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
            assertEquals(9.0, duration, 0.5, "Audio duration should be approximately 9 seconds");
        });
    }

    @Test
    void testGeneratedBytes(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import javax.sound.sampled.AudioFileFormat;
            import javax.sound.sampled.AudioFormat;
            import javax.sound.sampled.AudioInputStream;
            import javax.sound.sampled.AudioSystem;
            import java.io.ByteArrayInputStream;
            import java.io.ByteArrayOutputStream;

            public class TestApp {
                public static void main(String[] args) throws Exception {
                    byte[] beepWav = generateBeepWavBytes(2);
                    Jt.audio(beepWav)
                        .format("audio/wav")
                        .use();
                }

                private static byte[] generateBeepWavBytes(int seconds) throws Exception {
                    final float sampleRate = 44100;
                    final int numSamples = (int) (seconds * sampleRate);
                    final int numChannels = 1;

                    byte[] data = new byte[numSamples];
                    int period = (int) (sampleRate / 440);
                    for (int i = 0; i < numSamples; i++) {
                        data[i] = (i % period < period / 2) ? (byte) 127 : (byte) -128;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    AudioFormat format = new AudioFormat(sampleRate, 8, numChannels, true, false);

                    try (AudioInputStream ais = new AudioInputStream(
                            new ByteArrayInputStream(data), format, numSamples)) {
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
                    }

                    return baos.toByteArray();
                }
            }
            """;

        // Verify audio component is rendered
        // Verify audio element exists with controls
        // Verify src contains media hash (starts with /_/media/)
        // Verify duration is approximately 2 seconds (generated beep)
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify audio element exists with controls
            assertThat(page.locator("#app jt-audio audio[controls]")).isVisible(WAIT_1_SEC_MAX);

            // Verify src contains media hash (starts with /_/media/)
            var audioSource = page.locator("#app jt-audio audio source");
            String src = audioSource.getAttribute("src");
            assertTrue(src.startsWith("/_/media/"), "Audio src should start with /_/media/, got: " + src);
            assertThat(audioSource).hasAttribute("type", "audio/wav");

            // Verify duration is approximately 2 seconds (generated beep)
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
            int duration = (int) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
            assertEquals(2, duration, "Audio duration should be approximately 2 seconds");
        });
    }

    @Test
    void testLoop(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
                        .loop(true)
                        .use();
                }
            }
            """;

        // Verify audio component is rendered
        // Verify loop attribute (native loop when no time constraints)
        // Verify duration is 9 seconds (0:09)
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify loop attribute (native loop when no time constraints)
            assertThat(page.locator("#app jt-audio audio[loop]")).isVisible(WAIT_1_SEC_MAX);

            // Verify duration is 9 seconds (0:09)
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
            double duration = (double) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
            assertEquals(9.0, duration, 0.5, "Audio duration should be approximately 9 seconds");
        });
    }

    @Test
    void testStartAndEndTime(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
                        .startTime(3)
                        .endTime(6)
                        .use();
                }
            }
            """;

        // Verify audio component is rendered
        // Verify time attributes are set
        // Verify no native loop (JavaScript handles time control)
        // Verify duration is 9 seconds (0:09) - full audio duration
        // Verify initial currentTime is set to startTime
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify time attributes are set
            var audioComponent = page.locator("#app jt-audio");
            assertThat(audioComponent).hasAttribute("start-time-millis", "3000");
            assertThat(audioComponent).hasAttribute("end-time-millis", "6000");

            // Verify no native loop (JavaScript handles time control)
            var audioElement = page.locator("#app jt-audio audio");
            assertThat(audioElement).not().hasAttribute("loop", "");

            // Verify duration is 9 seconds (0:09) - full audio duration
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
            double duration = (double) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
            assertEquals(9.0, duration, 0.5, "Audio duration should be approximately 9 seconds");

            // Verify initial currentTime is set to startTime
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').currentTime >= 3");
            int currentTime = (int) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').currentTime");
            assertEquals(3.0, currentTime, 0.5, "Initial currentTime should be at startTime (3 seconds)");
        });
    }

    @Test
    void testStartEndAndLoop(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3")
                        .startTime(3)
                        .endTime(6)
                        .loop(true)
                        .use();
                }
            }
            """;

        // Verify audio component is rendered
        // Verify time attributes are set
        // Verify component has loop property (but not native loop attribute when time constraints exist)
        // Verify no native loop attribute on audio element (JavaScript handles loop with time constraints)
        // Verify duration is 9 seconds (0:09) - full audio duration
        // Verify initial currentTime is set to startTime
        PlaywrightUtils.runInBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify time attributes are set
            var audioComponent = page.locator("#app jt-audio");
            assertThat(audioComponent).hasAttribute("start-time-millis", "3000");
            assertThat(audioComponent).hasAttribute("end-time-millis", "6000");

            // Verify component has loop property (but not native loop attribute when time constraints exist)
            assertThat(audioComponent).hasAttribute("loop", "");

            // Verify no native loop attribute on audio element (JavaScript handles loop with time constraints)
            var audioElement = page.locator("#app jt-audio audio");
            assertThat(audioElement).not().hasAttribute("loop", "");

            // Verify duration is 9 seconds (0:09) - full audio duration
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').readyState >= 1");
            double duration = (double) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').duration");
            assertEquals(9.0, duration, 0.5, "Audio duration should be approximately 9 seconds");

            // Verify initial currentTime is set to startTime
            page.waitForFunction("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').currentTime >= 3");
            int currentTime = (int) page.evaluate("document.querySelector('#app jt-audio').shadowRoot.querySelector('audio').currentTime");
            assertEquals(3.0, currentTime, 0.5, "Initial currentTime should be at startTime (3 seconds)");
        });
    }
}
