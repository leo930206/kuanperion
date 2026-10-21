package dev.kuan.skyblockassistant;

import java.util.ArrayDeque;
import java.util.Deque;

/** Monotonic three-second notifications; simultaneous events are shown one after another. */
final class ScreenNotificationQueue {
    private static final long DISPLAY_NANOS = 3_000_000_000L;
    private final Deque<String> pending = new ArrayDeque<>();
    private String current;
    private long shownAt;

    void add(String text, long now) {
        if (current == null) {
            current = text;
            shownAt = now;
        } else {
            pending.addLast(text);
        }
    }

    String current(long now) {
        if (current != null && now - shownAt >= DISPLAY_NANOS) {
            current = pending.pollFirst();
            shownAt = now;
        }
        return current;
    }

    void clear() {
        current = null;
        pending.clear();
    }
}
