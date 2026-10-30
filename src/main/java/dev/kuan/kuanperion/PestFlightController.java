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
 public interface ExchangeOpener {boolean open(AutomationSession.Handle handle,long now);}
 public interface Travel {boolean send(AutomationSession.Handle handle,PestPlot plot,long now);}
 private final Travel travel;private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final GardenFlightCore flight;private final PestBarnInteraction barn;
 private final InventoryReceipts baitItems;private final CursorReceipts baitCursors;private final BazaarAcquisitionController.Opener baitOpener;private final java.util.function.LongSupplier baitPurse;private final ExchangeOpener exchangeOpener;
 private HypixelItemCatalogClient baitCatalog;private HypixelMarketClient baitPrices;private AutomationSession.Handle baitDataOwner,baitBuyingOwner;private Object baitDataLevel;private long baitDataRequestedAt;private PestBaitPolicy baitBuyingPolicy;private SpendingBudget baitBuyingBudget;private int baitBuyingRemaining;private BazaarAcquisitionController baitAcquisition;
 private PestGameView.Snapshot latestFacts;private PestIdentity.Vacuum preparedTool;private long lastUse;private boolean used;
 public PestFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe){this(coordinator,session,observe,(h,p,n)->false);}
 public PestFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel){this(coordinator,session,observe,travel,null,null,null,null);}
 public PestFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel,InventoryReceipts items,CursorReceipts cursors,BazaarAcquisitionController.Opener opener,java.util.function.LongSupplier purse){this(coordinator,session,observe,travel,items,cursors,opener,purse,(h,n)->false);}
 public PestFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel,InventoryReceipts items,CursorReceipts cursors,BazaarAcquisitionController.Opener opener,java.util.function.LongSupplier purse,ExchangeOpener exchangeOpener){this.exchangeOpener=Objects.requireNonNull(exchangeOpener);this.coordinator=coordinator;this.session=session;this.observe=observe;this.travel=travel;this.baitItems=items;this.baitCursors=cursors;this.baitOpener=opener;this.baitPurse=purse;flight=new GardenFlightCore(coordinator,session,observe,AutomationFeature.PEST);barn=new PestBarnInteraction(session,coordinator,new PestBarnGameView(coordinator,session,flight,()->preparedTool));}
 private boolean allowed(AutomationSession.Handle h,long now,AutomationCoordinator.Resource... resources){return flight.allowed(h,now,resources);}
 public boolean manualInput(){return flight.manualInput();}
 static FlightAnchor flightAnchor(PestTask.Anchor anchor){return new FlightAnchor(anchor.position(),anchor.yaw(),anchor.pitch(),anchor.slot(),anchor.item(),anchor.flying());}
 static PestTask.Motion pestMotion(FlightMotion motion){return switch(motion){case MOVING->PestTask.Motion.MOVING;case ARRIVED->PestTask.Motion.ARRIVED;case BLOCKED->PestTask.Motion.BLOCKED;};}
 public boolean prepare(AutomationSession.Handle h,PestIdentity.Vacuum tool,PestTask.Anchor anchor,int radius,long now){
  if(tool==null||!allowed(h,now,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.HOTBAR)||!PestIdentity.sameVacuum(tool,AutomationGameView.item(mc().player.getInventory().getItem(tool.slot()),true)))return false;
  if(!flight.prepare(h,tool.slot(),tool.item(),flightAnchor(anchor),radius,now))return false;preparedTool=tool;used=false;return true;
 }
 public void afterStop(AutomationSession.Handle h){flight.afterStop(h);}
 public void dispose(AutomationSession.Handle h){
  if(h==null)return;
  try{if(flight.owner()!=null&&flight.owner().equals(h)){flight.dispose(h);latestFacts=null;preparedTool=null;}}
  finally{AutomationStops.run(()->barn.dispose(h),()->{if(h.equals(baitDataOwner))AutomationStops.run(this::cancelBaitAcquisition,this::shutdownBaitData);});}
 }
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
 /** Fresh read-only full frame; the private transfer proof and narrow native sender independently guard every effect. */
 public PestBaitButtonTransfer.Frame baitFrame(long now){
  var mc=mc();var c=context();var facts=PestGameView.read(mc,c,now);
  var cursor=mc.player==null?new ItemSnapshot("","minecraft:air","","","",0,List.of(),true):AutomationGameView.item(mc.player.containerMenu.getCarried(),false);
  return new PestBaitButtonTransfer.Frame(c.worldId(),now,mc.level,mc.player==null?null:mc.player.containerMenu,AutomationGameView.menu(mc,c.worldId()),VisitorGameView.inventory(mc),cursor,facts.traps(),facts.trapTargets());
 }
 public GuardedMenuAction.Result baitClick(AutomationSession.Handle h,PestBaitButtonTransfer.Action action,long now){
  var mc=mc();
  if(h==null||h.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(h)||flight.owner()==null||!flight.owner().equals(h)||flight.levelIdentity()!=mc.level||!mc.isSameThread()||mc.player==null||mc.gameMode==null||mc.gui.overlay()!=null||manualInput()||!mc.player.isAlive()||mc.player.isSpectator()||mc.player.isPassenger()||mc.player.isSleeping()||mc.player.isUsingItem()||!mc.player.getAbilities().mayfly||preparedTool==null||mc.player.getInventory().getSelectedSlot()!=preparedTool.slot()||!PestIdentity.sameVacuum(preparedTool,AutomationGameView.item(mc.player.getMainHandItem(),true)))return GuardedMenuAction.Result.REJECTED;
  return AutomationMenuActions.sendPestBaitNative(h,action,baitFrame(now),now)?GuardedMenuAction.Result.SENT:GuardedMenuAction.Result.REJECTED;
 }
 public PestExchangeTask.Frame exchangeFrame(long now){
  var mc=mc();var c=context();var player=mc.player;var cursor=player==null?new ItemSnapshot("","minecraft:air","","","",0,List.of(),true):AutomationGameView.item(player.containerMenu.getCarried(),false);
  return new PestExchangeTask.Frame(c.worldId(),now,mc.level,player==null?null:player.containerMenu,AutomationGameView.menu(mc,c.worldId()),VisitorGameView.inventory(mc),cursor,player==null?-1:player.getInventory().getSelectedSlot());
 }
 private boolean exchangeNativeAllowed(AutomationSession.Handle h){
  var c=context();var mc=mc();var player=mc.player;
  return h!=null&&h.lease().owner()==AutomationFeature.PEST&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&h.equals(flight.owner())&&flight.levelIdentity()==mc.level&&mc.isSameThread()&&player!=null&&mc.gameMode!=null&&mc.gui.overlay()==null&&!manualInput()&&player.isAlive()&&!player.isSpectator()&&!player.isPassenger()&&!player.isSleeping()&&!player.isUsingItem()&&player.getAbilities().mayfly&&preparedTool!=null&&player.getInventory().getSelectedSlot()==preparedTool.slot()&&PestIdentity.sameVacuum(preparedTool,AutomationGameView.item(player.getMainHandItem(),true))&&c.worldId()==h.lease().worldId()&&c.garden()&&(h.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU);
 }
 public PestBarnTarget.Facts barnTargets(long now){return PestBarnGameView.read(mc(),context(),now);}
 public PestTask.BarnReady bindBarnExchange(AutomationSession.Handle h,PestBarnTarget target,long now){return barn.bind(h,target,now);}
 public boolean openExchange(AutomationSession.Handle h,long now){if(!exchangeNativeAllowed(h))return false;if(barn.uses(h))return barn.open(h,now);return allowed(h,now,AutomationCoordinator.Resource.COMMAND)&&exchangeOpener.open(h,now);}
 public GuardedMenuAction.Result exchangeClick(AutomationSession.Handle h,PestExchangeTask.Action action,long now){
  if(!exchangeNativeAllowed(h))return GuardedMenuAction.Result.REJECTED;
  try{return AutomationMenuActions.sendPestExchangeNative(h,action,exchangeFrame(now),now)?GuardedMenuAction.Result.SENT:GuardedMenuAction.Result.REJECTED;}catch(RuntimeException ambiguous){return GuardedMenuAction.Result.UNCERTAIN;}
 }
 public boolean closeExchange(AutomationSession.Handle h,PestExchangeTask.Action action,long now){return exchangeNativeAllowed(h)&&AutomationMenuActions.closePestExchangeNative(h,action,exchangeFrame(now),now);}
 /** Demand-only official data; a pre-existing root and explicit native product/count/whole-run budget are mandatory. */
 public boolean beginBaitAcquisition(AutomationSession.Handle h,PestBaitPolicy policy,SpendingBudget budget,int remaining,long now){
  if(!mc().isSameThread()||baitBuyingOwner!=null||baitItems==null||baitCursors==null||baitOpener==null||baitPurse==null||h==null||h.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(h)||!h.equals(flight.owner())||flight.levelIdentity()!=mc().level||!flight.allowed(h,now,AutomationCoordinator.Resource.COMMAND)||policy==null||!policy.canBuy()||budget==null||budget.limit()!=policy.buying().maximumCoins()||budget.available()<=0||remaining<policy.buying().targetCount()||remaining>policy.rules().maximumRunItems()||baitDataOwner!=null&&!baitDataOwner.equals(h))return false;
  var frame=baitFrame(now);if(frame.menu()!=null||frame.cursor().count()!=0||frame.inventory().size()!=43||PestBaitButtonPlan.select(policy.rules(),frame.inventory(),remaining).isPresent()||!session.isCurrent(h)||!h.equals(flight.owner())||flight.levelIdentity()!=mc().level)return false;
  baitDataOwner=h;baitBuyingOwner=h;baitDataLevel=mc().level;baitDataRequestedAt=now;baitBuyingPolicy=policy;baitBuyingBudget=budget;baitBuyingRemaining=remaining;baitAcquisition=null;
  if(baitCatalog==null)baitCatalog=new HypixelItemCatalogClient();if(baitPrices==null)baitPrices=new HypixelMarketClient();baitCatalog.refresh();baitPrices.refresh();return true;
 }
 private boolean baitAcquisitionCurrent(AutomationSession.Handle h){
  if(!mc().isSameThread()||h==null||!h.equals(baitBuyingOwner)||!h.equals(baitDataOwner)||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||mc().level!=baitDataLevel||flight.levelIdentity()!=baitDataLevel||!h.equals(flight.owner()))return false;
  var c=context();return h.equals(baitBuyingOwner)&&session.isCurrent(h)&&mc().level==baitDataLevel&&c.worldId()==h.lease().worldId()&&c.garden()&&(h.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU);
 }
 public PestTask.Acquisition pollBaitAcquisition(AutomationSession.Handle h,long now){
  if(!baitAcquisitionCurrent(h))return PestTask.Acquisition.FAILED;
  if(baitAcquisition==null){
   var inventory=VisitorGameView.inventory(mc());
   if(PestBaitButtonPlan.select(baitBuyingPolicy.rules(),inventory,baitBuyingRemaining).isPresent()){cancelBaitAcquisition();return PestTask.Acquisition.COMPLETE;}
   var catalog=baitCatalog.latest();var prices=baitPrices.latest();
   if(catalog.isEmpty()||prices.isEmpty())return now-baitDataRequestedAt>=0&&now-baitDataRequestedAt<12_000_000_000L?PestTask.Acquisition.RUNNING:PestTask.Acquisition.FAILED;
   var need=new BazaarAcquisitionPlan.Need(baitBuyingPolicy.rules().baitId(),baitBuyingPolicy.buying().targetCount());
   var plan=BazaarAcquisitionPlan.plan(List.of(need),inventory,baitBuyingPolicy.buying().rules(need.productId()),prices.get(),catalog.get(),baitBuyingBudget,System.currentTimeMillis()).orElse(null);
   if(plan==null||plan.requests().size()!=1||!baitAcquisitionCurrent(h))return PestTask.Acquisition.FAILED;
   baitAcquisition=new BazaarAcquisitionController(coordinator,session,observe,baitItems,baitCursors,h,plan,baitBuyingBudget,baitOpener,baitPurse);
   if(!baitAcquisition.begin(now))return PestTask.Acquisition.FAILED;
  }
  baitAcquisition.tick(now);
  return switch(baitAcquisition.state()){
   case COMPLETE->{var current=baitFrame(now);boolean ready=baitAcquisitionCurrent(h)&&current.menu()==null&&current.cursor().count()==0&&PestBaitButtonPlan.select(baitBuyingPolicy.rules(),current.inventory(),baitBuyingRemaining).isPresent();cancelBaitAcquisition();yield ready?PestTask.Acquisition.COMPLETE:PestTask.Acquisition.FAILED;}
   case FAILED->PestTask.Acquisition.FAILED;case UNCERTAIN->PestTask.Acquisition.UNCERTAIN;default->PestTask.Acquisition.RUNNING;
  };
 }
 public boolean ownsBaitAcquisitionScreen(){return baitAcquisition!=null&&baitAcquisition.ownsScreen();}
 public void baitAcquisitionMessage(long world,String text){if(baitAcquisition!=null)baitAcquisition.onSystemMessage(world,text);}
 public void cancelBaitAcquisition(){var previous=baitAcquisition;baitAcquisition=null;baitBuyingOwner=null;baitBuyingPolicy=null;baitBuyingBudget=null;baitBuyingRemaining=0;if(previous!=null)previous.cancel();}
 private void shutdownBaitData(){var catalog=baitCatalog;var prices=baitPrices;baitCatalog=null;baitPrices=null;baitDataOwner=null;baitDataLevel=null;AutomationStops.run(()->{if(catalog!=null)catalog.close();},()->{if(prices!=null)prices.close();});}
 public boolean closeBaitMenu(Object identity,int id){
  var mc=mc();var h=flight.owner();var c=context();
  if(h==null||!session.isCurrent(h)||h.lease().owner()!=AutomationFeature.PEST||identity==null||!mc.isSameThread()||mc.player==null||mc.level!=flight.levelIdentity()||c.worldId()!=h.lease().worldId()||!c.garden()||c.blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU)||manualInput()||mc.player.containerMenu!=identity||mc.player.containerMenu.containerId!=id||!mc.player.containerMenu.getCarried().isEmpty())return false;
  mc.player.closeContainer();return mc.player.containerMenu==mc.player.inventoryMenu&&mc.gui.screen()==null&&mc.player.containerMenu.getCarried().isEmpty();
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
