// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.mixin;
import dev.kuan.kuanperion.KuanperionClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observe server-applied results, never client prediction or outbound packets.
 * Container handlers also return normally for ignored packets: observe only after their actual writes. */
@Mixin(ClientPacketListener.class)
public abstract class AutomationReceiptMixin {
 @Inject(method="handleContainerClose",at=@At("HEAD"))
 private void kuanperion$beforeClose(ClientboundContainerClosePacket packet,CallbackInfo ci){KuanperionClient.observeAutomationMenuClose(packet,true);}
 @Inject(method="handleContainerClose",at=@At("TAIL"))
 private void kuanperion$afterClose(ClientboundContainerClosePacket packet,CallbackInfo ci){KuanperionClient.observeAutomationMenuClose(packet,false);}
 @Inject(method="handleRemoveEntities",at=@At("TAIL"))
 private void kuanperion$harvestRemove(ClientboundRemoveEntitiesPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationHarvest(packet);}
 @Inject(method="handleSetEquipment",at=@At("TAIL"))
 private void kuanperion$harvestEquipment(ClientboundSetEquipmentPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationHarvest(packet);}
 @Inject(method="handleBlockUpdate",at=@At("TAIL"))
 private void kuanperion$harvestBlock(ClientboundBlockUpdatePacket packet,CallbackInfo ci){KuanperionClient.observeAutomationHarvest(packet);}
 @Inject(method="handleBlockEntityData",at=@At("TAIL"))
 private void kuanperion$harvestBlockEntity(ClientboundBlockEntityDataPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationHarvest(packet);}
 @Inject(method="handleChunkBlocksUpdate",at=@At("TAIL"))
 private void kuanperion$harvestSection(ClientboundSectionBlocksUpdatePacket packet,CallbackInfo ci){KuanperionClient.observeAutomationHarvest(packet);}
 @Inject(method="handleSetCursorItem",at=@At("TAIL"))
 private void kuanperion$cursor(ClientboundSetCursorItemPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationCursor(packet);}
 @Inject(method="handlePlayerInfoUpdate",at=@At("TAIL"))
 private void kuanperion$sprayTab(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket packet,CallbackInfo ci){KuanperionClient.observeAutomationSprayTab((ClientPacketListener)(Object)this,packet);}
 @Inject(method="handleMovePlayer",at=@At("TAIL"))
 private void kuanperion$position(ClientboundPlayerPositionPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationPosition(packet);}
 @Inject(method="handleContainerSetSlot",at={
  @At(value="INVOKE",target="Lnet/minecraft/world/inventory/InventoryMenu;setItem(IILnet/minecraft/world/item/ItemStack;)V",shift=At.Shift.AFTER),
  @At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;setItem(IILnet/minecraft/world/item/ItemStack;)V",shift=At.Shift.AFTER)
 },require=2,allow=2)
 private void kuanperion$slot(ClientboundContainerSetSlotPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationSlot(packet.getContainerId(),packet.getSlot(),packet.getItem());}
 @Inject(method="handleContainerContent",at={
  @At(value="INVOKE",target="Lnet/minecraft/world/inventory/InventoryMenu;initializeContents(ILjava/util/List;Lnet/minecraft/world/item/ItemStack;)V",shift=At.Shift.AFTER),
  @At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;initializeContents(ILjava/util/List;Lnet/minecraft/world/item/ItemStack;)V",shift=At.Shift.AFTER)
 },require=2,allow=2)
 private void kuanperion$content(ClientboundContainerSetContentPacket packet,CallbackInfo ci){for(int i=0;i<packet.items().size();i++)KuanperionClient.observeAutomationSlot(packet.containerId(),i,packet.items().get(i));KuanperionClient.observeAutomationContainerCursor(packet);}
 @Inject(method="handleSetPlayerInventory",at=@At("TAIL"))
 private void kuanperion$inventory(ClientboundSetPlayerInventoryPacket packet,CallbackInfo ci){KuanperionClient.observeAutomationInventorySlot(packet.slot(),packet.contents());}
}
