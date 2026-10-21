package dev.kuan.skyblockassistant;

import java.util.ArrayDeque;
import java.util.Deque;

/** Independent event labels share a sequential, monotonic three-second display. */
final class ScreenNotificationQueue {
    private static final long DISPLAY_NANOS = 3_000_000_000L;
    private record Entry(NotificationType type, String text) {}
    private final Deque<Entry> pending = new ArrayDeque<>();
    private Entry current;
    private long shownAt;

    void add(NotificationType type, String text, long now) {
        Entry entry = new Entry(type, text);
        if (current == null) { current = entry; shownAt = now; }
        else pending.addLast(entry);
    }

    String current(long now) {
        if (current != null && now - shownAt >= DISPLAY_NANOS) {
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
