import io.javelit.core.Jt;

public class UrlAudioApp {
  public static void main(String[] args) {
    Jt.audio("https://github.com/javelit/public_assets/raw/refs/heads/main/audio/piano-chords.mp3").use();
  }
}
