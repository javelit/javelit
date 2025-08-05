import tech.catheu.jeamlit.core.Jt;
import tech.catheu.jeamlit.components.layout.ColumnsComponent;

import java.util.List;

public class MyApp {

    public static void main(String[] args) throws InterruptedException {
        // Button with session state
        Jt.sessionState().putIfAbsent("clicks", 0);

        Jt.title("Jeamlit Demo App").use();
        Jt.text("Welcome to Jeamlit - Streamlit for Java!").use();
        Jt.text("This demo shows basic components and state management.").use();

        var containerDebug = Jt.container("debug-container")
                .border(false)
                .use();
        Jt.text("My debug container").use(containerDebug);

        final var columns = Jt.columns("the-column", 3)
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
        //Jt.title("ANOTHER ONE").use(Jt.sidebar());
        Jt.title("PLEASE WAIT").use(Jt.sidebar());
        //Jt.title("ANOTHER ONE").use(Jt.sidebar());
        Jt.title("WAIT").use(Jt.sidebar());
        Double age = Jt.slider("Select your age").min(0).max(100).value(30).use();
        Jt.text("You selected age: " + age).use();
        var container = Jt.container("container-1").use();
        Jt.text("test in container").use(container);
        var container2 =  Jt.container("container-2").use(container);
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

        var empty = Jt.empty("debug-empty").border(true).use(columns.col(2));
        Jt.text("debug empty - 1 ").use(empty);
        Thread.sleep(5000);
        Jt.text("debug empty - 2 ").use(empty);
        System.out.println("GOING TO SLEEP FOR LAST ONE");
        Thread.sleep(5000);
        Jt.text("debug empty - 3 ").use(empty);
        System.out.println("FNISHED SLEEPING FOR LAST ONE");
    }
    
    private static String getAgeCategory(Double age) {
        if (age < 18) return "Minor";
        if (age < 65) return "Adult";
        return "Senior";
    }
}