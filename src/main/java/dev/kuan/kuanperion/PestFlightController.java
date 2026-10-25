// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.flight.*;
import dev.kuan.kuanperion.automation.pest.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Pest-specific observations/use/traps; primitive flight remains one separately leased fixed-feature core. */
public final class PestFlightController implements PestTask.Port {
 public interface Travel {boolean send(AutomationSession.Handle handle,PestPlot plot,long now);}
 private final Travel travel;private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final GardenFlightCore flight;
 private PestGameView.Snapshot latestFacts;private PestIdentity.Vacuum preparedTool;private long lastUse;private boolean used;
 public PestFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe){this(coordinator,session,observe,(h,p,n)->false);}
 public PestFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel){this.coordinator=coordinator;this.session=session;this.observe=observe;this.travel=travel;flight=new GardenFlightCore(coordinator,session,observe,AutomationFeature.PEST);}
 private boolean allowed(AutomationSession.Handle h,long now,AutomationCoordinator.Resource... resources){return flight.allowed(h,now,resources);}
 public boolean manualInput(){return flight.manualInput();}
 static FlightAnchor flightAnchor(PestTask.Anchor anchor){return new FlightAnchor(anchor.position(),anchor.yaw(),anchor.pitch(),anchor.slot(),anchor.item(),anchor.flying());}
 static PestTask.Motion pestMotion(FlightMotion motion){return switch(motion){case MOVING->PestTask.Motion.MOVING;case ARRIVED->PestTask.Motion.ARRIVED;case BLOCKED->PestTask.Motion.BLOCKED;};}
 public boolean prepare(AutomationSession.Handle h,PestIdentity.Vacuum tool,PestTask.Anchor anchor,int radius,long now){
  if(tool==null||!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.HOTBAR)||!PestIdentity.sameVacuum(tool,AutomationGameView.item(mc().player.getInventory().getItem(tool.slot()),true)))return false;
  if(!flight.prepare(h,tool.slot(),tool.item(),flightAnchor(anchor),radius,now))return false;preparedTool=tool;used=false;return true;
 }
 public void afterStop(AutomationSession.Handle h){flight.afterStop(h);}
 public void dispose(AutomationSession.Handle h){if(h==null||flight.owner()==null||!flight.owner().equals(h))return;flight.dispose(h);latestFacts=null;preparedTool=null;}
 public void release(AutomationSession.Handle h){flight.release(h);}
 public PestTask.Motion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long now){return pestMotion(flight.move(h,goal,tolerance,now));}
 public PestTask.Motion restore(AutomationSession.Handle h,PestTask.Anchor anchor,long now){return pestMotion(flight.restore(h,flightAnchor(anchor),now));}
