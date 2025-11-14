 import io.javelit.core.Jt;

 public class ToggleApp {
     public static void main(String[] args) {
         boolean enabled = Jt.toggle("Enable notifications").use();

         Jt.text("Notifications: " + (enabled ? "Enabled" : "Disabled")).use();
     }
 }
