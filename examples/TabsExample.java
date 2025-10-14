///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.44.0


import java.util.List;

import io.jeamlit.core.Jt;

public class TabsExample {

    public static void main(String[] args) throws InterruptedException {
        final var tabs = Jt.tabs(List.of("Cat", "Dog", "Owl")).use();
        Jt.title("A cat").use(tabs.tab(0));
        Jt.title("A dog").use(tabs.tab(1));
        Jt.title("An owl").use(tabs.tab(2));


        final var secondTabs = Jt.tabs(List.of("Cat", "Dog", "Owl")).key("second-tab").use();
        Jt.title("A cat again").use(secondTabs.tab("Cat"));
        Jt.title("A dog again").use(secondTabs.tab("Dog"));
        Jt.title("An owl again").use(secondTabs.tab("Owl"));


        // no need for keys if the tabs can be differentiated based on config
        final var thirdTabs = Jt.tabs(List.of("C", "D", "O")).use();
        Jt.title("A cat again").use(thirdTabs.tab("Cat"));
        Jt.title("A dog again").use(thirdTabs.tab("Dog"));
        Jt.title("An owl again").use(thirdTabs.tab("Owl"));
    }
}