public PestTargets.Point position(){return mc().player==null?null:point(mc().player.position());}
 public boolean travel(AutomationSession.Handle h,PestPlot plot,long now){return plot!=null&&allowed(h,now,AutomationCoordinator.Resource.COMMAND)&&travel.send(h,plot,now);}
 public AutomationContext context(){return observe.get();}
 private Minecraft mc(){return Minecraft.getInstance();}
 private static PestTargets.Point point(Vec3 p){return new PestTargets.Point(p.x,p.y,p.z);}
 private static Vec3 vec(PestTargets.Point p){return new Vec3(p.x(),p.y(),p.z());}
 public PestTask.Frame frame(long now){
  var mc=mc();var context=context();var facts=PestGameView.read(mc,context,now);latestFacts=facts;var p=mc.player;
  var zero=new PestTargets.Point(0,0,0);var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
  if(p==null)return new PestTask.Frame(context.worldId(),now,zero,zero,0,0,-1,empty,false,false,true,-1,List.of(),List.of(),false);
  return new PestTask.Frame(facts.world(),facts.observedAt(),point(p.position()),point(p.getEyePosition()),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),AutomationGameView.item(p.getMainHandItem(),true),p.getAbilities().mayfly,p.getAbilities().flying,manualInput(),facts.tab().alive(),facts.targets(),facts.vacuums(),facts.complete()).withPlots(facts.tab().plots(),facts.tab().plotsKnown());
 }
 public PestTrapRelease.Facts facts(long now){
  var context=context();var facts=latestFacts;
  if(facts==null||facts.world()!=context.worldId()||facts.observedAt()!=now)facts=PestGameView.read(mc(),context,now);
  return new PestTrapRelease.Facts(facts.world(),facts.observedAt(),facts.tab().alive(),facts.traps(),facts.trapTargets(),facts.targets().stream().map(PestTargets.Target::uuid).collect(java.util.stream.Collectors.toUnmodifiableSet()));
 }
 public boolean cursorEmpty(){return mc().player!=null&&mc().player.containerMenu.getCarried().isEmpty();}
 public MenuSnapshot menu(){return AutomationGameView.menu(mc(),context().worldId());}
 public boolean release(AutomationSession.Handle h,MenuContract contract,MenuContract.Ticket ticket,long now){
  return flight.owner()!=null&&flight.owner().equals(h)&&session.isCurrent(h)&&h.lease().owner()==AutomationFeature.PEST&&AutomationMenuActions.click(coordinator,h.lease(),contract,ticket,GuardedMenuAction.Space.REQUIRE_FREE_SLOT,this::context,this::menu,now);
 }
 public void closeOwnedMenu(int id){
  var mc=mc();var visible=menu();if(flight.owner()!=null&&session.isCurrent(flight.owner())&&flight.levelIdentity()==mc.level&&visible!=null&&visible.worldId()==flight.owner().lease().worldId()&&visible.containerId()==id&&mc.player!=null&&mc.player.containerMenu.getCarried().isEmpty())mc.player.closeContainer();
 }
 public PestTrapRelease.Open open(AutomationSession.Handle h,PestTrapTarget target,long now){
  release(h);
  if(target==null||!allowed(h,now,AutomationCoordinator.Resource.USE,AutomationCoordinator.Resource.LOOK)||flight.owner()==null||!flight.owner().equals(h))return PestTrapRelease.Open.REJECTED;
  var mc=mc();var p=mc.player;if(mc.gameMode==null||p.isSpectator()||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty()||preparedTool==null||!PestIdentity.sameVacuum(preparedTool,AutomationGameView.item(p.getMainHandItem(),true))||!flight.clear(point(p.position()),point(p.position())))return PestTrapRelease.Open.REJECTED;
  var entity=mc.level.getEntity(target.entityId());
  if(!(entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand)||stand.isRemoved()||!stand.hasCustomName()||!stand.getUUID().equals(target.uuid())||PestGameView.trapMarker(stand.getId(),stand.getUUID(),stand.getCustomName().getString(),stand.position().add(0,stand.getEyeHeight()-.5,0)).filter(target::equals).isEmpty())return PestTrapRelease.Open.REJECTED;
  Vec3 eye=p.getEyePosition(),aim=vec(target.position());if(eye.distanceToSqr(aim)>2.75*2.75||!flight.loaded(new AABB(eye,aim).inflate(.01))||mc.level.clip(new ClipContext(eye,aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p)).getType()!=HitResult.Type.MISS)return PestTrapRelease.Open.REJECTED;
  double dx=aim.x-eye.x,dy=aim.y-eye.y,dz=aim.z-eye.z;float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
  float nextYaw=PestSteering.rotate(p.getYRot(),yaw,12,true),nextPitch=PestSteering.rotate(p.getXRot(),pitch,12,false);p.setYRot(nextYaw);p.setXRot(nextPitch);
  if(Math.abs(PestSteering.rotate(nextYaw,yaw,30,true)-nextYaw)>3||Math.abs(nextPitch-pitch)>3)return PestTrapRelease.Open.AIMING;
  if(!allowed(h,now,AutomationCoordinator.Resource.USE))return PestTrapRelease.Open.REJECTED;
  mc.gameMode.interact(p,stand,new EntityHitResult(stand,aim),InteractionHand.MAIN_HAND);return PestTrapRelease.Open.SENT;
 }
 public PestTask.Capture capture(AutomationSession.Handle h,PestTargets.Target target,PestIdentity.Vacuum tool,long now){
  release(h);
  if(!allowed(h,now,AutomationCoordinator.Resource.USE,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.HOTBAR)||flight.owner()==null)return PestTask.Capture.REJECTED;
  var mc=mc();var p=mc.player;if(mc.gameMode==null||!p.getAbilities().flying||p.getInventory().getSelectedSlot()!=tool.slot()||!PestIdentity.sameVacuum(tool,AutomationGameView.item(p.getMainHandItem(),true)))return PestTask.Capture.REJECTED;
  var facts=PestGameView.read(mc,context(),now);if(!facts.fresh(h.lease().worldId(),now)||facts.targets().stream().noneMatch(t->t.uuid().equals(target.uuid())&&t.id()==target.id()&&t.markerId()==target.markerId()&&t.position().equals(target.position())))return PestTask.Capture.REJECTED;
  if(!flight.clear(point(p.position()),point(p.position())))return PestTask.Capture.REJECTED;
  Vec3 eye=p.getEyePosition(),aim=vec(target.position());if(eye.distanceToSqr(aim)>tool.range()*tool.range())return PestTask.Capture.REJECTED;
  var beam=new AABB(eye,aim).inflate(.01);if(!flight.loaded(beam)||mc.level.clip(new ClipContext(eye,aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p)).getType()!=HitResult.Type.MISS)return PestTask.Capture.REJECTED;
  double dx=aim.x-eye.x,dy=aim.y-eye.y,dz=aim.z-eye.z;float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
  float nextYaw=PestSteering.rotate(p.getYRot(),yaw,12,true),nextPitch=PestSteering.rotate(p.getXRot(),pitch,12,false);
  p.setYRot(nextYaw);p.setXRot(nextPitch);
  if(Math.abs(PestSteering.rotate(nextYaw,yaw,30,true)-nextYaw)>3||Math.abs(nextPitch-pitch)>3)return PestTask.Capture.AIMING;
  if(used&&now-lastUse<250_000_000L)return PestTask.Capture.AIMING;
  if(!allowed(h,now,AutomationCoordinator.Resource.USE))return PestTask.Capture.REJECTED;
  mc.gameMode.useItem(p,InteractionHand.MAIN_HAND);lastUse=now;used=true;return PestTask.Capture.USING;
 }
}
