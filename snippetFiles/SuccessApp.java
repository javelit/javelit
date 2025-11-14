 import io.javelit.core.Jt;

 public class SuccessApp {
     public static void main(String[] args) {
         String username = Jt.textInput("Username").use();

         if (username.isEmpty()) {
             Jt.success("Username is required!").use();
         } else if (username.length() > 3) {
             Jt.success("Username is long enough.").use();
         }
     }
 }
