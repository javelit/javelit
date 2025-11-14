 import io.javelit.core.Jt;

 public class FormApp {
     public static void main(String[] args) {
         var form = Jt.form().use();

         String name = Jt.textInput("Full Name").use(form);
         String email = Jt.textInput("Email").use(form);
         int age = Jt.numberInput("Age", Integer.class).min(0).max(120).use(form);
         boolean subscribe = Jt.checkbox("Subscribe to newsletter").use(form);

         if (Jt.formSubmitButton("Register").use()) {
             Jt.text("Welcome, " + name + "!").use();
             Jt.text("Email: " + email).use();
         }
     }
 }
