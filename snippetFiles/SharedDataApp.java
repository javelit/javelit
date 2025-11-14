 import io.javelit.core.Jt;

 public class SharedDataApp {
     public static void main(String[] args) {
         // initialization
         Jt.cache().putIfAbsent("counter", 0);
         // increment visits
         int totalVisits = Jt.cache().computeInt("counter", (k, v) -> v + 1);

         Jt.text("Total app visits: " + totalVisits).use();
     }
 }
