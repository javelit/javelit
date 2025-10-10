///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.32.0

import io.jeamlit.core.Jt;

public class EmojiAndIconsApp {
    public static void main(String[] args) {
        Jt.text("Icon Test Suite").use();
        
        
        // Button tests
        Jt.text("**Button Components:**").use();
        Jt.button("Search Button").icon(":search:").use();
        Jt.button("Check Button").icon("✅").use();
        
        
        // FormSubmitButton tests
        var f = Jt.form().use();
        Jt.text("**Form Submit Button Components:**").use(f);
        Jt.formSubmitButton("Submit Form").icon(":send:").use(f);
        Jt.formSubmitButton("Save Form").icon("💾").use(f);
        
        
        // Error tests
        Jt.text("**Error Components:**").use();
        Jt.error("Error with icon").icon(":warning:").use();
        Jt.error("Error with emoji").icon("⚠️").use();
        
        
        // TextInput tests
        Jt.text("**Text Input Components:**").use();
        Jt.textInput("Search").icon(":search:").use();
        Jt.textInput("Email").icon("✉️").use();
        
        
        // NumberInput tests
        Jt.text("**Number Input Components:**").use();
        Jt.numberInput("Amount").icon(":attach_money:").use();
        Jt.numberInput("Count").icon("🔢").use();
        
        
        // PageLink tests (requires navigation context)
        Jt.text("**Page Link Components:**").use();
        Jt.navigation(
            Jt.page(Home.class).title("Home").icon(":home:").home(),
            Jt.page(Settings.class).title("Settings").icon("⚙️")
        ).use();
        
        Jt.pageLink(Home.class).icon(":home:").use();
        Jt.pageLink(Settings.class).icon("⚙️").use();
    }
    
    public static class Home {
        public static void main(String[] args) {
            Jt.text("Home page").use();
        }
    }
    
    public static class Settings {
        public static void main(String[] args) {
            Jt.text("Settings page").use();
        }
    }
}
