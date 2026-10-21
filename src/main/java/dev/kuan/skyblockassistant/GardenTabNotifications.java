package dev.kuan.skyblockassistant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Parses Tab text; deduplicates pest and visitor counts independently. */
final class GardenTabNotifications {
    private static final Pattern COLORS = Pattern.compile("§[0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);
    private static final Pattern PESTS = Pattern.compile("pests\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALIVE = Pattern.compile("alive\\s*:\\s*(?:[x×]\\s*)?([0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PEST_FIELD = Pattern.compile("(?:plots|spray|repellent|bonus|cooldown)\\s*:.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern VISITORS = Pattern.compile("visitors?\\s*:\\s*(?:\\(\\s*([0-9]+)\\s*\\)|([0-9]+))", Pattern.CASE_INSENSITIVE);
    private final Set<Integer> notifiedPestCounts = new HashSet<>();
    private final Set<Integer> notifiedVisitorCounts = new HashSet<>();

    record Counts(int pests, int visitors) {}

    /** Zero means invalid; never trim, clamp, or truncate invalid input into a valid threshold. */
    static int parseThreshold(String value) {
        return value != null && value.length() == 1 && value.charAt(0) >= '1' && value.charAt(0) <= '8'
                ? value.charAt(0) - '0' : 0;
    }

    static int parseVisitorThreshold(String value) {
        int threshold = parseThreshold(value);
        return threshold <= 5 ? threshold : 0;
    }

    static Counts parse(List<String> tabLines) {
        int pests = -1;
        int visitors = -1;
        boolean inPests = false;
        for (String entry : tabLines) {
            if (entry == null) continue;
            for (String raw : entry.split("\\R")) {
                String line = COLORS.matcher(raw).replaceAll("")
                        .replaceAll("[\\p{Zs}]", " ").replaceAll("\\p{Cf}", "").strip();
                if (line.isEmpty()) continue;
                var visitor = VISITORS.matcher(line);
                if (visitor.matches()) visitors = count(visitor.group(1) != null ? visitor.group(1) : visitor.group(2));
                if (PESTS.matcher(line).matches()) {
                    inPests = true;
                } else {
                    var alive = ALIVE.matcher(line);
                    if (inPests && alive.matches()) pests = count(alive.group(1));
                    else if (!PEST_FIELD.matcher(line).matches()) inPests = false;
                }
            }
        }
        return new Counts(pests, visitors);
    }

    private static int count(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return -1; }
    }

    boolean pestReached(boolean enabled, int count, int threshold) {
        if (!enabled) { resetPests(); return false; }
        if (count < 0 || threshold < 1 || threshold > 8) return false;
        if (count < threshold) { resetPests(); return false; }
        return notifiedPestCounts.add(count);
    }

    boolean visitorsReached(boolean enabled, int count, int threshold) {
        if (!enabled) { resetVisitors(); return false; }
        if (count < 0 || count > 5 || threshold < 1 || threshold > 5) return false;
        if (count < threshold) { resetVisitors(); return false; }
        return notifiedVisitorCounts.add(count);
    }

    void resetPests() { notifiedPestCounts.clear(); }
    void resetVisitors() { notifiedVisitorCounts.clear(); }
}
