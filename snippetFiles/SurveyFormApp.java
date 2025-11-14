 

 import java.util.List;import io.javelit.core.Jt;

 public class SurveyFormApp {
     public static void main(String[] args) {
         var form = Jt.form().use();
         double satisfaction = Jt.slider("Satisfaction (1-10)").min(1).max(10).value(5).use(form);
         String feedback = Jt.textArea("Additional feedback").use(form);
         String department = Jt.selectbox("Department",
                                          List.of("Engineering", "Marketing", "Sales", "Support")).use(form);

         if (Jt.formSubmitButton("Submit Survey").use(form)) {
             Jt.text("Thank you for your feedback!").use();
             Jt.text("Satisfaction: " + satisfaction + "/10").use();
         }
     }
 }
