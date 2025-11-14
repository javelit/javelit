 import io.javelit.core.Jt;

 public class ColumnsApp {
     public static void main(String[] args) {
         var cols = Jt.columns(3).use();

         Jt.title("A cat").use(cols.col(0));
         Jt.title("A dog").use(cols.col(1));
         Jt.title("An owl").use(cols.col(2));
     }
 }
