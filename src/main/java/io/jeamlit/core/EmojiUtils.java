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
package io.jeamlit.core;

import net.fellbaum.jemoji.EmojiManager;

public final class EmojiUtils {

    static void ensureIsValidIcon(@org.jetbrains.annotations.Nullable String icon) {
        if (icon != null && !icon.isEmpty()) {
            // Validate icon format: single emoji or :icon_name:
            boolean isEmoji = EmojiManager.isEmoji(icon);
            boolean isMaterialIcon = icon.startsWith(":") && icon.endsWith(":") && icon.length() > 2;

            if (!isEmoji && !isMaterialIcon) {
                throw new IllegalArgumentException(
                        "icon must be a single emoji or Material Symbols in format ':icon_name:'. Got: " + icon);
            }
        }
    }
}
