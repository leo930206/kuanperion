package dev.kuan.skyblockassistant;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Channels are independent; enabling Discord requires confirming its Webhook. */
final class NotificationSettingsScreen extends Screen {
    record Options(boolean discord, boolean chat, boolean screen, String webhook) {}
    private final Screen parent;
    private final Consumer<Options> saveOptions;
    private boolean discord;
    private boolean chat;
    private boolean screen;
    private String webhook;

    NotificationSettingsScreen(Screen parent, boolean discord, boolean chat, boolean screen,
                               String webhook, Consumer<Options> saveOptions) {
        super(Component.literal("通知設定"));
        this.parent = parent;
        this.discord = discord;
        this.chat = chat;
        this.screen = screen;
        this.webhook = webhook;
        this.saveOptions = saveOptions;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 140;
        int y = this.height / 2 - 78;
        addRenderableWidget(new StringWidget(x, y, 280, 12, Component.literal("通知設定"), this.font));
        addRenderableWidget(Button.builder(toggleMsg("Discord 訊息通知", discord), b -> {
            if (discord) {
                discord = false;
                saveAndRefresh();
            } else {
                Minecraft.getInstance().setScreenAndShow(new WebhookSettingsScreen(this, webhook, value -> {
                    webhook = value;
                    discord = true;
                    saveAndRefresh();
                }));
            }
        }).bounds(x, y + 24, 280, 20).build());
        addRenderableWidget(Button.builder(toggleMsg("遊戲聊天室通知", chat), b -> {
            chat = !chat;
            saveAndRefresh();
        }).bounds(x, y + 52, 280, 20).build());
        addRenderableWidget(Button.builder(toggleMsg("遊戲螢幕通知", screen), b -> {
            screen = !screen;
            saveAndRefresh();
        }).bounds(x, y + 80, 280, 20).build());
        addRenderableWidget(Button.builder(Component.literal("完成"), b -> onClose())
                .bounds(this.width / 2 - 65, y + 116, 130, 20).build());
    }

    private void saveAndRefresh() {
        saveOptions.accept(new Options(discord, chat, screen, webhook));
        Minecraft.getInstance().setScreenAndShow(this);
    }

    private Component toggleMsg(String label, boolean enabled) {
        return Component.literal(label + "：").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(enabled ? "開啟" : "關閉")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
