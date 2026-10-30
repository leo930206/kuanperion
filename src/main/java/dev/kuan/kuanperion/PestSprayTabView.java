// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.PlayerPositionReceipts;
import dev.kuan.kuanperion.automation.pest.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
/** TAIL of pinned26.2 player-info application, not a polling-derived refresh or generic packet receipt. */
final class PestSprayTabView {
 static void applied(Minecraft mc,ClientPacketListener listener,ClientboundPlayerInfoUpdatePacket packet,long world,PlayerPositionReceipts.Stamp position,PestSprayTabReceipts receipts,long now){
  if(!mc.isSameThread()||mc.level==null||mc.player==null||mc.player.connection!=listener||packet==null||!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)||packet.entries().size()>128)return;
  var scope=scope(mc,world,position);if(scope.isEmpty())return;
  var players=GardenTabReader.players(mc);var entries=players.stream().map(GardenTabReader::text).toList();var field=PestSprayObservation.fieldEntry(entries);if(field.isEmpty())return;
  int index=field.getAsInt();var row=players.get(index);var display=row.getTabListDisplayName();
  if(display==null||listener.getPlayerInfo(row.getProfile().id())!=row)return;
  var updates=packet.entries().stream().filter(e->e.profileId().equals(row.getProfile().id())).toList();
  // Unknown/unlisted rows, ignored IDs, duplicate entries and equal-looking foreign components are not applied proof.
  if(updates.size()!=1||!Objects.equals(updates.getFirst().displayName(),display))return;
  receipts.observe(scope.get(),row,entries,index,now);
 }
 static Optional<PestSprayTabReceipts.Proof> current(Minecraft mc,long world,PlayerPositionReceipts.Stamp position,PestSprayTabReceipts receipts,long now){
  if(!mc.isSameThread()||mc.level==null||mc.player==null)return Optional.empty();var scope=scope(mc,world,position);if(scope.isEmpty())return Optional.empty();
  var players=GardenTabReader.players(mc);var entries=players.stream().map(GardenTabReader::text).toList();var field=PestSprayObservation.fieldEntry(entries);
  return field.isEmpty()?Optional.empty():receipts.current(scope.get(),players.get(field.getAsInt()),entries,now);
 }
 private static Optional<PestSprayTabReceipts.Scope> scope(Minecraft mc,long world,PlayerPositionReceipts.Stamp position){
  if(world<=0||position==null||position.world()!=world)return Optional.empty();var plot=PestPlot.at(mc.player.getX(),mc.player.getZ());if(plot.isEmpty()||plot.get().id()==0)return Optional.empty();
  return Optional.of(new PestSprayTabReceipts.Scope(world,mc.level,mc.player,plot.get().id(),position.epoch(),position.sequence()));
 }
}
