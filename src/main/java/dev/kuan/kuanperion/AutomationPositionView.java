// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.PlayerPositionReceipts;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.Relative;
/** Packet data only; never use predicted player.position() to invent a teleport acknowledgement. */
public final class AutomationPositionView {
 private AutomationPositionView(){}
 public static PlayerPositionReceipts.Position absolute(ClientboundPlayerPositionPacket packet){
  if(packet==null||packet.change()==null||packet.change().position()==null||packet.relatives()==null)return null;
  if(packet.relatives().contains(Relative.X)||packet.relatives().contains(Relative.Y)||packet.relatives().contains(Relative.Z))return null;
  var p=packet.change().position();try{return new PlayerPositionReceipts.Position(p.x,p.y,p.z);}catch(IllegalArgumentException invalid){return null;}
 }
}
