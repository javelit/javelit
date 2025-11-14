 import io.javelit.core.Jt;

 public class DividerApp {
     public static void main(String[] args) {
         Jt.title("Section 1").use();
         Jt.text("Content for section 1").use();

         Jt.divider("div1").use();

         Jt.title("Section 2").use();
         Jt.text("Content for section 2").use();
     }
 }
