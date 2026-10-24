// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;

import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.pest.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import org.lwjgl.glfw.GLFW;

/** Native legal flight only: server mayfly + standard keys/useItem, never velocity/noClip/teleport. */
public final class PestFlightController implements PestTask.Port {
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;
 private AutomationSession.Handle owner;private Object ownedLevel;private final Set<KeyMapping> pressed=new HashSet<>();
 private PestFlightPath search;private PestTargets.Point searchGoal,progressPoint;private List<PestTargets.Point> route=List.of();private int waypoint;
 private long plannedAt,lastProgress,lastUse;private boolean used;
 public PestFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe){this.coordinator=coordinator;this.session=session;this.observe=observe;}
 public AutomationContext context(){return observe.get();}
 private Minecraft mc(){return Minecraft.getInstance();}
 private static PestTargets.Point point(Vec3 p){return new PestTargets.Point(p.x,p.y,p.z);}
 private static Vec3 vec(PestTargets.Point p){return new Vec3(p.x(),p.y(),p.z());}
 public PestTask.Frame frame(long now){
  var mc=mc();var context=context();var facts=PestGameView.read(mc,context,now);var p=mc.player;
  var zero=new PestTargets.Point(0,0,0);var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
  if(p==null)return new PestTask.Frame(context.worldId(),now,zero,zero,0,0,-1,empty,false,false,true,-1,List.of(),List.of(),false);
  return new PestTask.Frame(facts.world(),facts.observedAt(),point(p.position()),point(p.getEyePosition()),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),AutomationGameView.item(p.getMainHandItem(),true),p.getAbilities().mayfly,p.getAbilities().flying,manualInput(),facts.tab().alive(),facts.targets(),facts.vacuums(),facts.complete());
 }
 private boolean allowed(AutomationSession.Handle h,long now,AutomationCoordinator.Resource... resources){
  var mc=mc();var context=context();if(!mc.isSameThread()||mc.player==null||mc.level==null||h==null||h.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(h)||!context.canIssueCommand()||!context.garden()||manualInput()||!mc.player.getAbilities().mayfly)return false;
  if(owner!=null&&(!owner.equals(h)||ownedLevel!=mc.level))return false;
  for(var resource:resources)if(!coordinator.permits(h.lease(),resource,context,now))return false;return true;
 }
 /** Physical bindings, not isDown(), which includes our own overlay. Typing in our GUI is not movement. */
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
 public boolean prepare(AutomationSession.Handle h,PestIdentity.Vacuum tool,PestTask.Anchor anchor,long now){
  if(!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.HOTBAR))return false;
  var mc=mc();var p=mc.player;if(p.isPassenger()||p.isSleeping()||p.isUsingItem()||PestGameView.vacuum(p.getInventory().getItem(tool.slot()),tool.slot()).filter(tool::equals).isEmpty())return false;
  owner=h;ownedLevel=mc.level;search=null;route=List.of();searchGoal=null;progressPoint=point(p.position());lastProgress=now;used=false;
  p.getInventory().setSelectedSlot(tool.slot());
  if(!p.getAbilities().flying){p.getAbilities().flying=true;p.onUpdateAbilities();}
  return true;
 }
 public void dispose(AutomationSession.Handle h){
  if(h==null||owner==null||!owner.equals(h))return;
  release(h);
  // Legacy safe-stop may have cleared these after release; honor real physical input last.
  var mc=mc();if(mc.options!=null&&mc.getWindow()!=null)for(var key:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift))key.setDown(mc.gui.screen()==null&&physical(key,false));
  owner=null;ownedLevel=null;search=null;searchGoal=null;route=List.of();
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
 public PestTask.Motion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long now){
  if(!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.LOOK)||owner==null)return PestTask.Motion.BLOCKED;
  var p=mc().player;var current=point(p.position());
  if(!p.getAbilities().flying||p.isPassenger()||!Double.isFinite(tolerance)||tolerance<.1||tolerance>1)return PestTask.Motion.BLOCKED;
  if(current.distanceSquared(goal)<=tolerance*tolerance*.25&&p.getDeltaMovement().lengthSqr()<.000225){if(!clear(current,current)){release(h);return PestTask.Motion.BLOCKED;}release(h);search=null;route=List.of();searchGoal=null;return PestTask.Motion.ARRIVED;}
  if(progressPoint==null||current.distanceSquared(progressPoint)>=.16){progressPoint=current;lastProgress=now;}
  if(now-lastProgress<0||now-lastProgress>=5_000_000_000L){release(h);return PestTask.Motion.BLOCKED;}
  if(searchGoal==null||searchGoal.distanceSquared(goal)>1||!route.isEmpty()&&now-plannedAt>=2_000_000_000L){
   release(h);searchGoal=goal;search=new PestFlightPath(h.lease().worldId(),current,goal,tolerance,96,2048,this::clear,now);route=List.of();waypoint=0;
  }
  if(route.isEmpty()){
   search.step(context().worldId(),now,16);
   if(search.state()==PestFlightPath.State.SEARCHING){release(h);return PestTask.Motion.MOVING;}
   var found=search.route(context().worldId(),now);if(found.isEmpty()){release(h);return PestTask.Motion.BLOCKED;}
   route=found.get();waypoint=Math.min(1,route.size()-1);plannedAt=now;
  }
  while(waypoint<route.size()-1&&current.distanceSquared(route.get(waypoint))<.25)waypoint++;
  var next=waypoint==route.size()-1?goal:route.get(waypoint);if(!clear(current,next)){release(h);searchGoal=null;return PestTask.Motion.MOVING;}
  var input=PestSteering.toward(current,next,point(p.getDeltaMovement()),p.getYRot(),p.getXRot(),12);
  if(!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.LOOK))return PestTask.Motion.BLOCKED;
  p.setYRot(input.yaw());p.setXRot(input.pitch());keys(input.forward(),input.back(),input.up(),input.down());return PestTask.Motion.MOVING;
 }
 /** Swept full player body; refuse unloaded chunks and hazardous blocks before collision access. */
 private boolean clear(PestTargets.Point from,PestTargets.Point to){
  var mc=mc();if(mc.level==null||mc.player==null||from.distanceSquared(to)>16)return false;
  var p=mc.player;var origin=point(p.position());var body=p.getBoundingBox().move(from.x()-origin.x(),from.y()-origin.y(),from.z()-origin.z()).deflate(.005);
  var swept=body.expandTowards(to.x()-from.x(),to.y()-from.y(),to.z()-from.z());
  if(!loaded(swept))return false;
  for(var block:BlockPos.betweenClosed((int)Math.floor(swept.minX),(int)Math.floor(swept.minY),(int)Math.floor(swept.minZ),(int)Math.floor(swept.maxX),(int)Math.floor(swept.maxY),(int)Math.floor(swept.maxZ))){
   var state=mc.level.getBlockState(block);
   if(!state.getFluidState().isEmpty()||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.CACTUS)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE)||state.is(Blocks.POWDER_SNOW))return false;
  }
  return mc.level.noCollision(p,swept);
 }
 private boolean loaded(AABB box){
  var level=mc().level;if(level==null||box.minY<level.getMinY()||box.maxY>level.getMaxY()||box.getXsize()*box.getYsize()*box.getZsize()>4096)return false;
  for(int x=(int)Math.floor(box.minX)>>4;x<=(int)Math.floor(box.maxX)>>4;x++)for(int z=(int)Math.floor(box.minZ)>>4;z<=(int)Math.floor(box.maxZ)>>4;z++)if(!level.hasChunkAt(new BlockPos(x<<4,(int)Math.floor(box.minY),z<<4)))return false;
  return true;
 }
 public PestTask.Capture capture(AutomationSession.Handle h,PestTargets.Target target,PestIdentity.Vacuum tool,long now){
  release(h);
  if(!allowed(h,now,AutomationCoordinator.Resource.USE,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.HOTBAR)||owner==null)return PestTask.Capture.REJECTED;
  var mc=mc();var p=mc.player;if(mc.gameMode==null||!p.getAbilities().flying||p.getInventory().getSelectedSlot()!=tool.slot()||!PestIdentity.sameVacuum(tool,AutomationGameView.item(p.getMainHandItem(),true)))return PestTask.Capture.REJECTED;
  var facts=PestGameView.read(mc,context(),now);if(!facts.fresh(h.lease().worldId(),now)||facts.targets().stream().noneMatch(t->t.uuid().equals(target.uuid())&&t.id()==target.id()&&t.markerId()==target.markerId()&&t.position().equals(target.position())))return PestTask.Capture.REJECTED;
  if(!clear(point(p.position()),point(p.position())))return PestTask.Capture.REJECTED;
  Vec3 eye=p.getEyePosition(),aim=vec(target.position());if(eye.distanceToSqr(aim)>tool.range()*tool.range())return PestTask.Capture.REJECTED;
  var beam=new AABB(eye,aim).inflate(.01);if(!loaded(beam)||mc.level.clip(new ClipContext(eye,aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p)).getType()!=HitResult.Type.MISS)return PestTask.Capture.REJECTED;
  double dx=aim.x-eye.x,dy=aim.y-eye.y,dz=aim.z-eye.z;float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
  float nextYaw=PestSteering.rotate(p.getYRot(),yaw,12,true),nextPitch=PestSteering.rotate(p.getXRot(),pitch,12,false);
  p.setYRot(nextYaw);p.setXRot(nextPitch);
  if(Math.abs(PestSteering.rotate(nextYaw,yaw,30,true)-nextYaw)>3||Math.abs(nextPitch-pitch)>3)return PestTask.Capture.AIMING;
  if(used&&now-lastUse<250_000_000L)return PestTask.Capture.AIMING;
  if(!allowed(h,now,AutomationCoordinator.Resource.USE))return PestTask.Capture.REJECTED;
  mc.gameMode.useItem(p,InteractionHand.MAIN_HAND);lastUse=now;used=true;return PestTask.Capture.USING;
 }
 public PestTask.Motion restore(AutomationSession.Handle h,PestTask.Anchor anchor,long now){
  release(h);
  if(!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.HOTBAR)||owner==null)return PestTask.Motion.BLOCKED;
  var mc=mc();var p=mc.player;if(point(p.position()).distanceSquared(anchor.position())>.35*.35||!clear(point(p.position()),point(p.position()))||!PestIdentity.sameTool(anchor.item(),AutomationGameView.item(p.getInventory().getItem(anchor.slot()),true)))return PestTask.Motion.BLOCKED;
  float yaw=PestSteering.rotate(p.getYRot(),anchor.yaw(),12,true),pitch=PestSteering.rotate(p.getXRot(),anchor.pitch(),12,false);p.setYRot(yaw);p.setXRot(pitch);
  if(Math.abs(PestSteering.rotate(yaw,anchor.yaw(),30,true)-yaw)>1||Math.abs(pitch-anchor.pitch())>1)return PestTask.Motion.MOVING;
  if(!anchor.flying()){
   var below=p.getBoundingBox().move(0,-.15,0);if(!loaded(below)||mc.level.noCollision(p,below))return PestTask.Motion.BLOCKED;
   p.getAbilities().flying=false;p.onUpdateAbilities();
  }
  p.setYRot(anchor.yaw());p.setXRot(anchor.pitch());p.getInventory().setSelectedSlot(anchor.slot());owner=null;ownedLevel=null;search=null;route=List.of();return PestTask.Motion.ARRIVED;
 }
}
