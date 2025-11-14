 import io.javelit.core.Jt;

 public class TypedNumberInputApp {
     public static void main(String[] args) {
         Integer age = Jt.numberInput("Age", Integer.class)
                         .minValue(0)
                         .maxValue(150)
                         .use();

         if (age != null) {
             String category = age < 18 ? "Minor" : age < 65 ? "Adult" : "Senior";
             Jt.text("Category: " + category).use();
         }
     }
 }
