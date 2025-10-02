///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.26.0





import java.util.List;import io.jeamlit.components.layout.ColumnsComponent;import io.jeamlit.core.Jt;

public class ExpanderExample {

    public static void main(String[] args) throws InterruptedException {
        final var expanderC = Jt.expander("the-exaplanation", "See explanation").use();
        Jt.title("THE EXPLANATION TITLE").use(expanderC);
        Jt.text("Ita fac, no way: Vindica te tibi, et tempus. Lorem ipsum.").use(expanderC);
    }
}
