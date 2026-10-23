package dev.kuan.kuanperion.renderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
/** Screen lifecycle only. Rendering is deferred until Minecraft has drawn its GUI. */
public abstract class NVGScreen extends Screen {
    protected NVGScreen(String title) { super(Component.literal(title)); }
    @Override protected void init() { initNVG(); }
    protected void initNVG() {}
    @Override public boolean keyPressed(KeyEvent input) {
        if (input.key()==256) { onClose(); return true; }
        return super.keyPressed(input);
    }
    @Override public boolean isPauseScreen() { return false; }
}
