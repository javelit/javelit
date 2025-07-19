import tech.catheu.jeamlit.core.Jt;

public class TestApp {
    public static void main(String[] args) {
        // loader un mega gros fichier de data
        //var data = Jt.sessionState().computeIfAbsent("data_lourde", k -> load_data_lourde("mon_fichier_lourd"));
        //Jt.text(data);

        Jt.title("My title");
        if (Jt.button("Test Button") || (boolean) Jt.sessionState().getOrDefault("clique", false)) {
            Jt.text("Button was clicked!");
            Jt.sessionState().put("clique", true);
        }
        
        var currentValue = Jt.slider("Test Slider", 0, 100, 50);
        Jt.text(currentValue > 50 ? "Vous êtes vieux" : "Vous êtes fringuant");
        //System.out.println("Slider value: " + value);
    }
}