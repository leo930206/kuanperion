// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;import java.util.function.Supplier;
import com.mojang.blaze3d.platform.InputConstants;import net.minecraft.client.KeyMapping;import org.lwjgl.glfw.GLFW;import net.minecraft.client.Minecraft;import net.minecraft.client.player.RemotePlayer;import net.minecraft.world.InteractionHand;import net.minecraft.world.level.ClipContext;import net.minecraft.world.phys.*;
/** PROVISIONAL local narrower NPC family: loaded RemotePlayer named exactly Phillip, never another entity kind.
 * The supplied source establishes name search/desk/aim/use, not this NPC type or current name/interaction contract. */
final class PestBarnGameView implements PestBarnInteraction.Port {
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final GardenFlightCore flight;private final Supplier<PestIdentity.Vacuum> prepared;
 PestBarnGameView(AutomationCoordinator c,AutomationSession s,GardenFlightCore f,Supplier<PestIdentity.Vacuum> tool){coordinator=c;session=s;flight=f;prepared=tool;}
 static PestBarnTarget.Facts read(Minecraft mc,AutomationContext c,long now){
  long world=c==null?0:c.worldId();var unknown=new PestBarnTarget.Facts(world,now,false,List.of());
  if(mc==null||!mc.isSameThread()||c==null||!c.garden()||mc.level==null||mc.player==null||!PestPlot.of(0).contains(mc.player.getX(),mc.player.getZ()))return unknown;
  try{var out=new ArrayList<PestBarnTarget>();int seen=0;for(var e:mc.level.entitiesForRendering()){
   if(++seen>512)return unknown;if(!(e instanceof RemotePlayer p)||!npc(p))continue;if(out.size()==32)return unknown;out.add(target(p));
  }return new PestBarnTarget.Facts(world,now,true,out);}catch(RuntimeException unavailable){return unknown;}
 }
 private static boolean npc(RemotePlayer p){return p!=null&&!p.isRemoved()&&p.isAlive()&&!p.isSpectator()&&p.getName().getString().equals("Phillip")&&PestPlot.of(0).contains(p.getX(),p.getZ());}
 private static PestBarnTarget target(RemotePlayer p){var a=p.getEyePosition();return new PestBarnTarget(p.getId(),p.getUUID(),new PestTargets.Point(a.x,a.y,a.z));}
 private static RemotePlayer entity(Minecraft mc,PestBarnTarget t){if(t==null||mc.level==null)return null;var e=mc.level.getEntity(t.entityId());return e instanceof RemotePlayer p&&npc(p)&&p.getUUID().equals(t.uuid())&&target(p).equals(t)?p:null;}
 public boolean allowed(AutomationSession.Handle h,long now){
  var mc=Minecraft.getInstance();var level=mc.level;var p=mc.player;
  if(!mc.isSameThread()||h==null||h.lease().owner()!=AutomationFeature.PEST||p==null||level==null||mc.gameMode==null||!session.isCurrent(h)||!h.equals(flight.owner())||level!=flight.levelIdentity()||!flight.allowed(h,now,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.USE))return false;
  var tool=prepared.get();return mc.level==level&&mc.player==p&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&h.equals(flight.owner())&&mc.level==flight.levelIdentity()&&tool!=null&&p.getInventory().getSelectedSlot()==tool.slot()&&PestIdentity.sameVacuum(tool,AutomationGameView.item(p.getMainHandItem(),true))&&!manualInput();
 }
 /** Physical priority also includes focus and hotbar keys, which the shared flight movement sampler does not cover. */
 private boolean manualInput(){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.options==null||mc.getWindow()==null||GLFW.glfwGetWindowAttrib(mc.getWindow().handle(),GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE||flight.manualInput())return true;
  for(var key:mc.options.keyHotbarSlots)if(physical(key))return true;return false;
 }
 private boolean physical(KeyMapping mapping){
  if(mapping==null)return true;if(mapping.isUnbound())return false;var key=InputConstants.getKey(mapping.saveString());long window=Minecraft.getInstance().getWindow().handle();
  if(key.getType()==InputConstants.Type.KEYSYM)return key.getValue()<0||GLFW.glfwGetKey(window,key.getValue())==GLFW.GLFW_PRESS;
  if(key.getType()==InputConstants.Type.MOUSE)return GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS;return true;
 }
 public PestBarnInteraction.Frame frame(AutomationSession.Handle h,PestBarnTarget t,long now){
  var mc=Minecraft.getInstance();var p=mc.player;if(!mc.isSameThread()||h==null||p==null||mc.level==null||mc.gameMode==null)return null;var e=entity(mc,t);if(e==null)return null;
  var eye=p.getEyePosition();var aim=e.getEyePosition();var tool=prepared.get();var actual=AutomationGameView.item(p.getMainHandItem(),true);
  boolean usable=mc.gui.screen()==null&&mc.gui.overlay()==null&&p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&!p.isSleeping()&&!p.isUsingItem()&&p.getAbilities().mayfly&&p.containerMenu==p.inventoryMenu&&p.containerMenu.containerId==0&&p.containerMenu.getCarried().isEmpty()&&tool!=null&&p.getInventory().getSelectedSlot()==tool.slot()&&PestIdentity.sameVacuum(tool,actual)&&p.getMainHandItem().isItemEnabled(mc.level.enabledFeatures())&&!manualInput()&&eye.distanceToSqr(aim)<=2.75*2.75&&flight.loaded(new AABB(eye,aim).inflate(.01))&&mc.level.clip(new ClipContext(eye,aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p)).getType()==HitResult.Type.MISS;
  boolean hit=mc.hitResult instanceof EntityHitResult ray&&ray.getEntity()==e&&eye.distanceToSqr(ray.getLocation())<=2.75*2.75;
  return new PestBarnInteraction.Frame(h.lease().worldId(),now,mc.level,p,mc.gameMode,e,new PestTargets.Point(p.getX(),p.getY(),p.getZ()),new PestTargets.Point(eye.x,eye.y,eye.z),t.aim(),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),actual,usable,hit);
 }
 public PestBarnInteraction.Effect effect(AutomationSession.Handle h,PestBarnInteraction.Action action,long now){
  var mc=Minecraft.getInstance();boolean started=false;
  try{
   if(action==null||!allowed(h,System.nanoTime()))return PestBarnInteraction.Effect.REJECTED;
   var before=frame(h,action.target(),System.nanoTime());if(!action.claim(h,before,System.nanoTime())||!allowed(h,System.nanoTime()))return PestBarnInteraction.Effect.REJECTED;
   var physical=frame(h,action.target(),System.nanoTime());long at=System.nanoTime();
   if(h==null||at-h.lease().started()<0||at-h.lease().started()>=h.lease().lifetime()||!mc.isSameThread()||mc.level==null||mc.player==null||mc.gameMode==null||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||!h.equals(flight.owner())||mc.level!=flight.levelIdentity()||manualInput()||!action.matchesSnapshot(physical)||mc.level!=physical.level()||mc.player!=physical.player()||mc.gameMode!=physical.gameMode())return PestBarnInteraction.Effect.REJECTED;
   if(action.kind()==PestBarnInteraction.Kind.ROTATE){
    if(!Float.isFinite(action.yaw())||!Float.isFinite(action.pitch())||action.pitch()< -90||action.pitch()>90||Math.abs(action.yaw()-physical.yaw())>12.001||Math.abs(action.pitch()-physical.pitch())>12.001)return PestBarnInteraction.Effect.REJECTED;
    started=true;mc.player.setYRot(action.yaw());mc.player.setXRot(action.pitch());return PestBarnInteraction.Effect.SENT;
   }
   if(!physical.hit()||!(physical.entity() instanceof RemotePlayer npc)||entity(mc,action.target())!=npc||!(mc.hitResult instanceof EntityHitResult ray)||ray.getEntity()!=npc||mc.player.getEyePosition().distanceToSqr(ray.getLocation())>2.75*2.75)return PestBarnInteraction.Effect.REJECTED;
   started=true;mc.gameMode.interact(mc.player,npc,ray,InteractionHand.MAIN_HAND);return PestBarnInteraction.Effect.SENT;
  }catch(RuntimeException ambiguous){return started?PestBarnInteraction.Effect.UNCERTAIN:PestBarnInteraction.Effect.REJECTED;}
 }
}
