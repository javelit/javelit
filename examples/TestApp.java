import tech.catheu.jeamlit.core.Jt;

public class TestApp {
    public static void main(String[] args) {
        // loader un mega gros fichier de data
        //var data = Jt.sessionState().computeIfAbsent("data_lourde", k -> load_data_lourde("mon_fichier_lourd"));
        //Jt.text(data);

        Jt.use(Jt.title("My title"));
        if (Jt.use(Jt.button("Test Button")) || (boolean) Jt.sessionState().getOrDefault("clique", false)) {
            Jt.use(Jt.text("Button was clicked!"));
            Jt.sessionState().put("clique", true);
        }
        
        var currentValue = Jt.use(Jt.slider("Test Slider").min(0).max(100).value(50));
        Jt.use(Jt.text(currentValue > 50 ? "Vous êtes vieux" : "Vous êtes fringuant"));
        //System.out.println("Slider value: " + value);
    }
}