// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.visitor.*;import net.minecraft.client.Minecraft;import java.util.*;
import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.entity.decoration.ArmorStand;import net.minecraft.util.StringUtil;import net.minecraft.world.level.chunk.status.ChunkStatus;
/** Read-only native projection. No interaction, packet, price lookup, profile resolution or configuration writes. */
public final class VisitorGameView {
 private VisitorGameView(){}
 public record Observation(VisitorOffer offer,Map<Integer,ItemSnapshot> inventory){public Observation{Objects.requireNonNull(offer);inventory=Map.copyOf(inventory);}}
 /** Includes pinned26.2 offhand40, body41 and saddle42, not only the36 main slots. */
 public static Map<Integer,ItemSnapshot> inventory(Minecraft mc){
  if(mc==null||!mc.isSameThread()||mc.player==null||mc.level==null)return Map.of();var inv=mc.player.getInventory();
  if(inv.getContainerSize()!=43||inv.getNonEquipmentItems().size()!=36)return Map.of();var result=new HashMap<Integer,ItemSnapshot>();
  for(int i=0;i<43;i++)result.put(i,AutomationGameView.item(inv.getItem(i),i<9||i>=36));return Map.copyOf(result);
 }
 public static Optional<Observation> offer(Minecraft mc,long world,String selectedVisitor,Map<String,String> exactMaterialIds){
  var inventory=inventory(mc);if(inventory.size()!=43||world<=0)return Optional.empty();var menu=AutomationGameView.menu(mc,world);
  return VisitorOffer.inspect(menu,selectedVisitor,exactMaterialIds).map(offer->new Observation(offer,inventory));
 }
 public static VisitorQueue queue(Minecraft mc){return mc==null||!mc.isSameThread()||mc.player==null||mc.level==null?VisitorQueue.unknown():VisitorQueue.parse(GardenTabReader.read(mc));}
 /** Exact queued hologram plus a sole nearby living non-listed character; never first/nearest entity. */
 public static List<VisitorTargets.Target> targets(Minecraft mc,VisitorQueue queue){
  if(mc==null||!mc.isSameThread()||mc.player==null||mc.level==null||queue==null||!queue.known()||mc.player.connection.getListedOnlinePlayers().size()>80)return List.of();
  var listed=new HashSet<UUID>();for(var info:mc.player.connection.getListedOnlinePlayers())listed.add(info.getProfile().id());
  var names=new HashSet<String>();queue.entries().forEach(entry->names.add(entry.name()));var facts=new ArrayList<VisitorTargets.Fact>();int inspected=0;
  for(var entity:mc.level.entitiesForRendering()){
   if(++inspected>4096)return List.of();if(entity==mc.player||entity.distanceToSqr(mc.player)>64*64||!(entity instanceof LivingEntity))continue;
   boolean marker=entity instanceof ArmorStand;String name=StringUtil.stripColor((entity.hasCustomName()?entity.getCustomName():entity.getName()).getString()).trim();
   if(marker&&(!entity.hasCustomName()||!names.contains(name)))continue;
   if(facts.size()>=256||name.length()>256)return List.of();var pos=entity.blockPosition();if(mc.level.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,ChunkStatus.FULL,false)==null)return List.of();
   facts.add(new VisitorTargets.Fact(entity.getId(),entity.getUUID(),marker?VisitorTargets.Kind.MARKER:VisitorTargets.Kind.CHARACTER,new Point(entity.getX(),entity.getY(),entity.getZ()),name,!entity.isRemoved()&&entity.isAlive(),entity.isInvisible(),listed.contains(entity.getUUID())));
  }
  return VisitorTargets.select(queue,facts);
 }
}
