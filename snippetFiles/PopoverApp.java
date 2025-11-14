 

 import java.util.List;import io.javelit.core.Jt;

 public class PopoverApp {
     public static void main(String[] args) {
         var settings = Jt.popover("⚙️ Settings").use();

         Jt.text("Configure your preferences:").use(settings);
         boolean notifications = Jt.checkbox("Enable notifications").use(settings);
         String theme = Jt.selectbox("Theme", List.of("Light", "Dark")).use(settings);

         if (notifications) {
             Jt.text("Notifications are enabled").use();
         }
         Jt.text("The selected theme is " + theme).use();
     }
 }
