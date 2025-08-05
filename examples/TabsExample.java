import tech.catheu.jeamlit.core.Jt;
import tech.catheu.jeamlit.components.layout.ColumnsComponent;

import java.util.List;

public class TabsExample {

    public static void main(String[] args) throws InterruptedException {
        final var tabs = Jt.tabs("first-tabs",List.of("Cat", "Dog", "An Owl")).use();
        Jt.title("A cat").use(tabs.tab(0));
        Jt.title("A dog").use(tabs.tab(1));
        Jt.title("An owl").use(tabs.tab(2));


        final var secondTabs = Jt.tabs("second-tabs", List.of("Cat", "Dog", "Owl")).key("second-tab").use();
        Jt.title("A cat again").use(secondTabs.tab("Cat"));
        Jt.title("A dog again").use(secondTabs.tab("Dog"));
        Jt.title("An owl again").use(secondTabs.tab("Owl"));
    }
}