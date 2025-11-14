 import io.javelit.core.Jt;

 public class SliderApp {
     public static void main(String[] args) {
         var age = Jt.slider("How old are you?")
             .min(0)
             .max(130)
             .value(25)
             .use();

         Jt.text("I'm " + age + " years old").use();
     }
 }
