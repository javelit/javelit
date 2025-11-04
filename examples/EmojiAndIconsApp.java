/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.64.0

import io.javelit.core.Jt;

public class EmojiAndIconsApp {
    public static void main(String[] args) {
        Jt.text("Icon Test Suite").use();


        // Button tests
        Jt.markdown("**Button Components:**").use();
        Jt.button("Search Button").icon(":search:").use();
        Jt.button("Check Button").icon("✅").use();


        // FormSubmitButton tests
        var f = Jt.form().use();
        Jt.markdown("**Form Submit Button Components:**").use(f);
        Jt.formSubmitButton("Submit Form").icon(":send:").use(f);
        Jt.formSubmitButton("Save Form").icon("💾").use(f);


        // Error tests
        Jt.markdown("**Error Components:**").use();
        Jt.error("Error with icon").icon(":warning:").use();
        Jt.error("Error with emoji").icon("⚠️").use();


        // TextInput tests
        Jt.markdown("**Text Input Components:**").use();
        Jt.textInput("Search").icon(":search:").use();
        Jt.textInput("Email").icon("✉️").use();


        // NumberInput tests
        Jt.markdown("**Number Input Components:**").use();
        Jt.numberInput("Amount").icon(":attach_money:").use();
        Jt.numberInput("Count").icon("🔢").use();


        // PageLink tests (requires navigation context)
        Jt.markdown("**Page Link Components:**").use();
        var currentPage = Jt.navigation(
                Jt.page("/home", EmojiAndIconsApp::home).title("Home").icon(":home:").home(),
                Jt.page("/settings", EmojiAndIconsApp::settings).title("Settings").icon("⚙️")
        ).use();

        currentPage.run();

        Jt.pageLink("/home").icon(":home:").use();
        Jt.pageLink("/settings").icon("⚙️").use();
    }

    public static void home() {
        Jt.text("Home page").use();
    }

    public static void settings() {
        Jt.text("Settings page").use();
    }
}
