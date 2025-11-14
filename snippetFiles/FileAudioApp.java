import java.nio.file.Path;

import io.javelit.core.Jt;

 public class FileAudioApp {
     public static void main(String[] args) {
          // assumes piano-chords.mp3 is present in the working directory
          Jt.audio(Path.of("piano-chords.mp3")).use();
     }
 }
