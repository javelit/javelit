///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.60.0
//DEPS com.google.guava:guava:32.1.3-jre



import com.google.common.collect.Lists;import io.javelit.core.Jt;

public class TestJBangIntegration {
    public static void main(String[] args) {
        Jt.title("✅ Testing JBang 0.127.18 Integration - UPGRADED!").use();
        Jt.text("This test verifies that dependency resolution works with the upgraded JBang version.").use();
        
        // Use Guava to test dependency resolution
        var list = Lists.newArrayList("JBang", "0.127.18", "works!");
        Jt.text("Test result: " + String.join(" → ", list)).use();
        
        if (Jt.button("Test Dependency Loading").use()) {
            Jt.text("✅ Guava Lists class loaded successfully! Here's one: ").use();
            Jt.text("📦 Dependency resolution is working!").use();
        }
    }
}
