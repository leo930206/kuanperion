// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.greenhouse.GreenhouseTarget;import net.minecraft.network.protocol.Packet;import net.minecraft.network.protocol.game.*;import net.minecraft.world.entity.EquipmentSlot;import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.entity.BlockEntityTypes;import net.minecraft.world.level.block.state.BlockState;import java.util.function.*;
/** Projection of a finite set of server updates, not prediction, damage animation or harvest success. */
public final class GreenhousePacketView {
 private GreenhousePacketView(){}
 public static boolean changed(GreenhouseTarget.Ref watched,Packet<?> packet){
  if(watched==null||packet==null)return false;
  try{
   if(watched instanceof GreenhouseTarget.EntityRef ref){
    if(packet instanceof ClientboundRemoveEntitiesPacket remove)return remove.getEntityIds()!=null&&remove.getEntityIds().size()<=4096&&remove.getEntityIds().contains(ref.id());
    if(packet instanceof ClientboundSetEquipmentPacket equipment){
     if(equipment.getEntity()!=ref.id()||equipment.getSlots()==null||equipment.getSlots().size()>16)return false;int heads=0;
     for(var pair:equipment.getSlots()){if(pair==null||pair.getFirst()==null||pair.getSecond()==null)return false;if(pair.getFirst()==EquipmentSlot.HEAD)heads++;}return heads==1;
    }
    return false;
   }
   var ref=(GreenhouseTarget.BlockRef)watched;
   if(packet instanceof ClientboundBlockUpdatePacket block)return same(ref,block.getPos())&&block.getBlockState()!=null;
   if(packet instanceof ClientboundBlockEntityDataPacket data)return same(ref,data.getPos())&&data.getType()==BlockEntityTypes.SKULL&&data.getTag()!=null;
   if(packet instanceof ClientboundSectionBlocksUpdatePacket section)return positions(ref,section::runUpdates);
   return false;
  }catch(RuntimeException malformed){return false;}
 }
 private static boolean same(GreenhouseTarget.BlockRef ref,BlockPos pos){return pos!=null&&pos.getX()==ref.x()&&pos.getY()==ref.y()&&pos.getZ()==ref.z();}
 private static final class Stop extends RuntimeException{Stop(){super(null,null,false,false);}}
 private static final Stop STOP=new Stop();
 /** Native runUpdates has no early-exit API. Stop after the exact watched position or the section's 4096-block bound. */
 static boolean positions(GreenhouseTarget.Ref watched,Consumer<BiConsumer<BlockPos,BlockState>> changes){return state(watched,changes).isPresent();}
 static java.util.Optional<BlockState> state(GreenhouseTarget.Ref watched,Consumer<BiConsumer<BlockPos,BlockState>> changes){
  if(!(watched instanceof GreenhouseTarget.BlockRef ref)||changes==null)return java.util.Optional.empty();int[] count={0};BlockState[] found={null};
  try{changes.accept((pos,state)->{if(++count[0]>4096)throw STOP;if(state!=null&&same(ref,pos)){found[0]=state;throw STOP;}});}catch(Stop stop){}catch(RuntimeException malformed){return java.util.Optional.empty();}return java.util.Optional.ofNullable(found[0]);
 }
}
