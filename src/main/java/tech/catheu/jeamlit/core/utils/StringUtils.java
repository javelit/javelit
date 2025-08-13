package tech.catheu.jeamlit.core.utils;

import jakarta.annotation.Nonnull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class StringUtils {

    public static String percentEncode(final @Nonnull String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8).replace("+", "%20");
    }

}
