// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.mixin;
import dev.kuan.kuanperion.AutomationMenuActions;
import dev.kuan.kuanperion.KuanperionClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Escape/inventory/other-mod GUI close revokes ownership before vanilla closes. Owned controllers close LocalPlayer directly. */
@Mixin(AbstractContainerScreen.class)
public abstract class AutomationContainerCloseMixin {
 @Inject(method="onClose",at=@At("HEAD"))
 private void kuanperion$close(CallbackInfo ci){if(!AutomationMenuActions.ownsInventoryInput())KuanperionClient.onManualAutomationInventoryInput();}
}
