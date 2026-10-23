package dev.kuan.kuanperion;

import java.util.ArrayDeque;
import java.math.BigDecimal;
import java.util.Deque;

/** Independent event labels share a sequential display with each event's chosen duration. */
final class ScreenNotificationQueue {
    private record Entry(NotificationType type, String text, long durationNanos) {}
    private final Deque<Entry> pending = new ArrayDeque<>();
    private Entry current;
    private long shownAt;

    void add(NotificationType type, String text, int seconds, long now) {
        add(type, text, BigDecimal.valueOf(seconds), now);
    }

    void add(NotificationType type, String text, BigDecimal seconds, long now) {
        Entry entry = new Entry(type, text, NotificationChannels.durationNanos(seconds));
        if (current == null) { current = entry; shownAt = now; }
        else pending.addLast(entry);
    }

    String current(long now) {
        if (current != null && now - shownAt >= current.durationNanos()) {
            current = pending.pollFirst();
            shownAt = now;
        }
        return current == null ? null : current.text();
    }

    void remove(NotificationType type, long now) {
        pending.removeIf(entry -> entry.type() == type);
        if (current != null && current.type() == type) { current = pending.pollFirst(); shownAt = now; }
    }

    void clear() { current = null; pending.clear(); }
}
