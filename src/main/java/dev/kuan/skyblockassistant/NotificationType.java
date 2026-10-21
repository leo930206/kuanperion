package dev.kuan.skyblockassistant;

enum NotificationType {
    PESTS("pestNotification", "害蟲通知"),
    VISITORS("visitorNotification", "Visitor 通知"),
    JERRY("jerryNotification", "Jerry 偵測");

    final String configKey;
    final String label;
    NotificationType(String configKey, String label) { this.configKey = configKey; this.label = label; }
}
