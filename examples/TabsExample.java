///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.32.0





import java.util.List;import io.jeamlit.components.layout.ColumnsComponent;import io.jeamlit.core.Jt;

public class TabsExample {

    public static void main(String[] args) throws InterruptedException {
        final var tabs = Jt.tabs("first-tabs", List.of("Cat", "Dog", "An Owl")).use();
        Jt.title("A cat").use(tabs.tab(0));
        Jt.title("A dog").use(tabs.tab(1));
        Jt.title("An owl").use(tabs.tab(2));


        final var secondTabs = Jt.tabs("second-tabs", List.of("Cat", "Dog", "Owl")).key("second-tab").use();
        Jt.title("A cat again").use(secondTabs.tab("Cat"));
        Jt.title("A dog again").use(secondTabs.tab("Dog"));
        Jt.title("An owl again").use(secondTabs.tab("Owl"));
    }
}
