 

 import java.util.List;import io.javelit.core.Jt;

 public class TableApp {
     public static void main(String[] args) {
         record Person(String name, int age, String city) {
         }

         List<Object> data = List.of(new Person("Alice", 25, "New York"),
                                     new Person("Bob", 30, "San Francisco"),
                                     new Person("Charlie", 35, "Chicago"));

         Jt.table(data).use();
     }
 }
