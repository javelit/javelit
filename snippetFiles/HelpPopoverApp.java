 import io.javelit.core.Jt;

 public class HelpPopoverApp {
     public static void main(String[] args) {
         Jt.text("Username:").use();
         Jt.textInput("Enter username").use();

         var help = Jt.popover("❓ Help").use();
         Jt.text("**Username requirements:**").use(help);
         Jt.text("- Must be 3-20 characters long").use(help);
         Jt.text("- Only letters and numbers allowed").use(help);
         Jt.text("- Case sensitive").use(help);
     }
 }
