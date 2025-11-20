package pages;

import io.javelit.core.Jt;

public class SettingsPage {
  public static void app() {
    Jt.title("⚙️ Settings").use();
    Jt.text("Configure your application settings.").use();

    // Settings options
    Double fontSize = Jt.slider("Font Size")
                        .min(10.0)
                        .max(20.0)
                        .step(1.0)
                        .use();

    String theme = Jt.textInput("Theme")
                     .placeholder("light or dark")
                     .use();

    if (Jt.button("Save Settings").use()) {
      Jt.text("Settings saved!").use();
      if (fontSize != null) {
        Jt.text("Font Size: " + fontSize.intValue() + "px").use();
      }
      if (!theme.isEmpty()) {
        Jt.text("Theme: " + theme).use();
      }
    }
  }

  private SettingsPage() {
  }
}
