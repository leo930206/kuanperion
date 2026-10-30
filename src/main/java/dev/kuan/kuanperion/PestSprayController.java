// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.pest.*;
import java.util.*;import java.util.concurrent.CompletableFuture;import java.util.function.Supplier;
import com.mojang.blaze3d.platform.InputConstants;import net.minecraft.client.KeyMapping;import net.minecraft.client.Minecraft;import net.minecraft.world.InteractionHand;import org.lwjgl.glfw.GLFW;
/** Fixed in-place item-use adapter. No flight core, world-input exemption, physical key writes or async gameplay. */
public final class PestSprayController implements PestSprayTask.Port,PestSpraySelectionTask.Port {
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final Supplier<PestSprayPolicy> policy;private final PlayerPositionReceipts positions;private final PestSprayTabReceipts tabs;
 private InventoryReceipts buyItems;private CursorReceipts buyCursors;private BazaarAcquisitionController.Opener buyOpener;private java.util.function.LongSupplier buyPurse;
 private HypixelMarketClient buyPrices;private BazaarAcquisitionController acquisition;private AutomationSession.Handle buyOwner;private PestSprayPolicy buyPolicy;private PestSprayBuying buying;private SpendingBudget buyBudget;private long buyStarted;private PestSprayTask.Frame buyAnchor;
 private HypixelItemCatalogClient catalog;private CompletableFuture<?> request;private AutomationSession.Handle dataOwner;private Object dataLevel,dataPlayer;private String materialName="";private boolean effectInProgress;
 public PestSprayController(AutomationCoordinator c,AutomationSession s,PlayerPositionReceipts positions,PestSprayTabReceipts tabs,Supplier<AutomationContext> observe,Supplier<PestSprayPolicy> policy){coordinator=c;session=s;this.positions=positions;this.tabs=tabs;this.observe=observe;this.policy=policy;}
 public PestSprayController(AutomationCoordinator c,AutomationSession s,PlayerPositionReceipts positions,PestSprayTabReceipts tabs,Supplier<AutomationContext> observe,Supplier<PestSprayPolicy> policy,InventoryReceipts items,CursorReceipts cursors,BazaarAcquisitionController.Opener opener,java.util.function.LongSupplier purse){this(c,s,positions,tabs,observe,policy);buyItems=Objects.requireNonNull(items);buyCursors=Objects.requireNonNull(cursors);buyOpener=Objects.requireNonNull(opener);buyPurse=Objects.requireNonNull(purse);}
 /** Demand-only optional purchase; no new root, price request, menu or command without explicit Coins consent. */
 public boolean beginSprayAcquisition(AutomationSession.Handle h,PestSprayPolicy p,PestSprayBuying purchase,SpendingBudget budget,long now){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||buyOwner!=null||acquisition!=null||buyItems==null||buyCursors==null||buyOpener==null||buyPurse==null||h==null||!h.equals(dataOwner)||h.hold()==null||!session.holdsFarming()||h.lease().owner()!=AutomationFeature.PEST_SPRAY||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||mc.level!=dataLevel||mc.player!=dataPlayer||p==null||!p.configured()||!p.equals(policy.get())||purchase==null||!purchase.configured()||budget==null||budget.limit()!=purchase.maximumCoins()||budget.available()<=0||manualInput())return false;
  var c=context();var f=sprayFrame(now);if(c==null||c.worldId()!=h.lease().worldId()||!c.garden()||!c.farmingActive()||!c.canIssueCommand()||!coordinator.permits(h.lease(),AutomationCoordinator.Resource.COMMAND,c,now)||f==null||!f.idleInventory()||f.usingItem()||!PestSprayPlan.air(f.cursor())||PestSprayPlan.prepare(p,f.inventory()).isPresent()||PestSprayPlan.prepareForBuying(p,f.inventory()).isEmpty()||!session.isCurrent(h)||!h.equals(dataOwner)||mc.level!=dataLevel||mc.player!=dataPlayer||manualInput())return false;
  buyOwner=h;buyPolicy=p;buying=purchase;buyBudget=budget;buyAnchor=f;buyStarted=now;
  if(buyPrices==null)buyPrices=new HypixelMarketClient();buyPrices.refresh();return true;
 }
 private boolean sprayAcquisitionCurrent(AutomationSession.Handle h,long now){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||h==null||!h.equals(buyOwner)||!h.equals(dataOwner)||!session.isCurrent(h)||!session.holdsFarming()||!coordinator.holdsIdentity(h.lease())||mc.level!=dataLevel||mc.player!=dataPlayer||buyAnchor==null||manualInput()||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime())return false;
  var c=context();var f=sprayFrame(now);return h.equals(buyOwner)&&session.isCurrent(h)&&mc.level==dataLevel&&mc.player==dataPlayer&&c!=null&&c.worldId()==h.lease().worldId()&&c.garden()&&c.farmingActive()&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU)&&f!=null&&f.level()==buyAnchor.level()&&f.player()==buyAnchor.player()&&f.selected()==buyAnchor.selected()&&!f.usingItem()&&PestSprayPlan.air(f.cursor())&&f.x()==buyAnchor.x()&&f.y()==buyAnchor.y()&&f.z()==buyAnchor.z()&&f.yaw()==buyAnchor.yaw()&&f.pitch()==buyAnchor.pitch()&&f.positionEpoch()==buyAnchor.positionEpoch()&&f.positionSequence()==buyAnchor.positionSequence()&&f.inventory().get(buyAnchor.selected()).equals(buyAnchor.inventory().get(buyAnchor.selected()));
 }
 public PestSprayTask.Acquisition pollSprayAcquisition(AutomationSession.Handle h,long now){
  if(!sprayAcquisitionCurrent(h,now))return PestSprayTask.Acquisition.FAILED;
  if(acquisition==null){
   var names=catalog==null?Optional.<SkyblockItemCatalog>empty():catalog.latest();var prices=buyPrices.latest();
   if(names.isEmpty()||prices.isEmpty())return now-buyStarted>=0&&now-buyStarted<12_000_000_000L?PestSprayTask.Acquisition.RUNNING:PestSprayTask.Acquisition.FAILED;
   if(!buyPolicy.materialId().equals(names.get().names().get(buyPolicy.materialName())))return PestSprayTask.Acquisition.FAILED;
   var inventory=VisitorGameView.inventory(Minecraft.getInstance());var plan=BazaarAcquisitionPlan.plan(List.of(new BazaarAcquisitionPlan.Need(buyPolicy.materialId(),buying.targetCount())),inventory,buying.rules(buyPolicy),prices.get(),names.get(),buyBudget,System.currentTimeMillis()).orElse(null);
   if(plan==null||plan.requests().size()!=1||!sprayAcquisitionCurrent(h,now))return PestSprayTask.Acquisition.FAILED;
   var child=new BazaarAcquisitionController(coordinator,session,observe,buyItems,buyCursors,h,plan,buyBudget,buyOpener,buyPurse);acquisition=child;
   boolean begun=child.begin(now);if(acquisition!=child||!sprayAcquisitionCurrent(h,now))return PestSprayTask.Acquisition.FAILED;if(!begun)return PestSprayTask.Acquisition.FAILED;
  }
  var child=acquisition;child.tick(now);if(acquisition!=child||!sprayAcquisitionCurrent(h,now))return PestSprayTask.Acquisition.FAILED;
  return switch(child.state()){case COMPLETE->PestSprayTask.Acquisition.COMPLETE;case FAILED->PestSprayTask.Acquisition.FAILED;case UNCERTAIN->PestSprayTask.Acquisition.UNCERTAIN;default->PestSprayTask.Acquisition.RUNNING;};
 }
 public boolean ownsSprayAcquisitionScreen(){return acquisition!=null&&acquisition.ownsScreen();}
 public void sprayAcquisitionMessage(long world,String text){if(acquisition!=null)acquisition.onSystemMessage(world,text);}
 public void cancelSprayAcquisition(){var old=acquisition;var h=buyOwner;acquisition=null;buyOwner=null;buyPolicy=null;buying=null;buyBudget=null;buyAnchor=null;AutomationStops.run(()->{if(old!=null)old.cancel();},()->{if(h!=null)session.clearExpectedMenu(h);});}
 private void shutdownSprayBuying(){var prices=buyPrices;buyPrices=null;AutomationStops.run(this::cancelSprayAcquisition,()->{if(prices!=null)prices.close();});}
 public AutomationContext context(){return observe.get();}
 public PestSprayTask.Frame sprayFrame(long now){if(!Minecraft.getInstance().isSameThread())return null;var c=context();return nativeFrame(c.worldId(),now,dataOwner==null?policy.get().materialName():materialName);}
 private PestSprayTask.Frame nativeFrame(long world,long now,String material){
  var mc=Minecraft.getInstance();var p=mc.player;if(!mc.isSameThread()||p==null||mc.level==null||mc.getWindow()==null)return null;
  var inventory=new HashMap<Integer,ItemSnapshot>();for(var slot:AutomationGameView.inventory(mc))if(inventory.put(slot.index(),slot.item())!=null)return null;
  var velocity=p.getDeltaMovement();var positionStamp=positions.stamp(world,now);
  var tab=PestSprayTabView.current(mc,world,positionStamp,tabs,now).orElse(null);
  boolean idle=mc.gui.screen()==null&&mc.gui.overlay()==null&&p.containerMenu==p.inventoryMenu&&p.containerMenu.containerId==0&&p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&!p.isSleeping()&&GLFW.glfwGetWindowAttrib(mc.getWindow().handle(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE;
  return new PestSprayTask.Frame(world,now,mc.level,p,inventory,AutomationGameView.item(p.containerMenu.getCarried(),true),p.getInventory().getSelectedSlot(),idle,p.isUsingItem(),p.getX(),p.getY(),p.getZ(),velocity.x,velocity.y,velocity.z,p.getYRot(),p.getXRot(),tab==null?PestSprayObservation.unknown():PestSprayObservation.parse(tab.entries(),material),positionStamp.epoch(),positionStamp.sequence(),tab,PestSprayGroundView.residual(p,velocity.y));
 }
 public PestSprayTask.Effect sprayEffect(AutomationSession.Handle h,PestSprayTask.Action action,long now){
  var mc=Minecraft.getInstance();var level=mc.level;var player=mc.player;var gameMode=mc.gameMode;
  if(!mc.isSameThread()||player==null||level==null||mc.gameMode==null||effectInProgress||h==null||action==null||!h.equals(action.owner())||h.lease().owner()!=AutomationFeature.PEST_SPRAY||!h.equals(dataOwner)||level!=dataLevel||player!=dataPlayer)return PestSprayTask.Effect.REJECTED;
  boolean effectStarted=false;effectInProgress=true;
  try{
   var c=context();var resource=action.kind()==PestSprayTask.Kind.USE?AutomationCoordinator.Resource.USE:AutomationCoordinator.Resource.HOTBAR;
   if(!session.isCurrent(h)||!c.canIssueCommand()||!c.garden()||!c.farmingActive()||manualInput()||!coordinator.permits(h.lease(),resource,c,now))return PestSprayTask.Effect.REJECTED;
   var before=nativeFrame(h.lease().worldId(),now,materialName);if(!action.claim(h,before,now))return PestSprayTask.Effect.REJECTED;
   // claim may read a fallible context. Re-sample physical identities/pose/all43/cursor/Tab AFTER it.
   boolean manual=manualInput();var physical=nativeFrame(h.lease().worldId(),now,materialName);
   if(!mc.isSameThread()||mc.level!=level||mc.player!=player||mc.gameMode!=gameMode||!session.isCurrent(h)||!h.equals(dataOwner)||!coordinator.permits(h.lease(),resource,c,now)||manual||!action.matchesSnapshot(physical))return PestSprayTask.Effect.REJECTED;
   effectStarted=true;
   if(action.kind()==PestSprayTask.Kind.USE)gameMode.useItem(player,InteractionHand.MAIN_HAND);
   else player.getInventory().setSelectedSlot(action.targetSlot());
   return PestSprayTask.Effect.SENT;
  }catch(RuntimeException unavailable){return effectStarted?PestSprayTask.Effect.UNCERTAIN:PestSprayTask.Effect.REJECTED;}
  finally{effectInProgress=false;}
 }
 /** Same original root, source-style GUI selection; no generic inventory/world-input exemption. */
 public PestSpraySelectionTask.Frame selectionFrame(long now){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||dataOwner==null)return null;return nativeSelectionFrame(dataOwner.lease().worldId(),now);
 }
 private PestSpraySelectionTask.Frame nativeSelectionFrame(long world,long now){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.player==null||mc.level==null||mc.getWindow()==null)return null;var p=mc.player;var motion=nativeFrame(world,now,materialName);if(motion==null)return null;
  var menu=AutomationGameView.menu(mc,world);boolean usable=mc.gui.overlay()==null&&p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&!p.isSleeping()&&GLFW.glfwGetWindowAttrib(mc.getWindow().handle(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE;
  return new PestSpraySelectionTask.Frame(motion,menu==null?null:p.containerMenu,menu,PestSpraySelectionGameView.invariant(p.getInventory().getItem(p.getInventory().getSelectedSlot())),usable,menu==null&&airSwingReady());
 }
 private boolean selectionAllowed(AutomationSession.Handle h,PestSpraySelectionTask.Kind kind,long now){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||h==null||h.hold()==null||h.lease().owner()!=AutomationFeature.PEST_SPRAY||!session.isCurrent(h)||!session.holdsFarming()||!h.equals(dataOwner)||mc.level!=dataLevel||mc.player!=dataPlayer||manualInput())return false;
  var c=context();if(c==null||c.worldId()!=h.lease().worldId()||!c.garden()||!c.farmingActive()||c.blockers().stream().anyMatch(x->x!=AutomationContext.Blocker.MENU))return false;
  if(kind==PestSpraySelectionTask.Kind.OPEN)return c.canIssueCommand()&&coordinator.permits(h.lease(),AutomationCoordinator.Resource.ATTACK,c,now)&&coordinator.permits(h.lease(),AutomationCoordinator.Resource.HOTBAR,c,now)&&!manualInput();
  var owned=new AutomationContext(c.worldId(),c.farmingActive(),c.garden(),Set.of());return coordinator.permits(h.lease(),AutomationCoordinator.Resource.SCREEN,owned,now)&&!manualInput();
 }
 private boolean airSwingReady(){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.player==null||mc.level==null||mc.gameMode==null||mc.hitResult==null||mc.hitResult.getType()!=net.minecraft.world.phys.HitResult.Type.MISS||!((Object)mc instanceof dev.kuan.kuanperion.mixin.MinecraftAttackAccess access)||access.kuanperion$getMissTime()>0)return false;
  var p=mc.player;var hand=p.getItemInHand(InteractionHand.MAIN_HAND);return !p.isHandsBusy()&&!p.isUsingItem()&&!mc.gameMode.isSpectator()&&!mc.gameMode.isDestroying()&&hand.isItemEnabled(mc.level.enabledFeatures())&&!p.cannotAttackWithItem(hand,0)&&!hand.has(net.minecraft.core.component.DataComponents.PIERCING_WEAPON);
 }
 public PestSprayTask.Effect selectionEffect(AutomationSession.Handle h,PestSpraySelectionTask.Action action,long now){
  var mc=Minecraft.getInstance();var level=mc.level;var player=mc.player;var mode=mc.gameMode;
  if(!mc.isSameThread()||player==null||level==null||mode==null||effectInProgress||AutomationMenuActions.ownsInventoryInput()||action==null||!Objects.equals(h,action.owner()))return PestSprayTask.Effect.REJECTED;
  effectInProgress=true;boolean started=false;
  try{
   if(action.kind()!=PestSpraySelectionTask.Kind.OPEN)return AutomationMenuActions.sendPestSpraySelectionNative(h,action,now,()->nativeSelectionFrame(h.lease().worldId(),now),()->selectionAllowed(h,action.kind(),now));
   if(!selectionAllowed(h,action.kind(),now))return PestSprayTask.Effect.REJECTED;var before=nativeSelectionFrame(h.lease().worldId(),now);if(!action.claim(h,before,now)||!selectionAllowed(h,action.kind(),now))return PestSprayTask.Effect.REJECTED;
   var physical=nativeSelectionFrame(h.lease().worldId(),now);if(mc.level!=level||mc.player!=player||mc.gameMode!=mode||!action.matchesSnapshot(physical)||!airSwingReady())return PestSprayTask.Effect.REJECTED;
   // Ordinary selected-slot sync, then only vanilla's safe MISS swing subset. NEVER startAttack, entity attack or block destroy.
   if(!((Object)mode instanceof dev.kuan.kuanperion.mixin.SprayCarriedItemAccess sync))return PestSprayTask.Effect.REJECTED;
   started=true;sync.kuanperion$syncSprayCarriedItem();
   if(!selectionAllowed(h,action.kind(),now))return PestSprayTask.Effect.UNCERTAIN;physical=nativeSelectionFrame(h.lease().worldId(),now);
   if(mc.level!=level||mc.player!=player||mc.gameMode!=mode||!action.matchesSnapshot(physical)||!airSwingReady())return PestSprayTask.Effect.UNCERTAIN;
   player.resetAttackStrengthTicker();player.swing(InteractionHand.MAIN_HAND);return PestSprayTask.Effect.SENT;
  }catch(RuntimeException ambiguous){return started?PestSprayTask.Effect.UNCERTAIN:PestSprayTask.Effect.REJECTED;}finally{effectInProgress=false;}
 }
 public boolean manualInput(){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.options==null||mc.getWindow()==null)return true;
  for(var key:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint,mc.options.keyAttack,mc.options.keyUse))if(physical(key))return true;
  for(var key:mc.options.keyHotbarSlots)if(physical(key))return true;return false;
 }
 private boolean physical(KeyMapping mapping){
  if(mapping==null)return true;if(mapping.isUnbound())return false;var key=InputConstants.getKey(mapping.saveString());long window=Minecraft.getInstance().getWindow().handle();
  if(key.getType()==InputConstants.Type.KEYSYM)return key.getValue()<0||GLFW.glfwGetKey(window,key.getValue())==GLFW.GLFW_PRESS;
  if(key.getType()==InputConstants.Type.MOUSE)return GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS;return true;
 }
 public void requestSprayData(AutomationSession.Handle h){
  var mc=Minecraft.getInstance();var level=mc.level;var player=mc.player;if(!mc.isSameThread())throw new IllegalStateException("Client thread required");var c=context();var p=policy.get();
  if(!mc.isSameThread()||h==null||h.lease().owner()!=AutomationFeature.PEST_SPRAY||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||!c.canIssueCommand()||!c.garden()||!c.farmingActive()||h.lease().worldId()!=c.worldId()||level==null||player==null||mc.level!=level||mc.player!=player||manualInput()||dataOwner!=null||!p.configured())throw new IllegalStateException("Spray catalog demand not authorized");
  dataOwner=h;dataLevel=level;dataPlayer=player;materialName=p.materialName();if(catalog==null)catalog=new HypixelItemCatalogClient();request=catalog.refresh();
 }
 public Optional<Map<String,String>> sprayCatalog(AutomationSession.Handle h){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||h==null||!h.equals(dataOwner)||!session.isCurrent(h)||mc.level!=dataLevel||mc.player!=dataPlayer||catalog==null)return Optional.empty();return catalog.latest().map(SkyblockItemCatalog::names);
 }
 public void disposeSprayData(AutomationSession.Handle h){
  if(h==null||!h.equals(dataOwner))return;dataOwner=null;dataLevel=dataPlayer=null;materialName="";
  var pending=request!=null&&!request.isDone()?catalog:null;request=null;if(pending!=null)catalog=null;AutomationStops.run(this::shutdownSprayBuying,()->{if(pending!=null)pending.close();});
 }
 public void shutdownData(){var old=catalog;catalog=null;request=null;dataOwner=null;dataLevel=dataPlayer=null;materialName="";AutomationStops.run(this::shutdownSprayBuying,()->{if(old!=null)old.close();});}
}
