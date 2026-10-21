package dev.kuan.skyblockassistant;

public final class NotificationScreenDurationTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (String value : new String[]{"1", "3", "5", "60", "3600", "2147483647"})
            check(NotificationChannels.parseScreenSeconds(value) == Integer.parseInt(value), "valid seconds: " + value);
        for (String value : new String[]{"", "0", "-1", "1.5", "a", " 3", "3 ", "03", "３", "2147483648", "999999999999999999999999"})
            check(NotificationChannels.parseScreenSeconds(value) == 0, "invalid seconds: " + value);
        check(NotificationChannels.parseScreenSeconds(null) == 0, "null is invalid");
        check(new NotificationChannels(true, false, true).screenSeconds() == 3, "old settings default to three seconds");
        long second = 1_000_000_000L;
        ScreenNotificationQueue q = new ScreenNotificationQueue();
        q.add(NotificationType.JERRY, "short", 2, 0);
        q.add(NotificationType.PESTS, "long", 7, 0);
        check(q.current(2 * second - 1).equals("short"), "first event holds its selected duration");
        check(q.current(2 * second).equals("long"), "next event begins at selected boundary");
        check(q.current(9 * second - 1).equals("long") && q.current(9 * second) == null, "queued event keeps its own seven seconds");
        q.add(NotificationType.VISITORS, "max", Integer.MAX_VALUE, 0);
        long end = Integer.MAX_VALUE * second;
        check(q.current(end - 1).equals("max") && q.current(end) == null, "large accepted duration cannot overflow nanoseconds");
        boolean rejected = false;
        try { q.add(NotificationType.JERRY, "invalid", 0, 0); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "queue rejects zero duration");
        System.out.println("NotificationScreenDurationTest: PASS (positive integer validation, defaults, mixed durations, exact boundaries, overflow)");
    }
}
