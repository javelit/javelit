import java.nio.file.Path;import io.javelit.core.Jt;

 public class FilePdfApp {
     public static void main(String[] args) {
          // assumes document.pdf is present in the working directory
          Jt.pdf(Path.of("document.pdf")).use();
     }
 }
