package dev.kuan.skyblockassistant;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** One feature owns its switches; only the live Webhook supplier is shared. */
final class NotificationSettingsScreen extends Screen {
    record Options(NotificationChannels channels, int threshold, boolean stopActions) {}
    private final Screen parent;
    private final NotificationType type;
    private final Supplier<String> getWebhook;
    private final Consumer<String> setWebhook;
    private final Consumer<Options> saveOptions;
    private boolean discord, chat, screen, stopActions;
    private int threshold;
    private BigDecimal screenSeconds;

    NotificationSettingsScreen(Screen parent, NotificationType type, Options initial,
                               Supplier<String> getWebhook, Consumer<String> setWebhook, Consumer<Options> saveOptions) {
        super(Component.literal(type.label));
        this.parent = parent;
        this.type = type;
        this.getWebhook = getWebhook;
        this.setWebhook = setWebhook;
        this.saveOptions = saveOptions;
        discord = initial.channels().discord();
        chat = initial.channels().chat();
        screen = initial.channels().screen();
        screenSeconds = initial.channels().screenSeconds();
        threshold = initial.threshold();
        stopActions = initial.stopActions();
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 140;
        int y = this.height / 2 - 86;
        addRenderableWidget(new StringWidget(x, y, 280, 12, Component.literal(type.label), this.font));
        y += 24;
        if (type != NotificationType.JERRY) {
            addRenderableWidget(Button.builder(Component.literal("數量門檻：" + threshold), b -> openThreshold())
                    .bounds(x, y, 280, 20).build());
            y += 28;
        }
        addRenderableWidget(Button.builder(toggleMsg("Discord 訊息通知", discord), b -> {
            if (discord) { discord = false; saveAndRefresh(); }
            else Minecraft.getInstance().setScreenAndShow(new WebhookSettingsScreen(this, getWebhook.get(), value -> {
                setWebhook.accept(value);
                discord = true;
                saveAndRefresh();
            }));
        }).bounds(x, y, 280, 20).build());
        y += 28;
        addRenderableWidget(Button.builder(toggleMsg("遊戲內聊天室通知", chat), b -> {
            chat = !chat; saveAndRefresh();
        }).bounds(x, y, 280, 20).build());
        y += 28;
        addRenderableWidget(Button.builder(toggleMsg("遊戲內螢幕通知" + (screen ? "（" + screenSeconds.toPlainString() + " 秒）" : ""), screen), b -> {
            if (screen) { screen = false; saveAndRefresh(); }
            else Minecraft.getInstance().setScreenAndShow(new ScreenDurationScreen(this, screenSeconds, seconds -> {
                screenSeconds = seconds;
                screen = true;
                saveAndRefresh();
            }));
        }).bounds(x, y, 280, 20).build());
        y += 28;
        if (type == NotificationType.JERRY) {
            addRenderableWidget(Button.builder(toggleMsg("出現 Jerry 是否停下動作", stopActions), b -> {
                stopActions = !stopActions; saveAndRefresh();
            }).bounds(x, y, 280, 20).build());
            y += 28;
        }
        addRenderableWidget(Button.builder(Component.literal("完成"), b -> onClose())
                .bounds(this.width / 2 - 65, y + 8, 130, 20).build());
    }

    private void openThreshold() {
        IntConsumer confirm = value -> {
            threshold = value;
            saveAndRefresh();
        };
        Minecraft.getInstance().setScreenAndShow(type == NotificationType.PESTS
                ? new PestThresholdScreen(this, confirm) : new VisitorThresholdScreen(this, confirm));
    }

    private void saveAndRefresh() {
        saveOptions.accept(new Options(new NotificationChannels(discord, chat, screen, screenSeconds), threshold, stopActions));
        Minecraft.getInstance().setScreenAndShow(this);
    }

    private Component toggleMsg(String label, boolean on) {
        return Component.literal(label + "：").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(on ? "開啟" : "關閉").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
