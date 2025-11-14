 import io.javelit.core.Jt;

 public class MultiSubmitApp {
     public static void main(String[] args) {
         var form = Jt.form("document").use();

         String title = Jt.textInput("Document Title").use(form);
         String content = Jt.textArea("Content").use(form);

         if (Jt.formSubmitButton("Save Draft").key("save").use(form)) {
             Jt.text("Draft saved: " + title).use();
         }

         if (Jt.formSubmitButton("Publish").key("publish").use(form)) {
             Jt.text("Document published: " + title).use();
         }
     }
 }
