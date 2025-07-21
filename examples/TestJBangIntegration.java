//DEPS com.google.guava:guava:32.1.3-jre


import tech.catheu.jeamlit.core.Jt;
import com.google.common.collect.Lists;

public class TestJBangIntegration {
    public static void main(String[] args) {
        Jt.use(Jt.title("✅ Testing JBang 0.127.18 Integration - UPGRADED!"));
        Jt.use(Jt.text("This test verifies that dependency resolution works with the upgraded JBang version."));
        
        // Use Guava to test dependency resolution
        var list = Lists.newArrayList("JBang", "0.127.18", "works!");
        Jt.use(Jt.text("Test result: " + String.join(" → ", list)));
        
        if (Jt.use(Jt.button("Test Dependency Loading"))) {
            Jt.use(Jt.text("✅ Guava Lists class loaded successfully! Here's one: "));
            Jt.use(Jt.text("📦 Dependency resolution is working!"));
        }
    }
}