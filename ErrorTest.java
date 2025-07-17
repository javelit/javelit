import tech.catheu.jeamlit.core.Jt;

public class ErrorTest {
    public static void main(String[] args) {
        Jt.title("Error Test - This should throw DuplicateWidgetIDException");
        
        // This will work fine
        Jt.button("OK");
        
        // This should throw DuplicateWidgetIDException
        Jt.button("OK");
    }
}