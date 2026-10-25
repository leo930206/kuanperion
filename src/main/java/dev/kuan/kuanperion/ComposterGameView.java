// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.composter.*;import java.util.*;import net.minecraft.client.Minecraft;import net.minecraft.core.BlockPos;import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.world.entity.decoration.ArmorStand;import net.minecraft.world.level.chunk.status.ChunkStatus;import net.minecraft.world.phys.*;import net.minecraft.util.StringUtil;
/** Read-only configured machine + exact local hologram corroboration; never name-substring or blind source coordinates. */
public final class ComposterGameView {
 private ComposterGameView(){}
 public static boolean markerName(String name){return name!=null&&Set.of("Composter","COMPOSTER").contains(StringUtil.stripColor(name).trim());}
 public static boolean valid(Minecraft mc,AutomationContext context,ComposterPolicy.Anchor anchor){
  if(mc==null||context==null||anchor==null||!mc.isSameThread()||mc.player==null||mc.level==null||!context.garden()||context.hardBlocked())return false;
  var pos=new BlockPos(anchor.x(),anchor.y(),anchor.z());if(mc.level.getChunkSource().getChunk(anchor.x()>>4,anchor.z()>>4,ChunkStatus.FULL,false)==null||!BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).toString().equals(anchor.blockId()))return false;
  int visited=0,matches=0;for(var entity:mc.level.entitiesForRendering()){
   if(++visited>4096)return false;if(entity instanceof ArmorStand stand&&!stand.isRemoved()&&stand.hasCustomName()&&markerName(stand.getCustomName().getString())&&stand.position().distanceToSqr(Vec3.atCenterOf(pos))<=4*4)matches++;
  }return matches==1;
 }
 public static Optional<ComposterPolicy.Anchor> capture(Minecraft mc,AutomationContext context){
  if(mc==null||mc.player==null||mc.level==null||!mc.isSameThread()||!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||mc.player.getEyePosition().distanceToSqr(hit.getLocation())>5*5)return Optional.empty();
  var pos=hit.getBlockPos();if(mc.level.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,ChunkStatus.FULL,false)==null)return Optional.empty();
  try{var anchor=new ComposterPolicy.Anchor(pos.getX(),pos.getY(),pos.getZ(),BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).toString());return valid(mc,context,anchor)?Optional.of(anchor):Optional.empty();}catch(RuntimeException invalid){return Optional.empty();}
 }
}
