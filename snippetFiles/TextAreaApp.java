import io.javelit.core.Jt;

public class TextAreaApp {
  public static void main(String[] args) {
    String feedback = Jt.textArea("Your feedback").use();

    if (!feedback.isEmpty()) {
      Jt.text("Thank you for your feedback!").use();
      Jt.text("Character count: " + feedback.length()).use();
    }
  }
}
