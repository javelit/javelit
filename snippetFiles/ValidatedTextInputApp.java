 import io.javelit.core.Jt;

 public class ValidatedTextInputApp {
     public static void main(String[] args) {
         String email = Jt.textInput("Email address")
                          .placeholder("Enter your email")
                          .use();

         if (!email.isEmpty() && !email.contains("@")) {
             Jt.error("Please enter a valid email address").use();
         } else if (!email.isEmpty()) {
             Jt.text("Valid email: " + email).use();
         }
     }
 }
