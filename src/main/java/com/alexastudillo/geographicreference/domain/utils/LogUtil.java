package com.alexastudillo.geographicreference.domain.utils;

/**
 * Applies the company-standard location prefix to log messages.
 */
public class LogUtil {
    private LogUtil() {
        /* This utility class should not be instantiated */
    }

    public static String log(final String location, final String message, final Object... args) {
        final StringBuilder builder = new StringBuilder();
        builder.append("[").append(location).append("] ");
        builder.append(String.format(message, args));
        return builder.toString().trim();
    }
}
