 import io.javelit.core.Jt;

 public class CheckboxApp {
     public static void main(String[] args) {
         boolean agree = Jt.checkbox("I agree").use();

         if (agree) {
             Jt.text("Great!").use();
         }
     }
 }
