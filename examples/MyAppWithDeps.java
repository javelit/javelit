//DEPS com.google.guava:guava:32.1.3-jre
//DEPS org.apache.commons:commons-lang3:3.13.0

import tech.catheu.jeamlit.core.Jt;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

public class MyAppWithDeps {
    public static void main(String[] args) {
        Jt.use(Jt.title("Jeamlit with JBang Dependencies!"));
        Jt.use(Jt.text("This app uses external dependencies resolved via JBang directives."));
        
        // Use Guava
        var fruits = Lists.newArrayList("Apple", "Banana", "Cherry", "Date");
        Jt.use(Jt.text("Fruits (using Guava): " + fruits));
        
        // Use Apache Commons Lang
        String text = "  hello world  ";
        String capitalized = StringUtils.capitalize(StringUtils.trim(text));
        Jt.use(Jt.text("Capitalized text (using Commons Lang): " + capitalized));
        
        // Interactive element
        if (Jt.use(Jt.button("Show more details"))) {
            Jt.use(Jt.text("🎯 Guava version: " + Lists.class.getPackage().getImplementationVersion()));
            Jt.use(Jt.text("📦 Commons Lang available: " + (StringUtils.class != null)));
            Jt.use(Jt.text("⚡ Hot reload with dependencies works!"));
        }
        
        Jt.use(Jt.text("---"));
        Jt.use(Jt.text("💡 Edit this file to test hot reloading with dependencies!"));
    }
}