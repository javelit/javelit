

import io.javelit.core.Jt;import io.javelit.core.JtContainer;

public record Message(String role, String content) {
    // used to avoid collisions if the same message appears twice (eg same question from the user)
    static int messageCounter = 0;

    public void use(JtContainer container) {
        var key = "message_" + messageCounter++;
        if ("user".equals(role)) {
            Jt.markdown("**You:** " + content).key(key).use(container);
        } else {
            Jt.markdown("**Assistant:** " + content).key(key).use(container);
        }
    }
}
