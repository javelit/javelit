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

import io.jeamlit.core.Jt;

public class TestQueryPage {
    public static void main(String[] args) {
        final var l = Jt.urlQueryParameters().get("name");
        var name = "unknown visitor";
        if (l != null && !l.isEmpty()) {
            name = l.getFirst();
        }

        Jt.title("🔍 Query Parameter Test").use();
        Jt.text("Hello %s!".formatted(name)).use();
    }
}
