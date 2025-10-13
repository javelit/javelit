///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.40.0

import io.jeamlit.core.Jt;

public class TestApp99 {
    public static void main(String[] args) {
        // loader un mega gros fichier de data
        //var data = Jt.sessionState().computeIfAbsent("data_lourde", k -> load_data_lourde("mon_fichier_lourd"));
        //Jt.text(data);

        Jt.title("My title").use();
        if (Jt.button("Test Button").use() || (boolean) Jt.sessionState().getOrDefault("clique", false)) {
            Jt.text("Button was clicked!").use();
            Jt.sessionState().put("clique", true);
        }
        
        var currentValue = Jt.slider("Test Slider").min(0).max(100).value(50).use();
        Jt.text(currentValue > 50 ? "Vous êtes vieux" : "Vous êtes fringuant").use();
        //System.out.println("Slider value: " + value);
    }
}
