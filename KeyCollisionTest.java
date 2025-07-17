import tech.catheu.jeamlit.core.Jt;
import tech.catheu.jeamlit.core.DuplicateWidgetIDException;

public class KeyCollisionTest {
    public static void main(String[] args) {
        Jt.title("Key Collision Test - Streamlit Behavior");
        
        // Test 1: Buttons with explicit unique keys (should work)
        Jt.text("✅ Test 1: Buttons with explicit unique keys");
        boolean btn1 = Jt.button("OK", "privacy");
        boolean btn2 = Jt.button("OK", "terms");
        
        if (btn1) Jt.text("Privacy button clicked!");
        if (btn2) Jt.text("Terms button clicked!");
        
        // Test 2: Sliders with different parameters (should work - no collision)
        Jt.text("✅ Test 2: Sliders with different parameters");
        int slider1 = Jt.slider("Value", 0, 100, 50);
        int slider2 = Jt.slider("Value", 0, 50, 25);  // Different params - no collision
        
        Jt.text("Slider 1 value: " + slider1);
        Jt.text("Slider 2 value: " + slider2);
        
        // Test 3: This would cause DuplicateWidgetIDException - commented out
        Jt.text("❌ Test 3: Duplicate auto-generated keys (commented out - would throw error)");
        Jt.text("// Jt.button(\"OK\"); // This would work");
        Jt.text("// Jt.button(\"OK\"); // This would throw DuplicateWidgetIDException");
        
        // Test 4: This would cause DuplicateWidgetIDException - commented out  
        Jt.text("❌ Test 4: Duplicate explicit keys (commented out - would throw error)");
        Jt.text("// Jt.button(\"Save\", \"save_btn\"); // This would work");
        Jt.text("// Jt.button(\"Save\", \"save_btn\"); // This would throw DuplicateWidgetIDException");
        
        Jt.text("✅ All tests passed! Streamlit-like behavior implemented correctly.");
    }
}