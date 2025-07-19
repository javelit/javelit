//DEPS com.google.guava:guava:32.1.3-jre
//DEPS org.apache.commons:commons-lang3:3.13.0

import tech.catheu.jeamlit.core.Jt;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

public class MyAppWithDeps {
    public static void main(String[] args) {
        Jt.title("Jeamlit with JBang Dependencies!");
        Jt.text("This app uses external dependencies resolved via JBang directives.");
        
        // Use Guava
        var fruits = Lists.newArrayList("Apple", "Banana", "Cherry", "Date");
        Jt.text("Fruits (using Guava): " + fruits);
        
        // Use Apache Commons Lang
        String text = "  hello world  ";
        String capitalized = StringUtils.capitalize(StringUtils.trim(text));
        Jt.text("Capitalized text (using Commons Lang): " + capitalized);
        
        // Interactive element
        if (Jt.button("Show more details")) {
            Jt.text("🎯 Guava version: " + Lists.class.getPackage().getImplementationVersion());
            Jt.text("📦 Commons Lang available: " + (StringUtils.class != null));
            Jt.text("⚡ Hot reload with dependencies works!");
        }
        
        Jt.text("---");
        Jt.text("💡 Edit this file to test hot reloading with dependencies!");
    }
}