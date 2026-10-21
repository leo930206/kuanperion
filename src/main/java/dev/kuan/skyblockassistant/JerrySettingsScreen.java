package dev.kuan.skyblockassistant;

import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Draft options are applied only when confirmed; Cancel/Esc leaves detection off. */
final class JerrySettingsScreen extends Screen {
    private final Screen parent;
    private final BiConsumer<Boolean, Boolean> confirmOptions;
    private boolean notifyEnabled;
    private boolean stopActions;

    JerrySettingsScreen(Screen parent, boolean notifyEnabled, boolean stopActions,
                        BiConsumer<Boolean, Boolean> confirmOptions) {
        super(Component.literal("Jerry 偵測設定"));
        this.parent = parent;
        this.notifyEnabled = notifyEnabled;
        this.stopActions = stopActions;
        this.confirmOptions = confirmOptions;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 58;
        addRenderableWidget(new StringWidget(centerX - 160, y, 320, 12,
                Component.literal("Jerry 偵測設定"), this.font));
        addRenderableWidget(Button.builder(toggleMsg("出現 Jerry 是否通知", notifyEnabled), b -> {
            notifyEnabled = !notifyEnabled;
            b.setMessage(toggleMsg("出現 Jerry 是否通知", notifyEnabled));
        }).bounds(centerX - 160, y + 24, 320, 20).build());
        addRenderableWidget(Button.builder(toggleMsg("出現 Jerry 是否停下動作", stopActions), b -> {
            stopActions = !stopActions;
            b.setMessage(toggleMsg("出現 Jerry 是否停下動作", stopActions));
        }).bounds(centerX - 160, y + 52, 320, 20).build());
        addRenderableWidget(Button.builder(Component.literal("確認"), b ->
                confirmOptions.accept(notifyEnabled, stopActions))
                .bounds(centerX - 105, y + 84, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(centerX + 5, y + 84, 100, 20).build());
    }

    private Component toggleMsg(String label, boolean enabled) {
        return Component.literal(label + "：").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(enabled ? "開啟" : "關閉")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
