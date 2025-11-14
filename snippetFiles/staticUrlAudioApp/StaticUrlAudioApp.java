package staticUrlAudioApp;

import io.javelit.core.Jt;

public class StaticUrlAudioApp {
  public static void main(String[] args) {
    // static/piano-chords.mp3 is present in the working directory
    Jt.audio("app/static/piano-chords.mp3").use();
  }
}
