import tech.catheu.jeamlit.core.Jt;
import tech.catheu.jeamlit.components.layout.ColumnsComponent;

import java.util.List;

public class ExpanderExample {

    public static void main(String[] args) throws InterruptedException {
        final var expanderC = Jt.expander("the-exaplanation", "See explanation").use();
        Jt.title("THE EXPLANATION TITLE").use(expanderC);
        Jt.text("Ita fac, no way: Vindica te tibi, et tempus. Lorem ipsum.").use(expanderC);
    }
}