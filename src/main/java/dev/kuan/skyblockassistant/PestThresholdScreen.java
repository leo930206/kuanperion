package dev.kuan.skyblockassistant;

import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Confirmation is required before enabling pest notifications; cancellation changes nothing. */
final class PestThresholdScreen extends Screen {
    private final Screen parent;
    private final IntConsumer confirmThreshold;

    PestThresholdScreen(Screen parent, IntConsumer confirmThreshold) {
        super(Component.literal("害蟲通知門檻"));
        this.parent = parent;
        this.confirmThreshold = confirmThreshold;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 45;
        addRenderableWidget(new StringWidget(centerX - 120, y, 240, 12,
                Component.literal("請輸入害蟲數量門檻（1～8）"), this.font));
        EditBox countBox = new EditBox(this.font, centerX - 50, y + 20, 100, 20,
                Component.literal("害蟲數量門檻"));
        // Do not truncate a pasted multi-digit value to its first (apparently valid) digit.
        countBox.setMaxLength(32);
        addRenderableWidget(countBox);
        addRenderableWidget(new StringWidget(centerX - 140, y + 44, 280, 12,
                Component.literal("Tab 的 Pests → Alive 達到門檻時通知 Discord"), this.font));
        Button confirm = Button.builder(Component.literal("確認"), b -> {
            int threshold = GardenTabNotifications.parseThreshold(countBox.getValue());
            if (threshold != 0) confirmThreshold.accept(threshold);
        }).bounds(centerX - 105, y + 64, 100, 20).build();
        confirm.active = false;
        addRenderableWidget(confirm);
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(centerX + 5, y + 64, 100, 20).build());
        countBox.setResponder(value -> {
            boolean valid = GardenTabNotifications.parseThreshold(value) != 0;
            countBox.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
            confirm.active = valid;
        });
        setInitialFocus(countBox);
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
