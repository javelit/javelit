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

import io.javelit.e2e.helpers.PlaywrightUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.javelit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for AudioComponent.
 */
public class AudioComponentE2ETest {

    @Test
    void testBasicAudioFromUrl(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://example.com/audio.mp3").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify audio element exists
            assertThat(page.locator("#app jt-audio audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify controls are present
            assertThat(page.locator("#app jt-audio audio[controls]")).isVisible(WAIT_1_SEC_MAX);

            // Verify src attribute
            var audioElement = page.locator("#app jt-audio audio source");
            assertThat(audioElement).hasAttribute("src", "https://example.com/audio.mp3");
            assertThat(audioElement).hasAttribute("type", "audio/wav");
        });
    }

    @Test
    void testAudioFromBytes(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.nio.file.Files;
            import java.nio.file.Path;

            public class TestApp {
                public static void main(String[] args) throws Exception {
                    // Create a minimal WAV file (44 bytes header + 1 byte data)
                    byte[] minimalWav = new byte[]{
                        0x52, 0x49, 0x46, 0x46, // "RIFF"
                        0x29, 0x00, 0x00, 0x00, // File size - 8
                        0x57, 0x41, 0x56, 0x45, // "WAVE"
                        0x66, 0x6D, 0x74, 0x20, // "fmt "
                        0x10, 0x00, 0x00, 0x00, // Subchunk1Size (16)
                        0x01, 0x00, // AudioFormat (1 = PCM)
                        0x01, 0x00, // NumChannels (1 = mono)
                        0x44, (byte)0xAC, 0x00, 0x00, // SampleRate (44100)
                        (byte)0x88, 0x58, 0x01, 0x00, // ByteRate
                        0x02, 0x00, // BlockAlign
                        0x10, 0x00, // BitsPerSample (16)
                        0x64, 0x61, 0x74, 0x61, // "data"
                        0x01, 0x00, 0x00, 0x00, // Subchunk2Size (1)
                        0x00 // 1 byte of audio data
                    };

                    Jt.audio(minimalWav).format("audio/wav").use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify audio element exists
            assertThat(page.locator("#app jt-audio audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify src contains media hash (starts with /_/media/)
            var audioElement = page.locator("#app jt-audio audio source");
            String src = audioElement.getAttribute("src");
            assertTrue(src.startsWith("/_/media/"), "Audio src should start with /_/media/, got: " + src);

            // Verify format
            assertThat(audioElement).hasAttribute("type", "audio/wav");
        });
    }

    @Test
    void testMediaFragmentUri(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;
            import java.time.Duration;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://example.com/audio.mp3")
                        .startTime(Duration.ofMillis(3500))
                        .endTime(Duration.ofMillis(7250))
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify audio component is rendered
            assertThat(page.locator("#app jt-audio")).isVisible(WAIT_1_SEC_MAX);

            // Verify Media Fragment URI is appended
            var audioElement = page.locator("#app jt-audio audio source");
            String src = audioElement.getAttribute("src");
            assertTrue(src.contains("#t=3.500,7.250"),
                "Audio src should contain #t=3.500,7.250, got: " + src);
        });
    }

    @Test
    void testMediaFragmentStartOnly(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://example.com/audio.mp3")
                        .startTime(3)
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify Media Fragment URI with start only
            var audioElement = page.locator("#app jt-audio audio source");
            String src = audioElement.getAttribute("src");
            assertTrue(src.contains("#t=3.000"),
                "Audio src should contain #t=3.000, got: " + src);
        });
    }

    @Test
    void testMediaFragmentEndOnly(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://example.com/audio.mp3")
                        .endTime(7)
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify Media Fragment URI with end only (should include 0 as start)
            var audioElement = page.locator("#app jt-audio audio source");
            String src = audioElement.getAttribute("src");
            assertTrue(src.contains("#t=0.000,7.000"),
                "Audio src should contain #t=0.000,7.000, got: " + src);
        });
    }

    @Test
    void testLoopAndAutoplay(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://example.com/audio.mp3")
                        .loop(true)
                        .autoplay(true)
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify loop and autoplay attributes
            assertThat(page.locator("#app jt-audio audio[loop]")).isVisible(WAIT_1_SEC_MAX);
            assertThat(page.locator("#app jt-audio audio[autoplay]")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testWidthPixels(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://example.com/audio.mp3")
                        .width(400)
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify width attribute is set
            assertThat(page.locator("#app jt-audio[width='400']")).isVisible(WAIT_1_SEC_MAX);
        });
    }

    @Test
    void testWidthStretch(TestInfo testInfo) {
        final @Language("java") String app = """
            import io.javelit.core.Jt;

            public class TestApp {
                public static void main(String[] args) {
                    Jt.audio("https://example.com/audio.mp3")
                        .width("stretch")
                        .use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(testInfo, app, page -> {
            // Verify width="stretch" attribute is set
            assertThat(page.locator("#app jt-audio[width='stretch']")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
