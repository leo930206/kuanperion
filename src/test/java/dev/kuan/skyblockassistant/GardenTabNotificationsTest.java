package dev.kuan.skyblockassistant;

import java.util.List;

public final class GardenTabNotificationsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 8; i++)
            check(GardenTabNotifications.parseThreshold(Integer.toString(i)) == i, "valid threshold " + i);
        for (String value : new String[]{"", "0", "9", "10", "12", "01", "-1", "1.0", " 1", "1 ", "a", "１", "1\n"})
            check(GardenTabNotifications.parseThreshold(value) == 0, "reject threshold: " + value);
        check(GardenTabNotifications.parseThreshold(null) == 0, "reject null");

        var counts = GardenTabNotifications.parse(List.of(
                "Jacob's Contest:", "Starts in: 5m 44s", "§cPests:",
                " §bAlive: §f4", " Plots: 8", " Spray: None", " Repellent: None",
                " Bonus: INACTIVE", " Cooldown: 2m 15s", "Pest Traps: 0/3",
                " Full Traps: None", " No Bait: None", "§bVisitors: §f(5)"));
        check(counts.pests() == 4 && counts.visitors() == 5, "screenshot counts, not Plots or cooldown");
        counts = GardenTabNotifications.parse(List.of("Pests:\n Alive: x12", "Visitors: (15)"));
        check(counts.pests() == 12 && counts.visitors() == 15, "multiline components and multi-digit counts");
        counts = GardenTabNotifications.parse(List.of("\u200bPESTS:\u00a0", " Alive:\u00a0× 8", "VISITORS: (0)"));
        check(counts.pests() == 8 && counts.visitors() == 0, "formatting and Unicode spacing");
        counts = GardenTabNotifications.parse(List.of("Alive: 7", "Visitors: (6)"));
        check(counts.pests() == -1 && counts.visitors() == 6, "Alive needs Pests section");
        counts = GardenTabNotifications.parse(List.of("Pests:", "Plots: 8", "Other:", "Alive: 6"));
        check(counts.pests() == -1, "do not read Alive under another section");
        counts = GardenTabNotifications.parse(List.of("Pests:", "Alive: 999999999999", "Visitors: (x)", "player Visitors: (5)"));
        check(counts.pests() == -1 && counts.visitors() == -1, "malformed and overflowing counts stay unknown");
        counts = GardenTabNotifications.parse(List.of());
        check(counts.pests() == -1 && counts.visitors() == -1, "empty Tab is unknown, not zero");

        var tracker = new GardenTabNotifications();
        check(!tracker.pestReached(true, 3, 4), "below pest threshold");
        for (int count = 4; count <= 8; count++) {
            check(tracker.pestReached(true, count, 4), "each qualifying pest count notifies once: " + count);
            check(!tracker.pestReached(true, count, 4), "sustained count stays silent: " + count);
        }
        for (int count = 8; count >= 4; count--)
            check(!tracker.pestReached(true, count, 4), "revisiting a notified count stays silent: " + count);
        check(!tracker.pestReached(true, -1, 4), "missing pests do not notify");
        check(!tracker.pestReached(true, 5, 4), "missing Tab does not clear notified counts");
        check(tracker.pestReached(true, 12, 4), "pest counts above eight still qualify");
        check(!tracker.pestReached(true, 12, 4), "multi-digit counts also notify once");
        check(!tracker.pestReached(true, 0, 4), "zero rearms pests");
        for (int count = 4; count <= 8; count++)
            check(tracker.pestReached(true, count, 4), "below threshold rearms every count: " + count);
        check(!tracker.pestReached(false, 7, 4), "disabled pests stay silent");
        check(tracker.pestReached(true, 7, 4), "re-enabled pests notify once");
        tracker.resetPests();
        check(tracker.pestReached(true, 7, 4), "new threshold selection rearms pests");
        check(!tracker.pestReached(true, 0, 4), "prepare below threshold");
        check(!tracker.pestReached(true, 9, 9), "invalid threshold never sends");

        for (int i = 1; i <= 5; i++)
            check(GardenTabNotifications.parseVisitorThreshold(Integer.toString(i)) == i, "valid visitor threshold " + i);
        for (String value : new String[]{"", "0", "6", "8", "10", "01", "-1", "1.0", " 1", "1 ", "a", "１", "1\n"})
            check(GardenTabNotifications.parseVisitorThreshold(value) == 0, "reject visitor threshold " + value);
        check(GardenTabNotifications.parseVisitorThreshold(null) == 0, "reject null visitor threshold");
        for (int threshold = 1; threshold <= 5; threshold++) {
            var visitorTracker = new GardenTabNotifications();
            check(!visitorTracker.visitorsReached(true, threshold - 1, threshold), "below visitor threshold");
            for (int count = threshold; count <= 5; count++) {
                check(visitorTracker.visitorsReached(true, count, threshold), "each visitor count notifies: " + count);
                check(!visitorTracker.visitorsReached(true, count, threshold), "sustained visitor count silent");
            }
            for (int count = 5; count >= threshold; count--)
                check(!visitorTracker.visitorsReached(true, count, threshold), "revisited visitor count silent");
            check(!visitorTracker.visitorsReached(true, -1, threshold), "missing visitors preserve history");
            check(!visitorTracker.visitorsReached(true, 6, threshold), "invalid count preserves history");
            check(!visitorTracker.visitorsReached(true, threshold, threshold), "unknown counts cannot rearm");
            check(!visitorTracker.visitorsReached(true, threshold - 1, threshold), "below threshold resets visitors");
            check(visitorTracker.visitorsReached(true, 5, threshold), "jump to five notifies only observed five");
            if (threshold < 5)
                check(visitorTracker.visitorsReached(true, threshold, threshold), "skipped-over counts not invented");
            check(!visitorTracker.visitorsReached(false, 5, threshold), "disabled visitors silent");
            check(visitorTracker.visitorsReached(true, 5, threshold), "re-enabling rearms visitors");
            visitorTracker.resetVisitors();
            check(visitorTracker.visitorsReached(true, 5, threshold), "explicit reset rearms visitors");
        }
        check(!tracker.visitorsReached(true, 5, 0) && !tracker.visitorsReached(true, 5, 6), "invalid visitor thresholds silent");
        tracker.resetVisitors();
        check(tracker.visitorsReached(true, 3, 3), "three visitor notification");
        check(tracker.pestReached(true, 4, 4), "visitor state does not suppress pests");
        tracker.resetPests();
        check(!tracker.visitorsReached(true, 3, 3), "pest reset does not rearm visitors");
        for (int threshold = 1; threshold <= 8; threshold++) {
            var perThreshold = new GardenTabNotifications();
            check(perThreshold.pestReached(true, threshold + 2, threshold), "jump directly to observed count");
            check(perThreshold.pestReached(true, threshold, threshold), "new lower qualifying count still notifies");
            check(!perThreshold.pestReached(true, threshold + 2, threshold), "jumping back does not notify twice");
        }
        System.out.println("GardenTabNotificationsTest passed (per-count pest and visitor thresholds, independent histories)");
    }
}
