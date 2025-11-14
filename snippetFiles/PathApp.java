 import io.javelit.core.Jt;

 public class PathApp {
     public static void main(String[] args) {
         Jt.navigation(
                  Jt.page("/home", () -> home()),
                  Jt.page("/details", () -> details())).use();

         Jt.text("The current path is: " + Jt.urlPath()).use();
     }

     public static void home() {
         Jt.title("Home Page").use();
     }

     public static void details() {
         Jt.title("Details Page").use();
     }
 }
