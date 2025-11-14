import java.nio.file.Path;

import io.javelit.core.Jt;

 public class HtmlFileApp {
     public static void main(String[] args) {
         // Assumes you have a file "content.html" in your project
         Jt.html(Path.of("content.html")).use();
     }
 }
