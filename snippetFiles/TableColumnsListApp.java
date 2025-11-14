 

 import java.util.List;
 import java.util.Map;import io.javelit.core.Jt;

 public class TableColumnsListApp {
     public static void main(String[] args) {
         Map<String, List<Object>> employeeData = Map.of(
                 "Name", List.of("Alice", "Bob", "Charlie", "Diana"),
                 "Department", List.of("Engineering", "Sales", "Marketing", "Engineering"),
                 "Salary", List.of(95000, 75000, 68000, 102000),
                 "Remote", List.of(true, false, true, true)
         );

         Jt.tableFromListColumns(employeeData).use();
     }
 }
