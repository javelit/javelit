package pages;

import tech.catheu.jeamlit.core.Jt;

public class TestQueryPage {
    public static void main(String[] args) {
        final var l = Jt.urlQueryParameters().get("name");
        var name = "unknown visitor";
        if (l != null && !l.isEmpty()) {
            name = l.getFirst();
        }

        Jt.title("🔍 Query Parameter Test").use();
        Jt.text("Hello %s!".formatted(name)).use();
    }
}
