import io.javelit.core.Jt;

public class HighQualityAudioInputApp {
  public static void main(String[] args) {
    var recording = Jt.audioInput("Record high quality audio").sampleRate(48000).use();
    if (recording != null) {
      Jt.audio(recording).use();
    }
  }
}
