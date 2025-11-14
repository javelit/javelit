 

 import java.time.LocalDate;
 import java.time.Period;import io.javelit.core.Jt;

 public class DateInputApp {
     public static void main(String[] args) {
         LocalDate birthday = Jt.dateInput("Your birthday").use();

         if (birthday != null) {
             int age = Period.between(birthday, LocalDate.now()).getYears();
             Jt.text("You are " + age + " years old").use();
         }
     }
 }
