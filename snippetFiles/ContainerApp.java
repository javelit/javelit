 import io.javelit.core.Jt;

 public class ContainerApp {
     public static void main(String[] args) {
         var container = Jt.container().use();

         Jt.text("This is inside the container").use(container);
         Jt.text("This is outside the container").use();
         Jt.text("This is inside too").use(container);
     }
 }
