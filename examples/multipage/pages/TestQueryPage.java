package pages;

import io.javelit.core.Jt;

public class TestQueryPage {
    public static void app() {
        final var l = Jt.urlQueryParameters().get("name");
        var name = "unknown visitor";
        if (l != null && !l.isEmpty()) {
            name = l.getFirst();
        }

        Jt.title("🔍 Query Parameter Test").use();
        Jt.text("Hello %s!".formatted(name)).use();
    }

    private TestQueryPage() {
    }
}
