// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.composter.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;import java.util.function.Supplier;import net.minecraft.client.Minecraft;import net.minecraft.world.InteractionHand;import net.minecraft.core.BlockPos;import net.minecraft.world.level.ClipContext;import net.minecraft.world.phys.*;
/** Fixed COMPOSTER native boundary; no purchase, sack slot assumption or cursor repair. */
public final class ComposterFlightController implements ComposterTask.Port {
 public interface Travel{boolean send(AutomationSession.Handle handle,PestPlot plot,long now);}
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final GardenFlightCore flight;private final Travel travel;
 private final InventoryReceipts purchaseItems;private final CursorReceipts purchaseCursors;private final BazaarAcquisitionController.Opener purchaseOpener;private final java.util.function.LongSupplier purse;
 private HypixelItemCatalogClient catalog;private HypixelMarketClient prices;private AutomationSession.Handle dataOwner,acquisitionOwner;private Object dataLevel;private long dataRequestedAt;private List<BazaarAcquisitionPlan.Need> acquisitionNeeds=List.of();private ComposterPolicy acquisitionPolicy;private SpendingBudget acquisitionBudget;private BazaarAcquisitionController acquisition;private boolean acquisitionComplete;
 public ComposterFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel){this(coordinator,session,observe,travel,null,null,null,null);}
 public ComposterFlightController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,Travel travel,InventoryReceipts items,CursorReceipts cursors,BazaarAcquisitionController.Opener opener,java.util.function.LongSupplier purse){this.coordinator=coordinator;this.session=session;this.observe=observe;this.travel=travel;purchaseItems=items;purchaseCursors=cursors;purchaseOpener=opener;this.purse=purse;flight=new GardenFlightCore(coordinator,session,observe,AutomationFeature.COMPOSTER);}
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
 public void dispose(AutomationSession.Handle h){try{flight.dispose(h);}finally{if(h!=null&&h.equals(dataOwner))AutomationStops.run(this::cancelAcquisition,this::shutdownData);}}
 private void shutdownData(){var c=catalog;var p=prices;catalog=null;prices=null;dataOwner=null;dataLevel=null;AutomationStops.run(()->{if(c!=null)c.close();},()->{if(p!=null)p.close();});}
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
 /** No startup clients: official anonymous data is requested only after an explicit original-parent shortage. */
 public boolean beginAcquisition(AutomationSession.Handle h,ComposterPolicy policy,Set<ComposterObservation.Kind> kinds,SpendingBudget budget,long now){
  if(acquisitionOwner!=null||purchaseItems==null||purchaseCursors==null||purchaseOpener==null||purse==null||h==null||h.lease().owner()!=AutomationFeature.COMPOSTER||!session.isCurrent(h)||!h.equals(flight.owner())||flight.levelIdentity()!=mc().level||!flight.allowed(h,now,AutomationCoordinator.Resource.COMMAND)||policy==null||kinds==null||kinds.isEmpty()||budget==null||budget.limit()!=policy.buying().maximumCoins()||budget.available()<=0||menu()!=null||!frame(now).cursorEmpty()||dataOwner!=null&&!dataOwner.equals(h))return false;
  var needs=new ArrayList<BazaarAcquisitionPlan.Need>();
  for(var kind:ComposterObservation.Kind.values())if(kinds.contains(kind))for(var e:policy.buying().stock().entrySet())if(policy.materials().get(e.getKey()).kind()==kind)needs.add(new BazaarAcquisitionPlan.Need(e.getKey(),e.getValue().targetCount()));
  if(needs.isEmpty())return false;
  dataOwner=h;dataLevel=mc().level;dataRequestedAt=now;acquisitionOwner=h;acquisitionNeeds=List.copyOf(needs);acquisitionPolicy=policy;acquisitionBudget=budget;acquisitionComplete=false;
  if(catalog==null)catalog=new HypixelItemCatalogClient();if(prices==null)prices=new HypixelMarketClient();catalog.refresh();prices.refresh();return true;
 }
 private boolean acquisitionCurrent(AutomationSession.Handle h){return mc().isSameThread()&&h!=null&&h.equals(acquisitionOwner)&&h.equals(dataOwner)&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&mc().level==dataLevel&&flight.levelIdentity()==dataLevel&&context().worldId()==h.lease().worldId();}
 private boolean nextAcquisition(long now){
  if(!acquisitionCurrent(acquisitionOwner)||catalog.latest().isEmpty()||prices.latest().isEmpty())return false;
  var inventory=VisitorGameView.inventory(mc());if(inventory.size()!=43)return false;
  for(var need:acquisitionNeeds){
   int owned=0;var material=acquisitionPolicy.buying().stock().get(need.productId()).material();
   for(int i=0;i<43;i++){var item=inventory.get(i);if(item==null)return false;if(item.count()==0||!item.skyblockId().equals(need.productId()))continue;
    if(i<9||i>=36||!item.identified()||item.protectedItem()||!item.uuid().isEmpty()||!item.minecraftId().equals(material.minecraftId())||item.count()>material.stackLimit())return false;owned+=item.count();
   }
   if(owned>=need.targetCount())continue;
   var plan=BazaarAcquisitionPlan.plan(List.of(need),inventory,acquisitionPolicy.buying().rules(acquisitionPolicy.maxItems()),prices.latest().get(),catalog.latest().get(),acquisitionBudget,System.currentTimeMillis()).orElse(null);
   if(plan==null||plan.requests().size()!=1)return false;
   acquisition=new BazaarAcquisitionController(coordinator,session,observe,purchaseItems,purchaseCursors,acquisitionOwner,plan,acquisitionBudget,purchaseOpener,purse);return acquisition.begin(now);
  }
  acquisition=null;acquisitionComplete=true;return true;
 }
 public ComposterTask.Acquisition pollAcquisition(AutomationSession.Handle h,long now){
  if(!acquisitionCurrent(h))return ComposterTask.Acquisition.FAILED;
  if(acquisition==null&&!acquisitionComplete){
   if(catalog.latest().isEmpty()||prices.latest().isEmpty())return now-dataRequestedAt>=0&&now-dataRequestedAt<12_000_000_000L?ComposterTask.Acquisition.RUNNING:ComposterTask.Acquisition.FAILED;
   if(!nextAcquisition(now))return ComposterTask.Acquisition.FAILED;
  }
  if(acquisitionComplete){cancelAcquisition();return ComposterTask.Acquisition.COMPLETE;}
  acquisition.tick(now);
  var result=switch(acquisition.state()){
   case COMPLETE->{if(!nextAcquisition(now))yield ComposterTask.Acquisition.FAILED;yield acquisitionComplete?ComposterTask.Acquisition.COMPLETE:ComposterTask.Acquisition.RUNNING;}
   case FAILED->ComposterTask.Acquisition.FAILED;case UNCERTAIN->ComposterTask.Acquisition.UNCERTAIN;default->ComposterTask.Acquisition.RUNNING;
  };
  if(result==ComposterTask.Acquisition.COMPLETE)cancelAcquisition();return result;
 }
 public boolean ownsAcquisitionScreen(){return acquisition!=null&&acquisition.ownsScreen();}
 public void acquisitionMessage(long world,String text){if(acquisition!=null)acquisition.onSystemMessage(world,text);}
 public void cancelAcquisition(){var previous=acquisition;acquisition=null;acquisitionOwner=null;acquisitionNeeds=List.of();acquisitionPolicy=null;acquisitionBudget=null;acquisitionComplete=false;if(previous!=null)previous.cancel();}
 public void closeOwnedMenu(int id){var mc=mc();var menu=menu();if(flight.owner()!=null&&session.isCurrent(flight.owner())&&flight.levelIdentity()==mc.level&&menu!=null&&menu.worldId()==flight.owner().lease().worldId()&&menu.containerId()==id&&ComposterObservation.inspect(menu).isPresent()&&mc.player!=null&&mc.player.containerMenu.getCarried().isEmpty())mc.player.closeContainer();}
}
