 import io.javelit.core.Jt;

 public class WarningApp {
     public static void main(String[] args) {
         String username = Jt.textInput("Username").use();

         if (username.isEmpty()) {
             Jt.warning("Username is required!").use();
         } else if (username.length() < 3) {
             Jt.warning("Username must be at least 3 characters long.").use();
         }
     }
 }
