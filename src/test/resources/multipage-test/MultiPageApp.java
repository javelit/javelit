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

public class MultiPageApp {
    public static void main(String[] args) {
        // Navigation with multiple pages
        final JtPage currentPage = Jt.navigation(
            Jt.page(SettingsPage.class).title("Settings").icon("⚙️").urlPath("/config/settings"),
            Jt.page(HomePage.class).title("Home").icon("🏠").home(),
            Jt.page(AboutPage.class).title("About").icon("ℹ️")
        ).use();

        currentPage.run();
        
        // Persistent footer element that should appear on all pages
        Jt.text("© 2025 Test App - Always Visible").use();
    }
}
