// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.flight.*;
import dev.kuan.kuanperion.automation.pest.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import org.lwjgl.glfw.GLFW;

/** Fixed-feature legal flight primitive: one lease, one owned key overlay, no gameplay use/attack or global macro state. */
final class GardenFlightCore {
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final AutomationFeature feature;
 private AutomationSession.Handle owner;private Object ownedLevel;private final Set<KeyMapping> pressed=new HashSet<>();private ItemSnapshot preparedTool;private int preparedSlot;
 private PestFlightBounds bounds;private PestFlightPath search;private PestTargets.Point searchGoal,progressPoint;private List<PestTargets.Point> route=List.of();private int waypoint;
 private long plannedAt,lastProgress;
 GardenFlightCore(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,AutomationFeature feature){
  if(feature!=AutomationFeature.PEST&&feature!=AutomationFeature.GREENHOUSE)throw new IllegalArgumentException("No flight driver for this feature");this.coordinator=coordinator;this.session=session;this.observe=observe;this.feature=feature;
 }
 AutomationSession.Handle owner(){return owner;}Object levelIdentity(){return ownedLevel;}ItemSnapshot tool(){return preparedTool;}int slot(){return preparedSlot;}
 boolean prepare(AutomationSession.Handle h,int slot,ItemSnapshot tool,FlightAnchor anchor,int radius,long now){
  if(slot<0||slot>8||tool==null||!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.HOTBAR))return false;
  var mc=mc();var p=mc.player;if(p.isPassenger()||p.isSleeping()||p.isUsingItem()||!PestIdentity.sameTool(tool,AutomationGameView.item(p.getInventory().getItem(slot),true)))return false;
  owner=h;ownedLevel=mc.level;preparedTool=tool;preparedSlot=slot;bounds=new PestFlightBounds(anchor.position(),radius);search=null;route=List.of();searchGoal=null;progressPoint=point(p.position());lastProgress=now;
  p.getInventory().setSelectedSlot(slot);if(!p.getAbilities().flying){p.getAbilities().flying=true;p.onUpdateAbilities();}return true;
 }
