// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.greenhouse.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;import java.util.function.Supplier;
import net.minecraft.client.Minecraft;import net.minecraft.core.BlockPos;import net.minecraft.world.InteractionHand;import net.minecraft.world.entity.decoration.ArmorStand;import net.minecraft.world.level.ClipContext;import net.minecraft.world.phys.*;
/** Native normal controls only. Explicit crop/plot/tool consent is held by GreenhouseTask, never inferred here. */
public final class GreenhouseFlightController implements GreenhouseTask.Port {
 public interface Travel{boolean send(AutomationSession.Handle handle,PestPlot plot,long now);}
 private final Supplier<AutomationContext> observe;private final GardenFlightCore flight;private final Travel travel;
 public GreenhouseFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel){this.observe=observe;this.travel=travel;flight=new GardenFlightCore(coordinator,session,observe,AutomationFeature.GREENHOUSE);}
 private Minecraft mc(){return Minecraft.getInstance();}
 private static PestTargets.Point point(Vec3 p){return new PestTargets.Point(p.x,p.y,p.z);}
 private static Vec3 vec(PestTargets.Point p){return new Vec3(p.x(),p.y(),p.z());}
 public boolean manualInput(){return flight.manualInput();}
 public AutomationContext context(){return observe.get();}
 public PestTargets.Point position(){return mc().player==null?null:point(mc().player.position());}
 public GreenhouseTask.Frame frame(long now){
  var mc=mc();var context=context();var p=mc.player;var zero=new PestTargets.Point(0,0,0);var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
  if(p==null||mc.level==null||!mc.isSameThread())return new GreenhouseTask.Frame(context.worldId(),now,zero,zero,0,0,-1,empty,false,false,true,List.of(),false);
  var tools=new ArrayList<GreenhouseTask.Tool>();for(int i=0;i<9;i++)tools.add(new GreenhouseTask.Tool(i,AutomationGameView.item(p.getInventory().getItem(i),true)));
  return new GreenhouseTask.Frame(context.worldId(),now,point(p.position()),point(p.getEyePosition()),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),AutomationGameView.item(p.getMainHandItem(),true),p.getAbilities().mayfly,p.getAbilities().flying,manualInput(),tools,true);
 }
 public boolean prepare(AutomationSession.Handle h,GreenhouseTask.Tool tool,FlightAnchor anchor,int radius,long now){return tool!=null&&flight.prepare(h,tool.slot(),tool.item(),anchor,radius,now);}
 public Optional<GreenhouseScan> scan(GreenhousePolicy policy,int plot,PestTargets.Point center,long now){return policy.plots().contains(plot)?GreenhouseGameView.scan(mc(),context(),center,policy.radius(),Set.of(plot),policy.kinds(),now):Optional.empty();}
 public GreenhouseTask.TargetStatus target(GreenhouseTarget expected,long now){return GreenhouseGameView.target(mc(),expected);}
 public FlightMotion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long now){return flight.move(h,goal,tolerance,now);}
 public FlightMotion restore(AutomationSession.Handle h,FlightAnchor original,long now){return flight.restore(h,original,now);}
 public void release(AutomationSession.Handle h){flight.release(h);}
 public void afterStop(AutomationSession.Handle h){flight.afterStop(h);}
 public void dispose(AutomationSession.Handle h){flight.dispose(h);}
 public boolean travel(AutomationSession.Handle h,PestPlot plot,long now){return plot!=null&&flight.allowed(h,now,AutomationCoordinator.Resource.COMMAND)&&travel.send(h,plot,now);}
 public GreenhouseTask.Attack harvest(AutomationSession.Handle h,GreenhouseTarget target,GreenhouseTask.Tool tool,long now){
  release(h);
  if(target==null||tool==null||!flight.allowed(h,now,AutomationCoordinator.Resource.ATTACK,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.HOTBAR)||flight.owner()==null)return GreenhouseTask.Attack.REJECTED;
  var mc=mc();var p=mc.player;
  if(mc.gameMode==null||p.isSpectator()||p.isPassenger()||!p.getAbilities().flying||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty()||p.getInventory().getSelectedSlot()!=tool.slot()||!PestIdentity.sameTool(tool.item(),AutomationGameView.item(p.getMainHandItem(),true))||!flight.clear(point(p.position()),point(p.position())))return GreenhouseTask.Attack.REJECTED;
  var live=target(target,now);if(live.state()!=GreenhouseTask.TargetState.MATURE||!target.equals(live.current()))return GreenhouseTask.Attack.REJECTED;
  Vec3 eye=p.getEyePosition(),aim=vec(target.position());if(eye.distanceToSqr(aim)>2.75*2.75||!flight.loaded(new AABB(eye,aim).inflate(.01)))return GreenhouseTask.Attack.REJECTED;
  var hit=mc.level.clip(new ClipContext(eye,aim,ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,p));
  if(target.ref() instanceof GreenhouseTarget.BlockRef ref){if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(new BlockPos(ref.x(),ref.y(),ref.z())))return GreenhouseTask.Attack.REJECTED;}
  else if(hit.getType()!=HitResult.Type.MISS)return GreenhouseTask.Attack.REJECTED;
  double dx=aim.x-eye.x,dy=aim.y-eye.y,dz=aim.z-eye.z;float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
  float nextYaw=PestSteering.rotate(p.getYRot(),yaw,12,true),nextPitch=PestSteering.rotate(p.getXRot(),pitch,12,false);p.setYRot(nextYaw);p.setXRot(nextPitch);
  if(Math.abs(PestSteering.rotate(nextYaw,yaw,30,true)-nextYaw)>3||Math.abs(nextPitch-pitch)>3)return GreenhouseTask.Attack.AIMING;
  if(!flight.allowed(h,now,AutomationCoordinator.Resource.ATTACK))return GreenhouseTask.Attack.REJECTED;
  if(target.ref() instanceof GreenhouseTarget.BlockRef){if(!mc.gameMode.startDestroyBlock(hit.getBlockPos(),hit.getDirection()))return GreenhouseTask.Attack.REJECTED;}
  else{var ref=(GreenhouseTarget.EntityRef)target.ref();var entity=mc.level.getEntity(ref.id());if(!(entity instanceof ArmorStand stand)||stand.isRemoved()||!stand.getUUID().equals(ref.uuid()))return GreenhouseTask.Attack.REJECTED;mc.gameMode.attack(p,stand);}
  p.swing(InteractionHand.MAIN_HAND);return GreenhouseTask.Attack.ISSUED;
 }
}
