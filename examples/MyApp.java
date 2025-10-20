///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.49.0





import java.util.List;import io.javelit.components.layout.ColumnsComponent;import io.javelit.core.Jt;
import io.javelit.core.JtContainer;

public class MyApp {

    public static void main(String[] args) throws InterruptedException {
        // Button with session state
        Jt.sessionState().putIfAbsent("clicks", 0);

        Jt.title("Javelit Demo App").use();
        Jt.text("Welcome to Javelit - Streamlit for Java!").use();
        Jt.text("This demo shows basic components and state management.").use();

        var containerDebug = Jt.container()
                .border(false)
                .use();
        Jt.text("My debug container").use(containerDebug);

        final var columns = Jt.columns(3)
                .verticalAlignment(ColumnsComponent.VerticalAlignment.TOP)
                .border(true)
                .gap(ColumnsComponent.Gap.SMALL)
                .widths(List.of(0.2, 0.3, 0.5))
                .use();
        Jt.text("In column 1 BUT CHANGED! ").use(columns.col(1));
        Jt.text("In column 0 ! ").use(columns.col(0));
        Jt.text("In column 0 Again BUT CHANGED! ").use(columns.col(0));
        Jt.text("In column 0 and Again! ").use(columns.col(0));
        //Jt.text("In column 2 ! ").use(columns.col(2));
        
        // Interactive widgets
        //Jt.title("ANOTHER ONE").use(JtContainer.SIDEBAR);
        Jt.title("PLEASE WAIT").use(JtContainer.SIDEBAR);
        //Jt.title("ANOTHER ONE").use(JtContainer.SIDEBAR);
        Jt.title("WAIT").use(JtContainer.SIDEBAR);
        Double age = Jt.slider("Select your age").min(0).max(100).value(30).use();
        Jt.text("You selected age: " + age).use();
        var container = Jt.container().key("container1").use();
        Jt.text("test in container").use(container);
        var container2 =  Jt.container().key("container2").use(container);
        if (Jt.button("hihi this is in container 2").use(container2)) {
            Jt.text("clicked and inside container 1 and container 2 ").use(container);
        }
        Jt.text("Age category: " + getAgeCategory(age)).use();
        if (Jt.button("Click me!").use()) {
            Jt.text("Button was clicked!").use();
            int clickCount = (Integer) Jt.sessionState().computeInt("clicks", (k, v) -> v + 1);
            Jt.text("Button clicked " + clickCount + " times").use();
        }
        
        // Show details button
        Jt.sessionState().putIfAbsent("details_shown", 0);
        
        if (Jt.button("Show details").use()) {
            Jt.text("🎯 This is a detailed view!").use();
            Jt.text("Current timestamp: " + System.currentTimeMillis()).use();
            
            int detailsCount = Jt.sessionState().computeInt("details_shown", (k, v) -> (Integer) v + 1);
            
            Jt.text("Details shown " + detailsCount + " times").use();
        }
        
        // Footer
        Jt.text("---").use();
        Jt.text("another text in container - but put way later").use(container);
        Jt.text("💡 Try changing values and see the app update in real-time!").use();

        var empty = Jt.text("col 2").use(columns.col(2));
    }
    
    private static String getAgeCategory(Double age) {
        if (age < 18) {
            return "Minor";
        }
        if (age < 65) {
            return "Adult";
        }
        return "Senior";
    }
}
