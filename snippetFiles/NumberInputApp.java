import io.javelit.core.Jt;

public class NumberInputApp {
  public static void main(String[] args) {
    Number quantity = Jt.numberInput("Quantity").minValue(1).maxValue(100).use();

    if (quantity != null) {
      Jt.text("You selected: " + quantity).use();
    }
  }
}
