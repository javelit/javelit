import tech.catheu.jeamlit.core.Jt;

public class Empty {

    public static void main(String[] args) {
        Jt.title("Test App").key("lol").use();
        Jt.title("Test App").key("lol").use();
        Jt.text("This app has an error.").use();
        Jt.error("""
                         ```
                         RAHHA very lonig textA very  textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very  lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig textA very lonig text
                         ```
                         """).icon("🚨").use();
        //Jt.text("This app has another one").use();
        lol();
        //Jt.text("Another text").use();
    }

    public static void lol() {
        if (true) {
            try {
                throw new RuntimeException("This app has an error.");
            } catch (Exception e) {
                throw new RuntimeException("Caught you", e);
            }
        }
    }}
