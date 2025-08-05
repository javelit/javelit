import tech.catheu.jeamlit.core.Jt;
import tech.catheu.jeamlit.components.layout.ColumnsComponent;

import java.util.List;

public class EmptyExample {

    public static void main(String[] args) throws InterruptedException {
        var emptyContainer = Jt.empty("emtpy-1").use();
        for (int seconds : List.of(0, 1,2,3,4,5,6,7,8,9)) {
            Jt.text("⏳ %s seconds have passed".formatted(seconds)).use(emptyContainer);
            Thread.sleep(1000);
        }
        Jt.text("The 10 seconds are over!").use(emptyContainer);
        Jt.button("re-run").use();
    }
}