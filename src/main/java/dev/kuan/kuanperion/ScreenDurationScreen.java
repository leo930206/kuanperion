package dev.kuan.kuanperion;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Confirm a positive duration; the caller owns its validation and input length policy. */
final class ScreenDurationScreen extends Screen {
    private final Screen parent;
    private final Consumer<BigDecimal> confirmSeconds;
    private final Function<String, BigDecimal> parseSeconds;
    private final int maxLength;
    private String draft;

    ScreenDurationScreen(Screen parent, BigDecimal initialSeconds, Consumer<BigDecimal> confirmSeconds) {
        this(parent, "螢幕通知顯示時間", initialSeconds, NotificationChannels::parseScreenSeconds, 32, confirmSeconds);
    }

    ScreenDurationScreen(Screen parent, String title, BigDecimal initialSeconds,
                         Function<String, BigDecimal> parseSeconds, int maxLength, Consumer<BigDecimal> confirmSeconds) {
        super(Component.literal(title));
        this.parseSeconds = parseSeconds;
        this.maxLength = maxLength;
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
        secondsBox.setMaxLength(maxLength);
        secondsBox.setValue(draft);
        addRenderableWidget(secondsBox);
        Button confirm = Button.builder(Component.literal("確認"), b -> {
            BigDecimal seconds = parseSeconds.apply(secondsBox.getValue());
            if (seconds != null) confirmSeconds.accept(seconds);
        }).bounds(centerX - 105, y + 60, 100, 20).build();
        confirm.active = parseSeconds.apply(secondsBox.getValue()) != null;
        addRenderableWidget(confirm);
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(centerX + 5, y + 60, 100, 20).build());
        secondsBox.setResponder(value -> {
            draft = value;
            boolean valid = parseSeconds.apply(value) != null;
            confirm.active = valid;
            secondsBox.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
        });
        setInitialFocus(secondsBox);
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
