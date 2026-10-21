package dev.kuan.skyblockassistant;

import java.net.URI;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Confirming a valid URL enables Discord; Cancel/Esc leaves its state unchanged. */
final class WebhookSettingsScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> confirmUrl;
    private String draft;

    WebhookSettingsScreen(Screen parent, String initialUrl, Consumer<String> confirmUrl) {
        super(Component.literal("Discord Webhook"));
        this.parent = parent;
        this.draft = initialUrl == null ? "" : initialUrl;
        this.confirmUrl = confirmUrl;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 45;
        addRenderableWidget(new StringWidget(centerX - 160, y, 320, 12,
                Component.literal("共用 Discord Webhook 網址"), this.font));
        EditBox webhookBox = new EditBox(this.font, centerX - 160, y + 24, 320, 20,
                Component.literal("Discord Webhook"));
        webhookBox.setMaxLength(300);
        webhookBox.setValue(draft);
        addRenderableWidget(webhookBox);
        Button confirm = Button.builder(Component.literal("確認"), b -> {
            if (validWebhookUrl(webhookBox.getValue())) confirmUrl.accept(webhookBox.getValue().trim());
        }).bounds(centerX - 105, y + 60, 100, 20).build();
        confirm.active = validWebhookUrl(webhookBox.getValue());
        addRenderableWidget(confirm);
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(centerX + 5, y + 60, 100, 20).build());
        webhookBox.setResponder(value -> {
            draft = value;
            boolean valid = validWebhookUrl(value);
            confirm.active = valid;
            webhookBox.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
        });
        setInitialFocus(webhookBox);
    }

    private static boolean validWebhookUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
