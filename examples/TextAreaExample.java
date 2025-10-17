///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.47.0



import io.jeamlit.components.input.TextAreaComponent;import io.jeamlit.core.Jt;

public class TextAreaExample {
    public static void main(String[] args) {
        Jt.title("Text Area Component Test").use();
        
        // Basic text area
        String basicText = new TextAreaComponent.Builder("Basic Text Area")
            .placeholder("Enter your text here...")
            .use();
        
        Jt.text("You entered: " + basicText).use();
        
        // Text area with max chars
        String limitedText = new TextAreaComponent.Builder("Limited Text Area")
            .maxChars(100)
            .placeholder("Max 100 characters...")
            .help("This text area has a 100 character limit")
            .use();
        
        Jt.text("Limited text (" + limitedText.length() + " chars): " + limitedText).use();
        
        // Text area with fixed height
        String fixedHeightText = new TextAreaComponent.Builder("Fixed Height Text Area")
            .height("150")
            .placeholder("This has a fixed height of 150px...")
            .use();
        
        Jt.text("Fixed height text: " + fixedHeightText).use();
        
        // Text area with content height
        String contentHeightText = new TextAreaComponent.Builder("Auto-Resize Text Area")
            .height("content")
            .placeholder("This will auto-resize to fit content...")
            .use();
        
        Jt.text("Auto-resize text: " + contentHeightText).use();
        
        if (Jt.button("Clear All").use()) {
            // This would reset values in a real Streamlit-like session
            Jt.text("Button clicked! (Values would be cleared in real session)").use();
        }
    }
}
