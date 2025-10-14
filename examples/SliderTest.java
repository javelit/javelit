///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.44.0

import io.jeamlit.core.Jt;

public class SliderTest {
    public static void main(String[] args) {
        Jt.title("Slider Test").use();
        
        Double value = Jt.slider("Test Slider").min(0).max(100).value(50).step(1).use();
        
        Jt.text("Current value: " + value).use();
        
        if (Jt.button("Show Value").use()) {
            Jt.text("Selected: " + value).use();
        }
    }
}
