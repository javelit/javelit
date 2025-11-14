import java.util.List;

import io.javelit.core.Jt;

public class SelectBoxApp {
  public static void main(String[] args) {
    String country = Jt.selectbox("Select your country",
                                  List.of("United States", "Canada", "United Kingdom", "Germany", "France")).use();

    if (country != null) {
      Jt.text("Selected country: " + country).use();
    }
  }
}