public AutomationContext context(){return observe.get();}
 private Minecraft mc(){return Minecraft.getInstance();}
 private static PestTargets.Point point(Vec3 p){return new PestTargets.Point(p.x,p.y,p.z);}
 private static Vec3 vec(PestTargets.Point p){return new Vec3(p.x(),p.y(),p.z());}
 boolean allowed(AutomationSession.Handle h,long now,AutomationCoordinator.Resource... resources){
  var mc=mc();var context=context();if(!mc.isSameThread()||mc.player==null||mc.level==null||h==null||h.lease().owner()!=feature||!session.isCurrent(h)||!context.canIssueCommand()||!context.garden()||manualInput()||!mc.player.getAbilities().mayfly)return false;
  if(owner!=null&&(!owner.equals(h)||ownedLevel!=mc.level))return false;
  for(var resource:resources)if(!coordinator.permits(h.lease(),resource,context,now))return false;return true;
 }
 public boolean manualInput(){
  var mc=mc();if(mc.options==null||mc.getWindow()==null)return true;if(mc.gui.screen()!=null)return false;
  for(var key:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint,mc.options.keyAttack,mc.options.keyUse))if(physical(key,true))return true;
  return false;
 }
 private boolean physical(KeyMapping mapping,boolean unknown){
  if(mapping.isUnbound())return false;var key=InputConstants.getKey(mapping.saveString());long window=mc().getWindow().handle();
  if(key.getType()==InputConstants.Type.KEYSYM)return key.getValue()<0?unknown:GLFW.glfwGetKey(window,key.getValue())==GLFW.GLFW_PRESS;
  if(key.getType()==InputConstants.Type.MOUSE)return GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS;
  return unknown; // Unsupported scancodes fail closed, never mistake our injected isDown for a human.
 }
 public void dispose(AutomationSession.Handle h){
  if(h==null||owner==null||!owner.equals(h))return;
  release(h);
  // Legacy safe-stop may have cleared these after release; honor real physical input last.
  var mc=mc();if(mc.options!=null&&mc.getWindow()!=null)for(var key:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift))key.setDown(mc.gui.screen()==null&&physical(key,false));
  owner=null;ownedLevel=null;preparedTool=null;bounds=null;search=null;searchGoal=null;route=List.of();
 }
 private void keys(boolean forward,boolean back,boolean up,boolean down){
  var o=mc().options;set(o.keyUp,forward);set(o.keyDown,back);set(o.keyLeft,false);set(o.keyRight,false);set(o.keyJump,up);set(o.keyShift,down);
 }
 private void set(KeyMapping key,boolean down){key.setDown(down);if(down)pressed.add(key);else pressed.remove(key);}
 public void release(AutomationSession.Handle h){
  if(h==null||owner==null||!owner.equals(h))return;
  // Restoring physical state is cleanup, not permission to mutate look/tool or new task state.
  var mc=mc();for(var key:pressed)key.setDown(mc.getWindow()!=null&&mc.gui.screen()==null&&physical(key,false));pressed.clear();
 }
 public FlightMotion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long now){
  if(!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.LOOK)||owner==null)return FlightMotion.BLOCKED;
  var p=mc().player;var current=point(p.position());
  if(!p.getAbilities().flying||p.isPassenger()||!Double.isFinite(tolerance)||tolerance<.1||tolerance>1)return FlightMotion.BLOCKED;
  if(current.distanceSquared(goal)<=tolerance*tolerance*.25&&p.getDeltaMovement().lengthSqr()<.000225){if(!clear(current,current)){release(h);return FlightMotion.BLOCKED;}release(h);search=null;route=List.of();searchGoal=null;return FlightMotion.ARRIVED;}
  if(progressPoint==null||current.distanceSquared(progressPoint)>=.16){progressPoint=current;lastProgress=now;}
  if(now-lastProgress<0||now-lastProgress>=5_000_000_000L){release(h);return FlightMotion.BLOCKED;}
  if(searchGoal==null||searchGoal.distanceSquared(goal)>1||!route.isEmpty()&&now-plannedAt>=2_000_000_000L){
   release(h);searchGoal=goal;search=new PestFlightPath(h.lease().worldId(),current,goal,tolerance,96,2048,this::clear,now);route=List.of();waypoint=0;
  }
  if(route.isEmpty()){
   search.step(context().worldId(),now,16);
   if(search.state()==PestFlightPath.State.SEARCHING){release(h);return FlightMotion.MOVING;}
   var found=search.route(context().worldId(),now);if(found.isEmpty()){release(h);return FlightMotion.BLOCKED;}
   route=found.get();waypoint=Math.min(1,route.size()-1);plannedAt=now;
  }
  while(waypoint<route.size()-1&&current.distanceSquared(route.get(waypoint))<.25)waypoint++;
  var next=waypoint==route.size()-1?goal:route.get(waypoint);if(!clear(current,next)){release(h);searchGoal=null;return FlightMotion.MOVING;}
  var input=PestSteering.toward(current,next,point(p.getDeltaMovement()),p.getYRot(),p.getXRot(),12);
  if(!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.LOOK))return FlightMotion.BLOCKED;
  p.setYRot(input.yaw());p.setXRot(input.pitch());keys(input.forward(),input.back(),input.up(),input.down());return FlightMotion.MOVING;
 }
 boolean clear(PestTargets.Point from,PestTargets.Point to){
  var mc=mc();if(mc.level==null||mc.player==null||bounds==null||!bounds.permits(from,to)||from.distanceSquared(to)>16)return false;
  var p=mc.player;var origin=point(p.position());var body=p.getBoundingBox().move(from.x()-origin.x(),from.y()-origin.y(),from.z()-origin.z()).deflate(.005);
  var swept=body.expandTowards(to.x()-from.x(),to.y()-from.y(),to.z()-from.z());
  if(!loaded(swept))return false;
  for(var block:BlockPos.betweenClosed((int)Math.floor(swept.minX),(int)Math.floor(swept.minY),(int)Math.floor(swept.minZ),(int)Math.floor(swept.maxX),(int)Math.floor(swept.maxY),(int)Math.floor(swept.maxZ))){
   var state=mc.level.getBlockState(block);
   if(!state.getFluidState().isEmpty()||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.CACTUS)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE)||state.is(Blocks.POWDER_SNOW))return false;
  }
  return mc.level.noCollision(p,swept);
 }
 boolean loaded(AABB box){
  var level=mc().level;if(level==null||box.minY<level.getMinY()||box.maxY>level.getMaxY()||box.getXsize()*box.getYsize()*box.getZsize()>4096)return false;
  for(int x=(int)Math.floor(box.minX)>>4;x<=(int)Math.floor(box.maxX)>>4;x++)for(int z=(int)Math.floor(box.minZ)>>4;z<=(int)Math.floor(box.maxZ)>>4;z++)if(!level.hasChunkAt(new BlockPos(x<<4,(int)Math.floor(box.minY),z<<4)))return false;
  return true;
 }
 public FlightMotion restore(AutomationSession.Handle h,FlightAnchor anchor,long now){
  release(h);
  if(!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.HOTBAR)||owner==null)return FlightMotion.BLOCKED;
  var mc=mc();var p=mc.player;if(point(p.position()).distanceSquared(anchor.position())>.35*.35||!clear(point(p.position()),point(p.position()))||!PestIdentity.sameTool(anchor.item(),AutomationGameView.item(p.getInventory().getItem(anchor.slot()),true)))return FlightMotion.BLOCKED;
  float yaw=PestSteering.rotate(p.getYRot(),anchor.yaw(),12,true),pitch=PestSteering.rotate(p.getXRot(),anchor.pitch(),12,false);p.setYRot(yaw);p.setXRot(pitch);
  if(Math.abs(PestSteering.rotate(yaw,anchor.yaw(),30,true)-yaw)>1||Math.abs(pitch-anchor.pitch())>1)return FlightMotion.MOVING;
  if(!anchor.flying()){
   var below=p.getBoundingBox().move(0,-.15,0);if(!loaded(below)||mc.level.noCollision(p,below))return FlightMotion.BLOCKED;
   p.getAbilities().flying=false;p.onUpdateAbilities();
  }
  p.setYRot(anchor.yaw());p.setXRot(anchor.pitch());p.getInventory().setSelectedSlot(anchor.slot());owner=null;ownedLevel=null;preparedTool=null;bounds=null;search=null;route=List.of();return FlightMotion.ARRIVED;
 }
}
