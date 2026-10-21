package dev.kuan.skyblockassistant;

import java.math.BigDecimal;

/** Per-event switches/duration; the Webhook URL is deliberately shared elsewhere. */
record NotificationChannels(boolean discord, boolean chat, boolean screen, BigDecimal screenSeconds) {
    static final BigDecimal DEFAULT_SCREEN_SECONDS = BigDecimal.valueOf(3);
    private static final BigDecimal MAX_SCREEN_SECONDS = BigDecimal.valueOf(Integer.MAX_VALUE);
    static final NotificationChannels NONE = new NotificationChannels(false, false, false);

    NotificationChannels(boolean discord, boolean chat, boolean screen) {
        this(discord, chat, screen, DEFAULT_SCREEN_SECONDS);
    }

    NotificationChannels(boolean discord, boolean chat, boolean screen, int seconds) {
        this(discord, chat, screen, BigDecimal.valueOf(seconds));
    }

    NotificationChannels {
        durationNanos(screenSeconds);
        screenSeconds = screenSeconds.stripTrailingZeros();
    }

    static BigDecimal parseScreenSeconds(String value) {
        if (value == null || !value.matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?")) return null;
        try {
            BigDecimal seconds = new BigDecimal(value);
            durationNanos(seconds);
            return seconds.stripTrailingZeros();
        } catch (IllegalArgumentException ignored) { return null; }
    }

    static long durationNanos(BigDecimal seconds) {
        if (seconds == null || seconds.signum() <= 0 || seconds.compareTo(MAX_SCREEN_SECONDS) > 0)
            throw new IllegalArgumentException("Screen duration is out of range");
        try { return seconds.movePointRight(9).longValueExact(); }
        catch (ArithmeticException error) { throw new IllegalArgumentException("Screen duration requires nanosecond precision"); }
    }

    boolean available(String webhook) { return chat || screen || (discord && !webhook.isBlank()); }
}
