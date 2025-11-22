import java.util.List;

import io.javelit.core.Jt;

public class QueryParamsApp {
  public static void main(String[] args) {
    var params = Jt.urlQueryParameters();

    String name = params.getOrDefault("name", List.of("unknown user")).get(0);

    Jt.title("Query params reader").use();
    Jt.text("Hello " + name).use();
    // URL: ?name=Alice would show:
    // Hello Alice
  }
}
