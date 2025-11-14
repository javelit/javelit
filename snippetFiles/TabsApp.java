 

 import java.util.List;import io.javelit.core.Jt;

 public class TabsApp {
     public static void main(String[] args) {
         var tabs = Jt.tabs(List.of("Overview", "Details", "Settings")).use();

         Jt.text("Welcome to the overview page").use(tabs.tab("Overview"));
         Jt.text("Here are the details").use(tabs.tab("Details"));
         Jt.text("Configure your settings here").use(tabs.tab("Settings"));
     }
 }
