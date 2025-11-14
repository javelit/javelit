 import io.javelit.core.Jt;

 public class ExpanderApp {
     public static void main(String[] args) {
         var expander = Jt.expander("See explanation").use();

         Jt.text("""
                 [A great explanation on the why and how of life.]
                 """).use(expander);
     }
 }
