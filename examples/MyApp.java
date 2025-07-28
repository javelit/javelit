import tech.catheu.jeamlit.core.Jt;

public class MyApp {

    public static void main(String[] args) {
        // Button with session state
        Jt.sessionState().putIfAbsent("clicks", 0);

        Jt.title("Jeamlit Demo App").use();
        Jt.text("Welcome to Jeamlit - Streamlit for Java!").use();
        Jt.text("This demo shows basic components and state management.").use();
        
        // Interactive widgets
        //Jt.title("ANOTHER ONE").use(Jt.sidebar());
        //Jt.title("PLEASE WAIT").use(Jt.sidebar());
        //Jt.title("ANOTHER ONE").use(Jt.sidebar());
        //Jt.title("WAIT").use(Jt.sidebar());
        Double age = Jt.slider("Select your age").min(0).max(100).value(30).use();
        Jt.text("You selected age: " + age).use();
        var containerLayout = Jt.container("container-1").use();
        Jt.text("test in layout").use(containerLayout);
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
        Jt.text("another text in layout - but put way later").use(containerLayout);
        Jt.text("💡 Try changing values and see the app update in real-time!").use();
    }
    
    private static String getAgeCategory(Double age) {
        if (age < 18) return "Minor";
        if (age < 65) return "Adult";
        return "Senior";
    }
}