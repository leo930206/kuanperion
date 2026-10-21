package dev.kuan.skyblockassistant;

import java.util.regex.Pattern;

/** Match any of the seven signals inside color-stripped chat, regardless of surrounding text. */
final class JerryMessageMatcher {
    private static final String ANY_TYPE = "[^.!?\\r\\n]*?";
    private static final Pattern[] SIGNALS = {
            signal("you\\s+found\\s+a\\s+" + ANY_TYPE + "\\bjerry\\b"),
            signal("you\\s+discovered\\s+a\\s+" + ANY_TYPE + "\\bjerry\\b"),
            signal("some\\s+" + ANY_TYPE + "\\bjerry\\s+was\\s+hiding\\s*,\\s*but\\s+you\\s+found\\s+it\\b"),
            signal("wild\\s+" + ANY_TYPE + "\\bjerry\\s+spawned\\b"),
            signal("\\bjerry\\s+appeared\\b"),
            signal("there\\s+is\\s+a\\s+" + ANY_TYPE + "\\bjerry\\b"),
            signal("you\\s+located\\s+a\\s+hidden\\s+" + ANY_TYPE + "\\bjerry\\b")
    };

    private static Pattern signal(String expression) {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    }

    static boolean matches(String text) {
        if (text == null) return false;
        for (Pattern signal : SIGNALS) {
            if (signal.matcher(text).find()) return true;
        }
        return false;
    }
}
