 import io.javelit.core.Jt;

 public class FormSubmitApp {
     public static void main(String[] args) {
         var form = Jt.form("contact").use();

         String name = Jt.textInput("Your Name").use(form);
         String message = Jt.textArea("Message").use(form);

         if (Jt.formSubmitButton("Send Message").use(form)) {
             Jt.text("Message sent successfully!").use();
             Jt.text("From: " + name).use();
         }
     }
 }
