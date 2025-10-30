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



import io.javelit.core.Jt;import io.javelit.core.JtPage;

public class HiddenNavApp {
    public static void main(String[] args) {
        // Navigation with hidden() - no sidebar should appear
        final JtPage currentPage = Jt.navigation(
                Jt.page("/home", HomePage::app).icon("🏠").home(),
            Jt.page("/settings", SettingsPage::app).title("Settings Page").icon("⚙️"),
            Jt.page("/about", AboutPage::app).title("About").icon("ℹ️")

        ).hidden().use();  // Call hidden() to hide navigation

        currentPage.run();
        
        // Content that should still be visible
        Jt.text("App with hidden navigation").use();
    }
}
