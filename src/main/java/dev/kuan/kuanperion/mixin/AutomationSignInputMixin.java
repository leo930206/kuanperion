// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.mixin;
import dev.kuan.kuanperion.AutomationMenuActions;
import dev.kuan.kuanperion.KuanperionClient;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;import net.minecraft.client.input.CharacterEvent;import net.minecraft.client.input.PreeditEvent;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Real user/other-mod sign edits revoke automation first; owned decimal entry retains its root. */
@Mixin(AbstractSignEditScreen.class)
public abstract class AutomationSignInputMixin {
 private static void manual(){if(!AutomationMenuActions.ownsInventoryInput())KuanperionClient.onManualAutomationInventoryInput();}
 @Inject(method="keyPressed",at=@At("HEAD")) private void kuanperion$key(KeyEvent event,CallbackInfoReturnable<Boolean> ci){manual();}
 @Inject(method="charTyped",at=@At("HEAD")) private void kuanperion$char(CharacterEvent event,CallbackInfoReturnable<Boolean> ci){manual();}
 @Inject(method="preeditUpdated",at=@At("HEAD")) private void kuanperion$preedit(PreeditEvent event,CallbackInfoReturnable<Boolean> ci){manual();}
 @Inject(method="onClose",at=@At("HEAD")) private void kuanperion$close(CallbackInfo ci){manual();}
}
