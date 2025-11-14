 import io.javelit.core.Jt;

 public class StaticImageApp {
     public static void main(String[] args) {
          // assume static/mountains.jpg is present in the working directory
         Jt.image("app/static/mountains.jpg").use();
     }
 }
