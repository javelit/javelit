import tech.catheu.jeamlit.core.Jt;

public class MyApp {

    public static void main(String[] args) {
        // Button with session state
        Jt.sessionState().putIfAbsent("clicks", 0);


        Jt.use(Jt.title("Jeamlit Demo App"));
        Jt.use(Jt.text("Welcome to Jeamlit - Streamlit for Java!"));
        Jt.use(Jt.text("This demo shows basic components and state management."));
        
        // Interactive widgets
        Double age = Jt.use(Jt.slider("Select your age").min(0).max(100).value(25));
        Jt.use(Jt.text("You selected age: " + age));
        Jt.use(Jt.text("Age category: " + getAgeCategory(age)));

        if (Jt.use(Jt.button("Click me!"))) {
            Jt.use(Jt.text("Button was clicked!"));
            int clickCount = (Integer) Jt.sessionState().computeInt("clicks", (k, v) -> v + 1);
            Jt.use(Jt.text("Button clicked " + clickCount + " times"));
        }
        
        // Show details button
        Jt.sessionState().putIfAbsent("details_shown", 0);
        
        if (Jt.use(Jt.button("Show details"))) {
            Jt.use(Jt.text("🎯 This is a detailed view!"));
            Jt.use(Jt.text("Current timestamp: " + System.currentTimeMillis()));
            
            int detailsCount = Jt.sessionState().computeInt("details_shown", (k, v) -> (Integer) v + 1);
            
            Jt.use(Jt.text("Details shown " + detailsCount + " times"));
        }
        
        // Footer
        Jt.use(Jt.text("---"));
        Jt.use(Jt.text("💡 Try changing values and see the app update in real-time!"));
    }
    
    private static String getAgeCategory(Double age) {
        if (age < 18) return "Minor";
        if (age < 65) return "Adult";
        return "Senior";
    }
}