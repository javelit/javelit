import java.util.List;

import io.javelit.core.Jt;

 public class ProductRadioApp {
     public static void main(String[] args) {
         record Product(String name, double price) {}

         Product selected = Jt
                 .radio("Choose product",
                        List.of(new Product("Basic Plan", 9.99),
                                new Product("Pro Plan", 19.99),
                                new Product("Enterprise Plan", 49.99)))
                 .formatFunction(e -> e.name + " ($" + e.price + ")")
                 .use();

         if (selected != null) {
             Jt.text("You chose: " + selected.name()).use();
             Jt.text("Price: $" + selected.price()).use();
         }
     }
 }
