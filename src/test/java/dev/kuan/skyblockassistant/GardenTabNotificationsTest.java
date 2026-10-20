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
        check(tracker.pestReached(true, 4, 4), "pests equal threshold notify");
        check(!tracker.pestReached(true, 8, 4), "pests above threshold do not spam");
        check(!tracker.pestReached(true, -1, 4), "missing pests do not notify");
        check(!tracker.pestReached(true, 5, 4), "missing Tab does not rearm pests");
        check(!tracker.pestReached(true, 0, 4), "zero rearms pests");
        check(tracker.pestReached(true, 6, 4), "crossing threshold again notifies");
        check(!tracker.pestReached(false, 7, 4), "disabled pests stay silent");
        check(tracker.pestReached(true, 7, 4), "re-enabled pests notify once");
        tracker.resetPests();
        check(tracker.pestReached(true, 7, 4), "new threshold selection rearms pests");
        check(!tracker.pestReached(true, 0, 4), "prepare below threshold");
        check(!tracker.pestReached(true, 9, 9), "invalid threshold never sends");

        check(!tracker.visitorsFull(true, 4), "four visitors silent");
        check(tracker.visitorsFull(true, 5), "five visitors notify");
        check(!tracker.visitorsFull(true, 5), "five visitors do not spam");
        check(!tracker.visitorsFull(true, -1), "unknown visitors silent");
        check(!tracker.visitorsFull(true, 5), "missing Tab does not rearm visitors");
        check(!tracker.visitorsFull(true, 6), "only exactly five visitors");
        check(tracker.visitorsFull(true, 5), "return to five notifies");
        check(!tracker.visitorsFull(false, 5), "disabled visitors silent");
        check(tracker.visitorsFull(true, 5), "re-enable visitors rearms");
        tracker.resetVisitors();
        check(tracker.visitorsFull(true, 5), "explicit visitor reset");
        check(tracker.pestReached(true, 4, 4), "visitor state does not suppress pests");
        tracker.resetPests();
        check(!tracker.visitorsFull(true, 5), "pest reset does not rearm visitors");
        System.out.println("GardenTabNotificationsTest passed");
    }
}
