import java.nio.file.Path;

import io.javelit.core.Jt;

 public class HtmlFileApp {
     public static void main(String[] args) {
         // Assumes "content.html" is present in the working directory
         Jt.html(Path.of("content.html")).use();
     }
 }
