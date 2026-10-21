package dev.kuan.skyblockassistant;

/** Immutable switches for one event type; the Webhook URL is deliberately not stored here. */
record NotificationChannels(boolean discord, boolean chat, boolean screen) {
    static final NotificationChannels NONE = new NotificationChannels(false, false, false);
    boolean available(String webhook) { return chat || screen || (discord && !webhook.isBlank()); }
}
