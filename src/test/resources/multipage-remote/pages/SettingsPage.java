/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
