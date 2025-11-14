 

 import java.util.Map;import io.javelit.core.Jt;

 public class TableColumnsArrayApp {
     public static void main(String[] args) {
         Map<String, Object[]> salesData = Map.of(
                 "Month", new String[]{"Jan", "Feb", "Mar", "Apr"},
                 "Sales", new Integer[]{1200, 1350, 1100, 1450},
                 "Target", new Integer[]{1000, 1300, 1200, 1400},
                 "Achieved", new Boolean[]{true, true, false, true}
         );

         Jt.tableFromArrayColumns(salesData).use();
     }
 }
