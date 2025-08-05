import tech.catheu.jeamlit.core.Jt;

public class PopoverExample {

    public static void main(String[] args) throws InterruptedException {
        final var popoverC = Jt.popover("popover-demo", "Open settings").use();
        Jt.title("Settings").use(popoverC);
        Jt.text("Configure your preferences here.").use(popoverC);
        if (Jt.button("click me.").help("HEHE").use(popoverC)) {
            Jt.text("I was clicked !").use();
        }
        
        Jt.text("This is some content outside the popover.").use();
        
        final var popover2 = Jt.popover("popover-help", "Help")
            .help("Click to open help information")
            .useContainerWidth(true)
            .use();
        Jt.text("Need assistance? Check our FAQ or contact support.").use(popover2);


        final var popover3 = Jt.popover("popover-disabled", "Disabled")
                .disabled(false)
                .use();
        Jt.text("never see that").use(popover3);
    }
}