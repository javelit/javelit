 import io.javelit.core.Jt;

 public class ToggleDefaultApp {
     public static void main(String[] args) {
         boolean autoSave = Jt.toggle("Auto-save")
             .value(true)
             .use();

         if (autoSave) {
             Jt.text("Changes will be saved automatically").use();
         }
     }
 }
