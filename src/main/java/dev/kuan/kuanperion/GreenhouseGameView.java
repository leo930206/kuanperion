// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.TextureIdentity;import dev.kuan.kuanperion.automation.AutomationContext;import dev.kuan.kuanperion.automation.greenhouse.*;import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import dev.kuan.kuanperion.automation.greenhouse.GreenhouseIdentity;import java.util.*;
import net.minecraft.client.Minecraft;import net.minecraft.client.multiplayer.ClientLevel;import net.minecraft.core.BlockPos;import net.minecraft.world.entity.decoration.ArmorStand;import net.minecraft.world.entity.EquipmentSlot;import net.minecraft.world.level.block.entity.SkullBlockEntity;import net.minecraft.world.level.chunk.LevelChunk;import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.network.protocol.Packet;import net.minecraft.network.protocol.game.*;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.level.block.AbstractSkullBlock;import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;import net.minecraft.world.item.*;import net.minecraft.world.item.component.ResolvableProfile;import net.minecraft.world.entity.player.PlayerSkin;
/** Read-only local profiles. No resolving/fetching an account/skin and no target inferred from a display name. */
public final class GreenhouseGameView {
 private GreenhouseGameView(){}
 public static Optional<GreenhouseIdentity.Kind> kind(ItemStack stack){
  if(stack==null||stack.isEmpty()||!stack.is(Items.PLAYER_HEAD)||stack.has(DataComponents.CUSTOM_NAME))return Optional.empty();return hash(stack.get(DataComponents.PROFILE)).flatMap(GreenhouseIdentity::kindHash);
 }
 public static Optional<String> hash(ResolvableProfile profile){
  if(profile==null||!profile.skinPatch().equals(PlayerSkin.Patch.EMPTY)||profile.partialProfile().properties()==null)return Optional.empty();
  var textures=profile.partialProfile().properties().get("textures");return textures.size()==1?TextureIdentity.hash(textures.iterator().next().value()):Optional.empty();
 }
 static boolean sameHead(ItemStack server,ItemStack current){
  if(server==null||current==null)return false;try{return AutomationGameView.item(server,false).equals(AutomationGameView.item(current,false));}catch(RuntimeException malformed){return false;}
 }
 static boolean sameState(BlockState server,BlockState current){return server!=null&&server.equals(current);}
 /** Raw server geometry alone is insufficient when a handler ignored an update for a missing/predicted target. */
 public static boolean applied(Minecraft mc,GreenhouseTarget.Ref watched,Packet<?> packet){
  if(mc==null||!mc.isSameThread()||mc.level==null||!GreenhousePacketView.changed(watched,packet))return false;
  try{
   if(watched instanceof GreenhouseTarget.EntityRef ref){
    if(packet instanceof ClientboundRemoveEntitiesPacket)return true; // The server explicitly removed this ID, even if local prediction already hid it.
    if(packet instanceof ClientboundSetEquipmentPacket equipment){
     var entity=mc.level.getEntity(ref.id());if(!(entity instanceof LivingEntity living)||!entity.getUUID().equals(ref.uuid()))return false;
     return equipment.getSlots().stream().filter(pair->pair.getFirst()==EquipmentSlot.HEAD).anyMatch(pair->sameHead(pair.getSecond(),living.getItemBySlot(EquipmentSlot.HEAD)));
    }return false;
   }
   var ref=(GreenhouseTarget.BlockRef)watched;var pos=new BlockPos(ref.x(),ref.y(),ref.z());if(!mc.level.hasChunkAt(pos))return false;
   var chunk=mc.level.getChunkSource().getChunk(ref.x()>>4,ref.z()>>4,ChunkStatus.FULL,false);if(chunk==null)return false;
   if(packet instanceof ClientboundBlockUpdatePacket block)return sameState(block.getBlockState(),mc.level.getBlockState(pos));
   if(packet instanceof ClientboundSectionBlocksUpdatePacket section)return GreenhousePacketView.state(ref,section::runUpdates).filter(state->sameState(state,mc.level.getBlockState(pos))).isPresent();
   return packet instanceof ClientboundBlockEntityDataPacket&&mc.level.getBlockState(pos).getBlock() instanceof AbstractSkullBlock&&chunk.getBlockEntities().get(pos) instanceof SkullBlockEntity;
  }catch(RuntimeException unavailable){return false;}
 }
 private static GreenhouseTask.TargetStatus status(GreenhouseTask.TargetState state){return new GreenhouseTask.TargetStatus(state,null);}
 static GreenhouseTask.TargetStatus headStatus(GreenhouseTarget expected,ItemStack head,Point point){
  if(head==null||head.has(DataComponents.CUSTOM_NAME))return status(GreenhouseTask.TargetState.UNKNOWN);
  if(head.isEmpty()||!head.is(Items.PLAYER_HEAD))return status(GreenhouseTask.TargetState.CHANGED);
  var hash=hash(head.get(DataComponents.PROFILE));if(hash.isEmpty())return status(GreenhouseTask.TargetState.UNKNOWN);
  return hash.flatMap(GreenhouseIdentity::kindHash).filter(expected.kind()::equals).isPresent()?new GreenhouseTask.TargetStatus(GreenhouseTask.TargetState.MATURE,new GreenhouseTarget(expected.ref(),expected.kind(),point)):status(GreenhouseTask.TargetState.CHANGED);
 }
 /** Local disappearance alone never proves success: the task separately requires its watched new server receipt. */
 public static GreenhouseTask.TargetStatus target(Minecraft mc,GreenhouseTarget expected){
  if(mc==null||expected==null||!mc.isSameThread()||mc.level==null||mc.player==null)return status(GreenhouseTask.TargetState.UNKNOWN);
  try{
   var position=expected.position();var pos=BlockPos.containing(position.x(),position.y(),position.z());
   if(mc.level.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,ChunkStatus.FULL,false)==null)return status(GreenhouseTask.TargetState.UNKNOWN);
   if(expected.ref() instanceof GreenhouseTarget.EntityRef ref){
    var entity=mc.level.getEntity(ref.id());if(entity==null||entity.isRemoved()||!entity.getUUID().equals(ref.uuid()))return status(GreenhouseTask.TargetState.CHANGED);
    if(!(entity instanceof ArmorStand stand))return status(GreenhouseTask.TargetState.UNKNOWN);
    var current=BlockPos.containing(stand.getX(),stand.getY(),stand.getZ());if(mc.level.getChunkSource().getChunk(current.getX()>>4,current.getZ()>>4,ChunkStatus.FULL,false)==null)return status(GreenhouseTask.TargetState.UNKNOWN);
    return headStatus(expected,stand.getItemBySlot(EquipmentSlot.HEAD),new Point(stand.getX(),stand.getY()+stand.getEyeHeight(),stand.getZ()));
   }
   var ref=(GreenhouseTarget.BlockRef)expected.ref();pos=new BlockPos(ref.x(),ref.y(),ref.z());var chunk=mc.level.getChunkSource().getChunk(ref.x()>>4,ref.z()>>4,ChunkStatus.FULL,false);if(chunk==null)return status(GreenhouseTask.TargetState.UNKNOWN);
   if(!(mc.level.getBlockState(pos).getBlock() instanceof AbstractSkullBlock))return status(GreenhouseTask.TargetState.CHANGED);
   var entity=chunk.getBlockEntities().get(pos);if(!(entity instanceof SkullBlockEntity skull)||skull.isRemoved())return status(GreenhouseTask.TargetState.UNKNOWN);
   var hash=hash(skull.getOwnerProfile());if(hash.isEmpty())return status(GreenhouseTask.TargetState.UNKNOWN);
   return hash.flatMap(GreenhouseIdentity::kindHash).filter(expected.kind()::equals).isPresent()?new GreenhouseTask.TargetStatus(GreenhouseTask.TargetState.MATURE,expected):status(GreenhouseTask.TargetState.CHANGED);
  }catch(RuntimeException unavailable){return status(GreenhouseTask.TargetState.UNKNOWN);}
 }
 /** Empty Optional means unavailable/unknown, never an empty completed greenhouse. No chunk is loaded here. */
 public static Optional<GreenhouseScan> scan(Minecraft mc,AutomationContext context,Point center,int radius,Set<Integer> plots,Set<GreenhouseIdentity.Kind> kinds,long now){
  if(mc==null||context==null||!context.garden()||context.hardBlocked()||mc.player==null||mc.level==null||!mc.isSameThread())return Optional.empty();
  try{return Optional.of(new GreenhouseScan(context.worldId(),center,radius,plots,kinds,new NativeSource(mc,context.worldId(),center,radius),now));}catch(RuntimeException unavailable){return Optional.empty();}
 }
 private interface Node{Optional<GreenhouseTarget> read(ClientLevel level);}
 private record EntityNode(ArmorStand stand,UUID uuid)implements Node{
  public Optional<GreenhouseTarget> read(ClientLevel level){
   if(stand.isRemoved()||!stand.getUUID().equals(uuid)||level.getEntity(stand.getId())!=stand)return Optional.empty();
   return kind(stand.getItemBySlot(EquipmentSlot.HEAD)).map(kind->new GreenhouseTarget(new GreenhouseTarget.EntityRef(stand.getId(),uuid),kind,new Point(stand.getX(),stand.getY()+stand.getEyeHeight(),stand.getZ())));
  }
 }
 private record BlockNode(BlockPos pos,SkullBlockEntity skull)implements Node{
  public Optional<GreenhouseTarget> read(ClientLevel level){
   var chunk=level.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,ChunkStatus.FULL,false);
   if(chunk==null||skull.isRemoved()||chunk.getBlockEntities().get(pos)!=skull)return Optional.empty();
   return hash(skull.getOwnerProfile()).flatMap(GreenhouseIdentity::kindHash).map(kind->new GreenhouseTarget(new GreenhouseTarget.BlockRef(pos.getX(),pos.getY(),pos.getZ()),kind,new Point(pos.getX()+.5,pos.getY()+.25,pos.getZ()+.5)));
  }
 }
 private static final class NativeSource implements GreenhouseScan.Source{
  private final Minecraft mc;private final ClientLevel level;private final long world;private final List<Node> nodes=new ArrayList<>();private final List<LevelChunk> chunks=new ArrayList<>();private int index;
  NativeSource(Minecraft mc,long world,Point center,int radius){
   if(radius<4||radius>25)throw new IllegalArgumentException("Invalid discovery radius");this.mc=mc;this.level=mc.level;this.world=world;int inspected=0;
   for(var entity:level.entitiesForRendering()){
    if(++inspected>4096)throw new IllegalArgumentException("Entity discovery limit");
    if(entity instanceof ArmorStand stand&&!stand.isRemoved()&&near(center,radius,stand.getX(),stand.getY()+stand.getEyeHeight(),stand.getZ()))nodes.add(new EntityNode(stand,stand.getUUID()));
   }
   int minX=((int)Math.floor(center.x()-radius))>>4,maxX=((int)Math.floor(center.x()+radius))>>4,minZ=((int)Math.floor(center.z()-radius))>>4,maxZ=((int)Math.floor(center.z()+radius))>>4;
   for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
    var chunk=level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);if(chunk==null)throw new IllegalArgumentException("Unloaded discovery chunk");chunks.add(chunk);
    for(var entry:chunk.getBlockEntities().entrySet()){
     if(++inspected>16384)throw new IllegalArgumentException("Block entity discovery limit");var pos=entry.getKey();
     if(entry.getValue() instanceof SkullBlockEntity skull&&near(center,radius,pos.getX()+.5,pos.getY()+.25,pos.getZ()+.5))nodes.add(new BlockNode(pos.immutable(),skull));
     if(nodes.size()>8192)throw new IllegalArgumentException("Discovery node limit");
    }
   }
  }
  private static boolean near(Point center,int radius,double x,double y,double z){double dx=x-center.x(),dz=z-center.z();return dx*dx+dz*dz<=radius*radius&&Math.abs(y-center.y())<=10;}
  public long world(){return mc.level==level&&mc.player!=null?world:0;}
  public boolean loaded(){
   if(world()==0)return false;
   for(var chunk:chunks){var pos=chunk.getPos();if(level.getChunkSource().getChunk(pos.x(),pos.z(),ChunkStatus.FULL,false)!=chunk)return false;}return true;
  }
  public boolean hasNext(){return index<nodes.size();}
  public Optional<GreenhouseTarget> next(){return nodes.get(index++).read(level);}
 }
}
