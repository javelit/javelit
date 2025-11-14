 

 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;import io.javelit.core.Jt;

 public class BytePdfApp {
     public static void main(String[] args) throws IOException {
         byte[] pdfBytes = Files.readAllBytes(Path.of("document.pdf"));
         Jt.pdf(pdfBytes).use();
     }
 }
