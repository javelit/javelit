///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.70.0

import io.javelit.core.Jt;

public class PopoverExample {

    public static void main(String[] args) throws InterruptedException {
        final var popoverC = Jt.popover("Open settings").use();
        Jt.title("Settings").use(popoverC);
        Jt.text("Configure your preferences here.").use(popoverC);
        if (Jt.button("click me.").help("HEHE").use(popoverC)) {
            Jt.text("I was clicked !").use();
        }
        
        Jt.text("This is some content outside the popover.").use();
        
        final var popover2 = Jt.popover("Help")
            .help("Click to open help information")
            .useContainerWidth(true)
            .use();
        Jt.text("Need assistance? Check our FAQ or contact support.").use(popover2);


        final var popover3 = Jt.popover("Disabled")
                .disabled(false)
                .use();
        Jt.text("never see that").use(popover3);
    }
}
