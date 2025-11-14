import java.util.List;

import io.javelit.core.Jt;

public class ProcessingSelectBoxApp {
  public static void main(String[] args) {
    String priority = Jt.selectbox("Task priority",
                                   List.of("Low", "Medium", "High", "Critical"))
                        .index(1)
                        .use();
    Jt.text("Priority: " + priority).use();
  }
}
