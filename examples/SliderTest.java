import tech.catheu.jeamlit.core.Jt;

public class SliderTest {
    public static void main(String[] args) {
        Jt.use(Jt.title("Slider Test"));
        
        int value = Jt.use(Jt.slider("Test Slider").min(0).max(100).value(50));
        
        Jt.use(Jt.text("Current value: " + value));
        
        if (Jt.use(Jt.button("Show Value"))) {
            Jt.use(Jt.text("Selected: " + value));
        }
    }
}