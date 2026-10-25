// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.mixin;
import dev.kuan.kuanperion.KuanperionClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observe server-applied results, never client prediction or outbound packets. */
@Mixin(ClientPacketListener.class)
public abstract class AutomationReceiptMixin {
 @Inject(method="handleSetCursorItem",at=@At("TAIL"))
 private void kuanperion$cursor(ClientboundSetCursorItemPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationCursor(packet);}
 @Inject(method="handleMovePlayer",at=@At("TAIL"))
 private void kuanperion$position(ClientboundPlayerPositionPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationPosition(packet);}
 @Inject(method="handleContainerSetSlot",at=@At("TAIL"))
 private void kuanperion$slot(ClientboundContainerSetSlotPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationSlot(packet.getContainerId(),packet.getSlot(),packet.getItem());}
 @Inject(method="handleContainerContent",at=@At("TAIL"))
 private void kuanperion$content(ClientboundContainerSetContentPacket packet,CallbackInfo ci){for(int i=0;i<packet.items().size();i++)KuanperionClient.observeAutomationSlot(packet.containerId(),i,packet.items().get(i));KuanperionClient.observeAutomationContainerCursor(packet);}
 @Inject(method="handleSetPlayerInventory",at=@At("TAIL"))
 private void kuanperion$inventory(ClientboundSetPlayerInventoryPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationInventorySlot(packet.slot(),packet.contents());}
}
