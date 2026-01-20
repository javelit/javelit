

import io.javelit.core.Jt;
import io.javelit.core.JtContainer;

public record Message(String role, String content) {

  public void use(JtContainer container) {
    if ("user".equals(role)) {
      Jt.markdown("**You:** " + content).use(container);
    } else {
      Jt.markdown("**Assistant:** " + content).use(container);
    }
  }
}
