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

import tech.catheu.jeamlit.core.Jt;

public class HiddenNavApp {
    public static void main(String[] args) {
        // Navigation with hidden() - no sidebar should appear
        Jt.navigation(
            Jt.page(HomePage.class).title("Home").icon("🏠").home(),
            Jt.page(SettingsPage.class).title("Settings").icon("⚙️"),
            Jt.page(AboutPage.class).title("About").icon("ℹ️")
        ).hidden().use();  // Call hidden() to hide navigation
        
        // Content that should still be visible
        Jt.text("App with hidden navigation").use();
    }
}