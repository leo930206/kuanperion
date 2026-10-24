// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.mixin;
import dev.kuan.kuanperion.AutomationMenuActions;
import dev.kuan.kuanperion.KuanperionClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Manual/other-mod inventory actions take priority over an in-flight automated transaction. */
@Mixin(MultiPlayerGameMode.class)
public abstract class AutomationInventoryInputMixin {
 @Inject(method="handleContainerInput",at=@At("HEAD"))
 private void kuanperion$manualInventoryInput(int container,int slot,int button,ContainerInput input,Player player,CallbackInfo ci){if(!AutomationMenuActions.ownsInventoryInput())KuanperionClient.onManualAutomationInventoryInput();}
}
