package pages;

import java.time.LocalTime;

import io.jeamlit.core.Jt;

public class DashboardPage {
    public static void main(String[] args) {
        Jt.title("📊 Dashboard").use();
        Jt.text("Welcome to the dashboard! This is the home page.").use();
        
        // Add some dashboard content
        if (Jt.button("Refresh Data").use()) {
            Jt.text("Data refreshed at: " + LocalTime.now()).use();
        }
    }
}
