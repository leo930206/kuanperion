package dev.kuan.skyblockassistant;

import java.math.BigDecimal;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Enabling screen alerts requires confirming a positive decimal duration. */
final class ScreenDurationScreen extends Screen {
    private final Screen parent;
    private final Consumer<BigDecimal> confirmSeconds;
    private String draft;

    ScreenDurationScreen(Screen parent, BigDecimal initialSeconds, Consumer<BigDecimal> confirmSeconds) {
        super(Component.literal("螢幕通知顯示時間"));
        this.parent = parent;
        this.draft = initialSeconds.toPlainString();
        this.confirmSeconds = confirmSeconds;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 45;
        addRenderableWidget(new StringWidget(centerX - 140, y, 280, 12,
                Component.literal("請輸入秒數（大於 0，可含一個小數點）"), this.font));
        EditBox secondsBox = new EditBox(this.font, centerX - 60, y + 24, 120, 20,
                Component.literal("顯示秒數"));
        secondsBox.setMaxLength(32);
        secondsBox.setValue(draft);
        addRenderableWidget(secondsBox);
        Button confirm = Button.builder(Component.literal("確認"), b -> {
            BigDecimal seconds = NotificationChannels.parseScreenSeconds(secondsBox.getValue());
            if (seconds != null) confirmSeconds.accept(seconds);
        }).bounds(centerX - 105, y + 60, 100, 20).build();
        confirm.active = NotificationChannels.parseScreenSeconds(secondsBox.getValue()) != null;
        addRenderableWidget(confirm);
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(centerX + 5, y + 60, 100, 20).build());
        secondsBox.setResponder(value -> {
            draft = value;
            boolean valid = NotificationChannels.parseScreenSeconds(value) != null;
            confirm.active = valid;
            secondsBox.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
        });
        setInitialFocus(secondsBox);
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
