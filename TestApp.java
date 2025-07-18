import tech.catheu.jeamlit.core.Jt;

public class TestApp {
    public static void main(String[] args) {
        Jt.title("My title");
        if (Jt.button("Test Button")) {
            Jt.text("Button was clicked!");
        }
        
        int value = Jt.slider("Test Slider", 0, 100, 50);
        System.out.println("Slider value: " + value);
    }
}