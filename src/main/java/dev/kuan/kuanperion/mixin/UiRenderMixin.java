package dev.kuan.kuanperion.mixin;
import dev.kuan.kuanperion.renderer.UiRenderQueue;
import dev.kuan.kuanperion.ui.MainGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Draw after deferred Minecraft GUI rendering, never during extraction. */
@Mixin(GameRenderer.class)
public class UiRenderMixin {
    @Inject(method="render", at=@At("TAIL"))
    private void kuanperion$renderUi(DeltaTracker delta, boolean tick, CallbackInfo ci) {
        UiRenderQueue.flush();
        if (Minecraft.getInstance().gui.screen() instanceof MainGUI ui) ui.renderAfterGameRenderer(delta.getGameTimeDeltaTicks());
    }
}
