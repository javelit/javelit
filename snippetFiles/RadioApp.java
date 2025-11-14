import java.util.List;

import io.javelit.core.Jt;

public class RadioApp {
  public static void main(String[] args) {
    String size = Jt.radio("Select size",
                           List.of("Small", "Medium", "Large")).use();

    if (size != null) {
      Jt.text("Selected size: " + size).use();
    }
  }
}
