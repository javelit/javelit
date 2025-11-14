 import io.javelit.core.Jt;

 public class TextInputApp {
     public static void main(String[] args) {
         String name = Jt.textInput("Your name").use();

         if (!name.isEmpty()) {
             Jt.text("Hello, " + name + "!").use();
         }
     }
 }
