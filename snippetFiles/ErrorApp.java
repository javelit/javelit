 import io.javelit.core.Jt;

 public class ErrorApp {
     public static void main(String[] args) {
         String username = Jt.textInput("Username").use();

         if (username.isEmpty()) {
             Jt.error("Username is required!").use();
         } else if (username.length() < 3) {
             Jt.error("Username must be at least 3 characters long.").use();
         }
     }
 }
