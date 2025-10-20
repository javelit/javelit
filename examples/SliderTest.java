///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.49.0

import io.javelit.core.Jt;

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
