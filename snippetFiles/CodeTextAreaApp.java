import io.javelit.core.Jt;

public class CodeTextAreaApp {
  public static void main(String[] args) {
    String code = Jt.textArea("Enter your Java code")
                    .height(200)
                    .placeholder("public class MyClass {\n    // Your code here\n}")
                    .use();

    if (!code.isEmpty()) {
      Jt.text("Code preview:").use();
      Jt.code(code).language("java").use();
    }
  }
}
