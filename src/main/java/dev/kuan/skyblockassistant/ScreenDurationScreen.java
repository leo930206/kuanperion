package dev.kuan.skyblockassistant;

import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Enabling screen alerts requires confirming a positive integer duration. */
final class ScreenDurationScreen extends Screen {
    private final Screen parent;
    private final IntConsumer confirmSeconds;
    private String draft;

    ScreenDurationScreen(Screen parent, int initialSeconds, IntConsumer confirmSeconds) {
        super(Component.literal("螢幕通知顯示時間"));
        this.parent = parent;
        this.draft = Integer.toString(initialSeconds);
        this.confirmSeconds = confirmSeconds;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 45;
        addRenderableWidget(new StringWidget(centerX - 140, y, 280, 12,
                Component.literal("請輸入顯示秒數（至少 1 秒，正整數）"), this.font));
        EditBox secondsBox = new EditBox(this.font, centerX - 60, y + 24, 120, 20,
                Component.literal("顯示秒數"));
        secondsBox.setMaxLength(32);
        secondsBox.setValue(draft);
        addRenderableWidget(secondsBox);
        Button confirm = Button.builder(Component.literal("確認"), b -> {
            int seconds = NotificationChannels.parseScreenSeconds(secondsBox.getValue());
            if (seconds != 0) confirmSeconds.accept(seconds);
        }).bounds(centerX - 105, y + 60, 100, 20).build();
        confirm.active = NotificationChannels.parseScreenSeconds(secondsBox.getValue()) != 0;
        addRenderableWidget(confirm);
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(centerX + 5, y + 60, 100, 20).build());
        secondsBox.setResponder(value -> {
            draft = value;
            boolean valid = NotificationChannels.parseScreenSeconds(value) != 0;
            confirm.active = valid;
            secondsBox.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
        });
        setInitialFocus(secondsBox);
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
