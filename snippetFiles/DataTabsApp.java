 import io.javelit.core.Jt;

 public class DataTabsApp {
     public static void main(String[] args) {
         var tabs = Jt.tabs(List.of("Sales", "Marketing", "Finance")).use();

         // Sales tab
         Jt.title("Sales Dashboard").use(tabs.tab(0));
         Jt.text("Total sales: $100,000").use(tabs.tab(0));

         // Marketing tab
         Jt.title("Marketing Metrics").use(tabs.tab(1));
         Jt.text("Conversion rate: 3.5%").use(tabs.tab(1));

         // Finance tab
         Jt.title("Financial Overview").use(tabs.tab(2));
         Jt.text("Revenue growth: +15%").use(tabs.tab(2));
     }
 }
