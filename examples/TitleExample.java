///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.32.0





import java.util.List;import io.jeamlit.components.layout.ColumnsComponent;import io.jeamlit.core.Jt;

public class TitleExample {

    public static void main(String[] args) throws InterruptedException {
        Jt.title("This is a title").use();
        Jt.title("_Jeamlit_ is cool :sunglasses:").use();
    }
}
