 

 import java.util.List;import io.javelit.core.Jt;

 public class EmptyApp {
     public static void main(String[] args) {
         var placeholder = Jt.empty().use();
         String selected = Jt.selectbox("Choose content",
             List.of("None", "Text", "Button")).use();

         switch (selected) {
             case "Text" -> Jt.text("Dynamic text content").use(placeholder);
             case "Button" -> {
                 if (Jt.button("Dynamic button").use(placeholder)) {
                     Jt.text("Button clicked!").use();
                 }
             }
             // case "None" -> container remains empty
         }
     }
 }
