package dev.kuan.skyblockassistant;

import java.math.BigDecimal;

public final class NotificationScreenDurationTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (String value : new String[]{"1", "3", "60", "2147483647", "0.5", "1.25", "3.0", "0.000000001", "1.2500000000"}) {
            BigDecimal seconds = NotificationChannels.parseScreenSeconds(value);
            check(seconds != null && seconds.compareTo(new BigDecimal(value)) == 0, "valid seconds: " + value);
        }
        for (String value : new String[]{"", "0", "0.0", "-1", "1..5", "1.2.3", ".", ".5", "1.", "a", " 3", "3 ", "03", "３", "NaN", "Infinity", "1e2", "2147483648", "0.0000000001", "999999999999999999999999"})
            check(NotificationChannels.parseScreenSeconds(value) == null, "invalid seconds: " + value);
        check(NotificationChannels.parseScreenSeconds(null) == null, "null is invalid");
        check(new NotificationChannels(true, false, true).screenSeconds().compareTo(BigDecimal.valueOf(3)) == 0, "old settings default to three seconds");
        ScreenNotificationQueue q = new ScreenNotificationQueue();
        q.add(NotificationType.JERRY, "short", new BigDecimal("0.5"), 0);
        q.add(NotificationType.PESTS, "long", new BigDecimal("1.25"), 0);
        check(q.current(499_999_999L).equals("short"), "first event holds half a second");
        check(q.current(500_000_000L).equals("long"), "queued event begins at exact fractional boundary");
        check(q.current(1_749_999_999L).equals("long") && q.current(1_750_000_000L) == null, "queued event keeps its own 1.25 seconds");
        q.add(NotificationType.JERRY, "tenth", new BigDecimal("0.1"), 0);
        check(q.current(99_999_999L).equals("tenth") && q.current(100_000_000L) == null, "decimal conversion is exact, not binary floating-point rounded");
        q.add(NotificationType.VISITORS, "max", Integer.MAX_VALUE, 0);
        long end = Integer.MAX_VALUE * 1_000_000_000L;
        check(q.current(end - 1).equals("max") && q.current(end) == null, "large accepted duration cannot overflow nanoseconds");
        boolean rejected = false;
        try { q.add(NotificationType.JERRY, "invalid", BigDecimal.ZERO, 0); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "queue rejects zero duration");
        System.out.println("NotificationScreenDurationTest: PASS (one decimal point, positive seconds, exact fractional timing, mixed durations, overflow)");
    }
}
