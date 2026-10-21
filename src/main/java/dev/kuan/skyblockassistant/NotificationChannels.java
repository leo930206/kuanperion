package dev.kuan.skyblockassistant;

/** Per-event switches/duration; the Webhook URL is deliberately shared elsewhere. */
record NotificationChannels(boolean discord, boolean chat, boolean screen, int screenSeconds) {
    static final int DEFAULT_SCREEN_SECONDS = 3;
    static final NotificationChannels NONE = new NotificationChannels(false, false, false);

    NotificationChannels(boolean discord, boolean chat, boolean screen) {
        this(discord, chat, screen, DEFAULT_SCREEN_SECONDS);
    }

    NotificationChannels {
        if (screenSeconds < 1) throw new IllegalArgumentException("Screen seconds must be positive");
    }

    static int parseScreenSeconds(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) return 0;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }

    boolean available(String webhook) { return chat || screen || (discord && !webhook.isBlank()); }
}
