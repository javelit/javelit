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
package tech.catheu.jeamlit.e2e.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsUtils.class);

    public enum OS {
        WINDOWS("Control"),
        LINUX("Control"),
        MAC("Meta");

        public final String modifier;

        OS(final String modifier) {
            this.modifier = modifier;
        }
    }

    public static OS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return OS.MAC;
        } else if (os.contains("win")) {
            return OS.WINDOWS;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return OS.LINUX;
        }
        LOGGER.warn("Unrecognized OS: " + os);
        return OS.LINUX;
    }
}
