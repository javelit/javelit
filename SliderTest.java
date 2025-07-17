import tech.catheu.jeamlit.core.Jt;

public class SliderTest {
    public static void main(String[] args) {
        Jt.title("Slider Test");
        
        int value = Jt.slider("Test Slider", 0, 100, 50);
        
        Jt.text("Current value: " + value);
        
        if (Jt.button("Show Value")) {
            Jt.text("Selected: " + value);
        }
    }
}