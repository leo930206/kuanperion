// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.visitor.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;import java.util.concurrent.CompletableFuture;import java.util.function.Supplier;import net.minecraft.client.Minecraft;import net.minecraft.world.InteractionHand;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.level.ClipContext;import net.minecraft.world.phys.*;
/** Fixed VISITOR native boundary. Public lookups begin only under a live task; callbacks never issue gameplay. */
public final class VisitorFlightController implements VisitorTask.Port {
 public interface Travel{boolean send(AutomationSession.Handle handle,PestPlot plot,long now);}
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final GardenFlightCore flight;private final Travel travel;
 private HypixelMarketClient prices;private HypixelItemCatalogClient catalog;private CompletableFuture<?> priceRequest,catalogRequest;private AutomationSession.Handle dataOwner;private Object dataLevel;private long requestedAt;private boolean requested;
 private final InventoryReceipts purchaseItems;private final CursorReceipts purchaseCursors;private final BazaarAcquisitionController.Opener purchaseOpener;private final java.util.function.LongSupplier purse;
 private BazaarAcquisitionController acquisition;private List<BazaarAcquisitionPlan.Need> acquisitionNeeds=List.of();private VisitorPolicy acquisitionPolicy;private SpendingBudget acquisitionBudget;private AutomationSession.Handle acquisitionOwner;private boolean acquisitionComplete;
 public VisitorFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel){this(coordinator,session,observe,travel,null,null,null,null);}
 public VisitorFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel,InventoryReceipts items,CursorReceipts cursors,BazaarAcquisitionController.Opener opener,java.util.function.LongSupplier purse){this.coordinator=coordinator;this.session=session;this.observe=observe;this.travel=travel;purchaseItems=items;purchaseCursors=cursors;purchaseOpener=opener;this.purse=purse;flight=new GardenFlightCore(coordinator,session,observe,AutomationFeature.VISITOR);}
 private Minecraft mc(){return Minecraft.getInstance();}private static PestTargets.Point point(Vec3 p){return new PestTargets.Point(p.x,p.y,p.z);}
 public AutomationContext context(){return observe.get();}public boolean manualInput(){return flight.manualInput();}public PestTargets.Point position(){return mc().player==null?null:point(mc().player.position());}
 public VisitorTask.Frame frame(long now){
  var mc=mc();var context=context();var p=mc.player;var zero=new PestTargets.Point(0,0,0);var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
  if(p==null||mc.level==null||!mc.isSameThread())return new VisitorTask.Frame(context.worldId(),now,new FlightAnchor(zero,0,0,0,empty,false),zero,false,true,false,false);
  var pose=new FlightAnchor(point(p.position()),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),AutomationGameView.item(p.getMainHandItem(),true),p.getAbilities().flying);
  return new VisitorTask.Frame(context.worldId(),now,pose,point(p.getEyePosition()),p.getAbilities().mayfly,manualInput(),p.containerMenu.getCarried().isEmpty(),!p.isSpectator()&&!p.isPassenger()&&!p.isUsingItem()&&!p.isSleeping());
 }
 public VisitorQueue queue(){return VisitorGameView.queue(mc());}public List<VisitorTargets.Target> targets(VisitorQueue queue){return VisitorGameView.targets(mc(),queue);}public Map<Integer,ItemSnapshot> inventory(){return VisitorGameView.inventory(mc());}public MenuSnapshot menu(){return AutomationGameView.menu(mc(),context().worldId());}public long wallMillis(){return System.currentTimeMillis();}
 public void requestData(AutomationSession.Handle h){
  long now=System.nanoTime();if(!mc().isSameThread()||h==null||h.lease().owner()!=AutomationFeature.VISITOR||!session.isCurrent(h)||!context().canIssueCommand()||!context().garden()||h.lease().worldId()!=context().worldId()||dataOwner!=null||requested&&(now-requestedAt<0||now-requestedAt<20_000_000_000L))throw new IllegalStateException("Visitor data request not authorized");
  dataOwner=h;dataLevel=mc().level;requested=true;requestedAt=now;
  if(catalog==null)catalog=new HypixelItemCatalogClient();if(prices==null)prices=new HypixelMarketClient();
  catalogRequest=catalog.refresh();priceRequest=prices.refresh();
 }
 public Optional<VisitorTask.Data> data(){
  if(!mc().isSameThread()||dataOwner==null||!session.isCurrent(dataOwner)||dataLevel!=mc().level||dataOwner.lease().worldId()!=context().worldId()||catalog==null||prices==null)return Optional.empty();
  return catalog.latest().flatMap(items->prices.latest().map(market->new VisitorTask.Data(items.names(),market)));
 }
 public boolean prepare(AutomationSession.Handle h,FlightAnchor original,FlightAnchor region,long now){return flight.prepare(h,original.slot(),original.item(),region,64,now);}
 public FlightMotion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long now){return flight.move(h,goal,tolerance,now);}public FlightMotion restore(AutomationSession.Handle h,FlightAnchor original,long now){return flight.restore(h,original,now);}public void release(AutomationSession.Handle h){flight.release(h);}
 public void afterStop(AutomationSession.Handle h){flight.afterStop(h);}
 public void dispose(AutomationSession.Handle h){
  try{flight.dispose(h);}finally{if(h!=null&&h.equals(dataOwner)){
   dataOwner=null;dataLevel=null;
   var pendingCatalog=catalogRequest!=null&&!catalogRequest.isDone()?catalog:null;var pendingPrices=priceRequest!=null&&!priceRequest.isDone()?prices:null;
   if(pendingCatalog!=null)catalog=null;if(pendingPrices!=null)prices=null;catalogRequest=null;priceRequest=null;
   try{if(pendingCatalog!=null)pendingCatalog.close();}finally{if(pendingPrices!=null)pendingPrices.close();}
  }}
 }
 public void shutdownData(){
  var oldCatalog=catalog;var oldPrices=prices;catalog=null;prices=null;catalogRequest=null;priceRequest=null;dataOwner=null;dataLevel=null;
  try{if(oldCatalog!=null)oldCatalog.close();}finally{if(oldPrices!=null)oldPrices.close();}
 }
 public boolean travel(AutomationSession.Handle h,PestPlot plot,long now){return plot!=null&&flight.allowed(h,now,AutomationCoordinator.Resource.COMMAND)&&travel.send(h,plot,now);}
 public VisitorTask.Open open(AutomationSession.Handle h,VisitorTargets.Target target,long now){
  release(h);if(target==null||!flight.allowed(h,now,AutomationCoordinator.Resource.USE,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.HOTBAR)||flight.owner()==null)return VisitorTask.Open.REJECTED;
  var mc=mc();var p=mc.player;if(mc.gameMode==null||p.isSpectator()||!p.getAbilities().flying||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty()||p.getInventory().getSelectedSlot()!=flight.slot()||!PestIdentity.sameTool(flight.tool(),AutomationGameView.item(p.getMainHandItem(),true))||!flight.clear(point(p.position()),point(p.position())))return VisitorTask.Open.REJECTED;
  var current=targets(queue());if(current.stream().filter(target::equals).count()!=1)return VisitorTask.Open.REJECTED;
  var entity=mc.level.getEntity(target.id());if(!(entity instanceof LivingEntity)||!entity.getUUID().equals(target.uuid())||entity.isRemoved()||!entity.isAlive())return VisitorTask.Open.REJECTED;
  Vec3 eye=p.getEyePosition(),aim=new Vec3(target.position().x(),target.position().y()+1,target.position().z());if(!entity.getBoundingBox().inflate(.1).contains(aim)||eye.distanceToSqr(aim)>2.75*2.75||!flight.loaded(new AABB(eye,aim).inflate(.01))||mc.level.clip(new ClipContext(eye,aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p)).getType()!=HitResult.Type.MISS)return VisitorTask.Open.REJECTED;
  double dx=aim.x-eye.x,dy=aim.y-eye.y,dz=aim.z-eye.z;float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));float nextYaw=PestSteering.rotate(p.getYRot(),yaw,12,true),nextPitch=PestSteering.rotate(p.getXRot(),pitch,12,false);p.setYRot(nextYaw);p.setXRot(nextPitch);
  if(Math.abs(PestSteering.rotate(nextYaw,yaw,30,true)-nextYaw)>3||Math.abs(nextPitch-pitch)>3)return VisitorTask.Open.AIMING;
  if(!flight.allowed(h,now,AutomationCoordinator.Resource.USE))return VisitorTask.Open.REJECTED;
  mc.gameMode.interact(p,entity,new EntityHitResult(entity,aim),InteractionHand.MAIN_HAND);return VisitorTask.Open.SENT;
 }
 public GuardedMenuAction.Result submit(AutomationSession.Handle h,MenuContract contract,MenuContract.Ticket ticket,long now){
  if(h==null||flight.owner()==null||!flight.owner().equals(h)||flight.levelIdentity()!=mc().level||!session.isCurrent(h)||h.lease().owner()!=AutomationFeature.VISITOR||ticket==null||ticket.click()!=MenuContract.Click.PICKUP||data().isEmpty())return GuardedMenuAction.Result.REJECTED;
  var offer=VisitorOffer.inspect(menu(),ticket.observed().title(),data().get().materialIds());if(offer.isEmpty()||offer.get().acceptSlot()!=ticket.slot()||!offer.get().menu().equals(ticket.observed()))return GuardedMenuAction.Result.REJECTED;
  return AutomationMenuActions.purchaseClick(coordinator,h.lease(),contract,ticket,this::context,this::menu,now);
 }
 public boolean beginAcquisition(AutomationSession.Handle h,VisitorOffer offer,VisitorPolicy policy,SpendingBudget budget,long now){
  if(acquisitionOwner!=null||purchaseItems==null||purchaseCursors==null||purchaseOpener==null||purse==null||h==null||h.lease().owner()!=AutomationFeature.VISITOR||!session.isCurrent(h)||!h.equals(dataOwner)||dataLevel!=mc().level||data().isEmpty()||offer==null||policy==null||budget==null||budget.limit()!=policy.rules().maximumBuyCoins()||menu()!=null||!frame(now).cursorEmpty())return false;
  var needs=new ArrayList<BazaarAcquisitionPlan.Need>();for(var required:offer.requirements()){if(required.quantity()>4096)return false;needs.add(new BazaarAcquisitionPlan.Need(required.itemId(),required.quantity()));}
  acquisitionOwner=h;acquisitionNeeds=List.copyOf(needs);acquisitionPolicy=policy;acquisitionBudget=budget;acquisitionComplete=false;
  if(!nextAcquisition(now)){cancelAcquisition();return false;}return true;
 }
 private boolean nextAcquisition(long now){
  if(!session.isCurrent(acquisitionOwner)||dataLevel!=mc().level||catalog==null||prices==null||catalog.latest().isEmpty()||prices.latest().isEmpty())return false;
  var inventory=inventory();if(inventory.size()!=43)return false;
  for(var need:acquisitionNeeds){
   int owned=0;for(int i=9;i<36;i++){var item=inventory.get(i);if(item==null)return false;if(item.identified()&&!item.protectedItem()&&item.uuid().isEmpty()&&item.skyblockId().equals(need.productId()))owned+=item.count();}
   if(owned>=need.targetCount())continue;
   var plan=BazaarAcquisitionPlan.plan(List.of(need),inventory,acquisitionPolicy.buyingRules(),prices.latest().get(),catalog.latest().get(),acquisitionBudget,wallMillis()).orElse(null);
   if(plan==null||plan.requests().size()!=1)return false;
   acquisition=new BazaarAcquisitionController(coordinator,session,observe,purchaseItems,purchaseCursors,acquisitionOwner,plan,acquisitionBudget,purchaseOpener,purse);return acquisition.begin(now);
  }
  acquisition=null;acquisitionComplete=true;return true;
 }
 public VisitorTask.Acquisition pollAcquisition(AutomationSession.Handle h,long now){
  if(h==null||!h.equals(acquisitionOwner)||!session.isCurrent(h)||dataLevel!=mc().level)return VisitorTask.Acquisition.FAILED;
  if(acquisitionComplete){cancelAcquisition();return VisitorTask.Acquisition.COMPLETE;}if(acquisition==null)return VisitorTask.Acquisition.FAILED;
  acquisition.tick(now);
  var result=switch(acquisition.state()){
   case COMPLETE->{if(!nextAcquisition(now))yield VisitorTask.Acquisition.FAILED;yield acquisitionComplete?VisitorTask.Acquisition.COMPLETE:VisitorTask.Acquisition.RUNNING;}
   case FAILED->VisitorTask.Acquisition.FAILED;case UNCERTAIN->VisitorTask.Acquisition.UNCERTAIN;default->VisitorTask.Acquisition.RUNNING;
  };
  if(result==VisitorTask.Acquisition.COMPLETE)cancelAcquisition();return result;
 }
 public boolean ownsAcquisitionScreen(){return acquisition!=null&&acquisition.ownsScreen();}
 public void acquisitionMessage(long world,String text){if(acquisition!=null)acquisition.onSystemMessage(world,text);}
 public void cancelAcquisition(){if(acquisition!=null)acquisition.cancel();acquisition=null;acquisitionOwner=null;acquisitionPolicy=null;acquisitionBudget=null;acquisitionNeeds=List.of();acquisitionComplete=false;}
 public void closeOwnedMenu(int id){var mc=mc();var menu=menu();if(mc.isSameThread()&&flight.owner()!=null&&session.isCurrent(flight.owner())&&flight.levelIdentity()==mc.level&&menu!=null&&menu.worldId()==flight.owner().lease().worldId()&&menu.containerId()==id&&data().flatMap(d->VisitorOffer.inspect(menu,menu.title(),d.materialIds())).isPresent()&&mc.player!=null&&mc.player.containerMenu.getCarried().isEmpty())mc.player.closeContainer();}
}
