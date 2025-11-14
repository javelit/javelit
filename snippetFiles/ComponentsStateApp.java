 import io.javelit.core.Jt;

 public class ComponentsStateApp {
     public static void main(String[] args) {
         double volumeFromUse = Jt.slider("Volume").key("volume").min(0).max(100).value(50).use();
         double volumeFromState = Jt.componentsState().getDouble("volume");

         Jt.text("Volume from slider return value: " + volumeFromUse).use();
         Jt.text("Value from components state map: " + volumeFromState).use();
     }
 }
