 import io.javelit.core.Jt;

 public class InfoApp {
     public static void main(String[] args) {
         String username = Jt.textInput("Username").use();

         if (username.isEmpty()) {
             Jt.info("Username is required!").use();
         } else if (username.length() < 3) {
             Jt.info("Username must be at least 3 characters long.").use();
         }
     }
 }
