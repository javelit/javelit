///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.44.0
//DEPS org.apache.commons:commons-lang3:3.13.0
//DEPS com.google.guava:guava:32.1.3-jre

import com.google.common.collect.Lists;
import io.jeamlit.core.Jt;
import org.apache.commons.lang3.StringUtils;

public class MyAppWithDeps {
    public static void main(String[] args) {
        Jt.title("Jeamlit with JBang Dependencies!").use();
        Jt.text("This app uses external dependencies resolved via JBang directives.").use();
        
        // Use Guava
        var fruits = Lists.newArrayList("Apple", "Banana", "Cherry", "Date");
        Jt.text("Fruits (using Guava): " + fruits).use();
        
        // Use Apache Commons Lang
        String text = "  hello world  ";
        String capitalized = StringUtils.capitalize(StringUtils.trim(text));
        Jt.text("Capitalized text (using Commons Lang): " + capitalized).use();
        
        // Interactive element
        if (Jt.button("Show more details").use()) {
            Jt.text("🎯 Guava version: " + Lists.class.getPackage().getImplementationVersion()).use();
            Jt.text("📦 Commons Lang available: " + (StringUtils.class != null)).use();
            Jt.text("⚡ Hot reload with dependencies works!").use();
        }
        
        Jt.text("---").use();
        Jt.text("💡 Edit this file to test hot reloading with dependencies!").use();
    }
}
