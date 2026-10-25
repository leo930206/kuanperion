// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.mixin;
import dev.kuan.kuanperion.KuanperionClient;import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import org.spongepowered.asm.mixin.injection.Inject;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Vanilla input entry points, not direct game-mode task actions; a fast released click must still win. */
@Mixin(Minecraft.class)
public abstract class AutomationWorldInputMixin {
 @Inject(method="startAttack",at=@At("HEAD"))
 private void kuanperion$manualWorldAttack(CallbackInfoReturnable<Boolean> ci){KuanperionClient.onManualAutomationWorldInput();}
 @Inject(method="startUseItem",at=@At("HEAD"))
 private void kuanperion$manualWorldUse(CallbackInfo ci){KuanperionClient.onManualAutomationWorldInput();}
}
