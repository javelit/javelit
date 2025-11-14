 import io.javelit.core.Jt;

 public class TableArrayApp {
     public static void main(String[] args) {
         record Product(String name, double price, boolean inStock) {}

         Product[] products = {
             new Product("Laptop", 999.99, true),
             new Product("Mouse", 25.50, false),
             new Product("Keyboard", 75.00, true)
         };

         Jt.table(products).use();
     }
 }
