 import io.javelit.core.Jt;

 public class AudioInputApp {
     public static void main(String[] args) {
         var recording = Jt.audioInput("Record a voice message").use();
         if (recording != null) {
             Jt.audio(recording).use();
         }
     }
 }
