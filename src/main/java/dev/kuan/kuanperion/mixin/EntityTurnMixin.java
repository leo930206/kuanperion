package dev.kuan.kuanperion.mixin;

import dev.kuan.kuanperion.KuanperionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void kuanHsba$lockManualLook(double yawDelta, double pitchDelta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if ((Object) this == mc.player) {
            if(yawDelta!=0||pitchDelta!=0)KuanperionClient.onManualAutomationLook();
            if(KuanperionClient.shouldLockManualLook())ci.cancel();
        }
    }
}
