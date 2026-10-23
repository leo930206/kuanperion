package dev.kuan.kuanperion.mixin;

import dev.kuan.kuanperion.KuanperionClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blocks vanilla attack input; automatic Mining uses direct game-mode calls. */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void kuanHsba$blockManualAttack(CallbackInfoReturnable<Boolean> cir) {
        if (KuanperionClient.shouldLockManualAttack()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void kuanHsba$blockManualHeldAttack(boolean attacking, CallbackInfo ci) {
        // Cancel even the release path: vanilla must not stop the automated target.
        if (KuanperionClient.shouldLockManualAttack()) {
            ci.cancel();
        }
    }
}
