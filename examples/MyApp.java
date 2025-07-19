import tech.catheu.jeamlit.core.Jt;

public class MyApp {

    public static void main(String[] args) {
        // Button with session state
        Jt.sessionState().putIfAbsent("clicks", 0);


        Jt.title("Jeamlit Demo App");
        Jt.text("Welcome to Jeamlit - Streamlit for Java!");
        Jt.text("This demo shows basic components and state management.");
        
        // Interactive widgets
        int age = Jt.slider("Select your age", 0, 100, 25);
        Jt.text("You selected age: " + age);
        Jt.text("Age category: " + getAgeCategory(age));

        if (Jt.button("Click me!")) {
            Jt.text("Button was clicked!");
            int clickCount = (Integer) Jt.sessionState().computeInt("clicks", (k, v) -> v + 1);
            Jt.text("Button clicked " + clickCount + " times");
        }
        
        // Show details button
        Jt.sessionState().putIfAbsent("details_shown", 0);
        
        if (Jt.button("Show details")) {
            Jt.text("🎯 This is a detailed view!");
            Jt.text("Current timestamp: " + System.currentTimeMillis());
            
            int detailsCount = Jt.sessionState().computeInt("details_shown", (k, v) -> (Integer) v + 1);
            
            Jt.text("Details shown " + detailsCount + " times");
        }
        
        // Footer
        Jt.text("---");
        Jt.text("💡 Try changing values and see the app update in real-time!");
    }
    
    private static String getAgeCategory(int age) {
        if (age < 18) return "Minor";
        if (age < 65) return "Adult";
        return "Senior";
    }
}