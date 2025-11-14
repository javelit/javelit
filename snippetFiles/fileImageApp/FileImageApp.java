package fileImageApp;

import java.nio.file.Path;

import io.javelit.core.Jt;

 public class FileImageApp {
     public static void main(String[] args) {
          // mountains.jpg is present in the working directory
          Jt.image(Path.of("mountains.jpg")).use();
     }
 }
