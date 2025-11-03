import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.javelit.core.Jt;

public class DemoApp {

    public static void main(String[] args) {
        // # Hello Cay
        var data = loadData();
        int ageLimit = Jt.slider("pick x").value(20).min(18).max(30).use().intValue();
        data = data.stream().filter(e -> (int) e.get("age") > ageLimit).toList();
        Jt.table(data).use();
    }

    public static List<Map<String, Object>> loadData() {
        List<Map<String, Object>> users = new ArrayList<>();
        users.add(Map.of("username", "player01", "age", 21, "elo", 1350));
        users.add(Map.of("username", "player02", "age", 19, "elo", 1420));
        users.add(Map.of("username", "player03", "age", 27, "elo", 1600));
        users.add(Map.of("username", "player04", "age", 24, "elo", 1510));
        users.add(Map.of("username", "player05", "age", 30, "elo", 1710));
        users.add(Map.of("username", "player06", "age", 26, "elo", 1490));
        users.add(Map.of("username", "player07", "age", 22, "elo", 1320));
        users.add(Map.of("username", "player08", "age", 28, "elo", 1580));
        users.add(Map.of("username", "player09", "age", 25, "elo", 1440));
        users.add(Map.of("username", "player10", "age", 20, "elo", 1380));
        users.add(Map.of("username", "player11", "age", 33, "elo", 1660));
        users.add(Map.of("username", "player12", "age", 29, "elo", 1540));
        users.add(Map.of("username", "player13", "age", 23, "elo", 1480));
        users.add(Map.of("username", "player14", "age", 31, "elo", 1690));
        users.add(Map.of("username", "player15", "age", 18, "elo", 1290));
        return users;
    }

}
