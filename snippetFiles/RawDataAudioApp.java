import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import io.javelit.core.Jt;

public class RawDataAudioApp {
  public static void main(String[] args) {
    byte[] beepWav = generateBeepWavBytes(2);
    Jt.audio(beepWav).format("audio/wav").use();
  }

  private static byte[] generateBeepWavBytes(int seconds) {
    final float sampleRate = 44100;
    final int numSamples = (int) (seconds * sampleRate);
    final int numChannels = 1;  // mono
    byte[] data = new byte[numSamples];
    // Generate simple square wave beep at 440 Hz
    int period = (int) (sampleRate / 440); // ~440 Hz
    for (int i = 0; i < numSamples; i++) {
      data[i] = i % period < period / 2 ? (byte) 127 : (byte) -128;
    }
    // Wrap raw PCM data into WAV format
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    AudioFormat format = new AudioFormat(sampleRate, 8, // bits per sample
                                         numChannels, true, // signed
                                         false // little endian
    );

    try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), format, numSamples)) {
      AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
