package dev.kuan.kuanperion;

import java.util.Set;
import java.util.regex.Pattern;

/** Match the enabled color on the actual Jerry signal, not unrelated prefix/suffix text. */
final class JerryMessageMatcher {
    private static final String JERRY = "(?<color>green|blue|purple|golden)\\s+jerry\\b";
    private static final Pattern[] SIGNALS = {
            signal("you\\s+found\\s+a\\s+" + JERRY),
            signal("you\\s+discovered\\s+a\\s+" + JERRY),
            signal("some\\s+" + JERRY + "\\s+was\\s+hiding\\s*,\\s*but\\s+you\\s+found\\s+it\\b"),
            signal("wild\\s+" + JERRY + "\\s+spawned\\b"),
            signal(JERRY + "\\s+appeared\\b"),
            signal("there\\s+is\\s+a\\s+" + JERRY),
            signal("you\\s+located\\s+a\\s+hidden\\s+" + JERRY)
    };

    private static Pattern signal(String expression) {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    }

    static JerryColor matchingColor(String text, Set<JerryColor> enabledColors) {
        if (text == null || enabledColors.isEmpty()) return null;
        for (Pattern signal : SIGNALS) {
            var matches = signal.matcher(text);
            while (matches.find()) {
                JerryColor color = JerryColor.fromName(matches.group("color"));
                if (enabledColors.contains(color)) return color;
            }
        }
        return null;
    }
}
