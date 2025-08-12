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
