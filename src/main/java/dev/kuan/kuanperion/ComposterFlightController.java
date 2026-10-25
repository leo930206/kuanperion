// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.composter.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;import java.util.function.Supplier;import net.minecraft.client.Minecraft;import net.minecraft.world.InteractionHand;import net.minecraft.core.BlockPos;import net.minecraft.world.level.ClipContext;import net.minecraft.world.phys.*;
/** Fixed COMPOSTER native boundary; no purchase, sack slot assumption or cursor repair. */
public final class ComposterFlightController implements ComposterTask.Port {
 public interface Travel{boolean send(AutomationSession.Handle handle,PestPlot plot,long now);}
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final GardenFlightCore flight;private final Travel travel;
 public ComposterFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel){this.coordinator=coordinator;this.session=session;this.observe=observe;this.travel=travel;flight=new GardenFlightCore(coordinator,session,observe,AutomationFeature.COMPOSTER);}
 private Minecraft mc(){return Minecraft.getInstance();}private static PestTargets.Point point(Vec3 p){return new PestTargets.Point(p.x,p.y,p.z);}
 public AutomationContext context(){return observe.get();}public boolean manualInput(){return flight.manualInput();}public PestTargets.Point position(){return mc().player==null?null:point(mc().player.position());}
 public ComposterTask.Frame frame(long now){
  var mc=mc();var context=context();var p=mc.player;var zero=new PestTargets.Point(0,0,0);var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
  if(p==null||mc.level==null||!mc.isSameThread())return new ComposterTask.Frame(context.worldId(),now,new FlightAnchor(zero,0,0,0,empty,false),zero,false,true,false,false);
  var pose=new FlightAnchor(point(p.position()),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),AutomationGameView.item(p.getMainHandItem(),true),p.getAbilities().flying);
  return new ComposterTask.Frame(context.worldId(),now,pose,point(p.getEyePosition()),p.getAbilities().mayfly,manualInput(),p.containerMenu.getCarried().isEmpty(),!p.isSpectator()&&!p.isPassenger()&&!p.isUsingItem()&&!p.isSleeping());
 }
 public boolean prepare(AutomationSession.Handle h,FlightAnchor original,FlightAnchor region,long now){return flight.prepare(h,original.slot(),original.item(),region,64,now);}
 public List<ComposterPlan.Stack> inventory(){return AutomationGameView.inventory(mc()).stream().filter(s->s.index()<36).map(s->new ComposterPlan.Stack(s.index(),s.item())).toList();}
 public MenuSnapshot menu(){return AutomationGameView.menu(mc(),context().worldId());}
 public FlightMotion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long now){return flight.move(h,goal,tolerance,now);}
 public FlightMotion restore(AutomationSession.Handle h,FlightAnchor original,long now){return flight.restore(h,original,now);}
 public void release(AutomationSession.Handle h){flight.release(h);}public void afterStop(AutomationSession.Handle h){flight.afterStop(h);}
 public void dispose(AutomationSession.Handle h){flight.dispose(h);}
 public boolean travel(AutomationSession.Handle h,PestPlot plot,long now){return plot!=null&&flight.allowed(h,now,AutomationCoordinator.Resource.COMMAND)&&travel.send(h,plot,now);}
 public ComposterTask.Open open(AutomationSession.Handle h,ComposterPolicy.Anchor machine,long now){
  release(h);if(machine==null||!flight.allowed(h,now,AutomationCoordinator.Resource.ATTACK,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.HOTBAR)||flight.owner()==null||!ComposterGameView.valid(mc(),context(),machine))return ComposterTask.Open.REJECTED;
  var mc=mc();var p=mc.player;if(mc.gameMode==null||p.isSpectator()||p.isPassenger()||!p.getAbilities().flying||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty()||p.getInventory().getSelectedSlot()!=flight.slot()||!PestIdentity.sameTool(flight.tool(),AutomationGameView.item(p.getMainHandItem(),true))||!flight.clear(point(p.position()),point(p.position())))return ComposterTask.Open.REJECTED;
  Vec3 eye=p.getEyePosition(),aim=new Vec3(machine.x()+.5,machine.y()+.25,machine.z()+.5);if(eye.distanceToSqr(aim)>2.75*2.75||!flight.loaded(new AABB(eye,aim).inflate(.01)))return ComposterTask.Open.REJECTED;
  var hit=mc.level.clip(new ClipContext(eye,aim,ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,p));if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(new BlockPos(machine.x(),machine.y(),machine.z())))return ComposterTask.Open.REJECTED;
  double dx=aim.x-eye.x,dy=aim.y-eye.y,dz=aim.z-eye.z;float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));float nextYaw=PestSteering.rotate(p.getYRot(),yaw,12,true),nextPitch=PestSteering.rotate(p.getXRot(),pitch,12,false);p.setYRot(nextYaw);p.setXRot(nextPitch);
  if(Math.abs(PestSteering.rotate(nextYaw,yaw,30,true)-nextYaw)>3||Math.abs(nextPitch-pitch)>3)return ComposterTask.Open.AIMING;
  if(!flight.allowed(h,now,AutomationCoordinator.Resource.ATTACK)||!mc.gameMode.startDestroyBlock(hit.getBlockPos(),hit.getDirection()))return ComposterTask.Open.REJECTED;p.swing(InteractionHand.MAIN_HAND);return ComposterTask.Open.SENT;
 }
 public boolean deposit(AutomationSession.Handle h,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection protection,long now){return flight.owner()!=null&&flight.owner().equals(h)&&flight.levelIdentity()==mc().level&&session.isCurrent(h)&&h.lease().owner()==AutomationFeature.COMPOSTER&&AutomationMenuActions.inventoryAction(coordinator,h.lease(),contract,ticket,protection,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,this::context,this::menu,now);}
 public void closeOwnedMenu(int id){var mc=mc();var menu=menu();if(flight.owner()!=null&&session.isCurrent(flight.owner())&&flight.levelIdentity()==mc.level&&menu!=null&&menu.worldId()==flight.owner().lease().worldId()&&menu.containerId()==id&&ComposterObservation.inspect(menu).isPresent()&&mc.player!=null&&mc.player.containerMenu.getCarried().isEmpty())mc.player.closeContainer();}
}
