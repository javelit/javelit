 import io.javelit.core.Jt;

 public class MultilineCodeApp {
     public static void main(String[] args) {
         String pythonCode = """
                 import numpy as np

                 a = np.arange(15).reshape(3, 5)
                 """;
         Jt.code(pythonCode).language("python").use();
     }
 }
