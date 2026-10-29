// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;
import java.util.*;import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
/** Original-parent Bazaar acquisition: source-marked U-BZ-BUY custom-amount route.
 * No root acquisition, Farm restart, cursor repair or settings writes. One plan/request per instance.
 * An additional post-purchase warning/confirmation is not blindly clicked or retried. */
public final class BazaarAcquisitionController {
 public enum State{IDLE,OPENING,WAIT_PRODUCT,WAIT_QUANTITY,WAIT_SIGN,WAIT_FINAL,PURCHASING,COMPLETE,FAILED,UNCERTAIN}
 @FunctionalInterface public interface Opener{boolean open(AutomationSession.Handle parent,BazaarAcquisitionPlan.Request request,long now);}
 interface Game extends BazaarPurchaseController.Game {
  Object screenIdentity();boolean sign();long purse();
  boolean open(AutomationSession.Handle h,BazaarAcquisitionPlan.Request request,long now);
  boolean amount(Object screen,int quantity);boolean close(Object menu,int container);
  GuardedMenuAction.Result navigate(AutomationCoordinator c,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,Supplier<AutomationContext> context,Supplier<MenuSnapshot> menus,long now);
 }
 private static final Set<AutomationFeature> BUYERS=Set.of(AutomationFeature.PEST,AutomationFeature.VISITOR,AutomationFeature.COMPOSTER,AutomationFeature.SUPERCRAFT);
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final InventoryReceipts items;private final CursorReceipts cursors;
 private final AutomationSession.Handle parent;private final BazaarAcquisitionPlan.Plan plan;private final SpendingBudget budget;private final Game game;private final LongSupplier clock;private final Object level;
 private State state=State.IDLE;private long phaseStart;private InventoryReceipts.Stamp opening;private CursorReceipts.OpeningStamp cursorOpening;private MenuSnapshot origin;private Object originIdentity,signIdentity;private BazaarPurchaseController terminal;private BazaarAcquisitionPlan.Request request;
 public BazaarAcquisitionController(AutomationCoordinator c,AutomationSession s,Supplier<AutomationContext> observe,InventoryReceipts items,CursorReceipts cursors,AutomationSession.Handle parent,BazaarAcquisitionPlan.Plan plan,SpendingBudget budget,Opener opener,LongSupplier purse){this(c,s,observe,items,cursors,parent,plan,budget,new NativeGame(opener,purse),System::nanoTime);}
 BazaarAcquisitionController(AutomationCoordinator c,AutomationSession s,Supplier<AutomationContext> observe,InventoryReceipts items,CursorReceipts cursors,AutomationSession.Handle parent,BazaarAcquisitionPlan.Plan plan,SpendingBudget budget,Game game,LongSupplier clock){
  coordinator=Objects.requireNonNull(c);session=Objects.requireNonNull(s);this.observe=Objects.requireNonNull(observe);this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.parent=parent;this.plan=plan;this.budget=budget;this.game=Objects.requireNonNull(game);this.clock=Objects.requireNonNull(clock);level=game.level();
 }
 public State state(){return state;}public boolean active(){return switch(state){case OPENING,WAIT_PRODUCT,WAIT_QUANTITY,WAIT_SIGN,WAIT_FINAL,PURCHASING->true;default->false;};}
 public boolean begin(long now){
  if(state!=State.IDLE)return false;
  try{
   if(plan==null||budget==null||plan.requests().size()!=1||!root(now)||game.screenIdentity()!=null||!game.cursorEmpty()||!plan.stillMatches(game.inventory())){state=State.FAILED;return false;}
   request=plan.requests().getFirst();if(!request.usesBudget(budget)){state=State.FAILED;return false;}
   opening=items.stamp(parent.lease().worldId());cursorOpening=cursors.openingStamp(parent.lease().worldId(),now);
   if(!advance(State.OPENING,now)||!game.open(parent,request,now)||!root(now)||state!=State.OPENING){cancel();return false;}return true;
  }catch(RuntimeException unavailable){cancel();return false;}
 }
 public void tick(long now){if(!active())return;try{run(now);}catch(RuntimeException unavailable){cancel();}}
 private void run(long now){
  if(!root(now)||!game.cursorEmpty()||now-phaseStart<0||now-phaseStart>=10_000_000_000L){cancel();return;}
  if(state==State.PURCHASING){
   terminal.poll(now);if(state!=State.PURCHASING)return;
   switch(terminal.state()){
    case CONFIRMED->{if(!ownsScreen()||!game.close(originIdentity,origin.containerId())||!root(now)||game.screenIdentity()!=null){state=State.UNCERTAIN;return;}session.clearExpectedMenu(parent);state=State.COMPLETE;}
    case REJECTED->state=State.FAILED;case UNCERTAIN->state=State.UNCERTAIN;default->{}
   }return;
  }
  if(!plan.stillMatches(game.inventory())){cancel();return;}
  if(state==State.WAIT_SIGN&&game.sign()){
   signIdentity=game.screenIdentity();if(signIdentity==null||!advance(State.WAIT_FINAL,now)||!root(now)||!game.amount(signIdentity,request.quantity())||!root(now)){cancel();}return;
  }
  var menu=game.menu(parent.lease().worldId());if(menu==null)return;
  if(!applied(menu,now))return;
  if(state==State.WAIT_FINAL){
   if(BazaarPurchaseView.inspect(menu,request).isEmpty())return;
   long purse=game.purse();if(purse<1||!root(now))return;
   origin=menu;originIdentity=game.menuIdentity();
   terminal=new BazaarPurchaseController(coordinator,session,observe,items,cursors,new BazaarPurchaseController.Purchase(parent,plan,request,budget,purse,opening,cursorOpening),game,clock);
   if(!advance(State.PURCHASING,now)){cancel();return;}
   terminal.buy(now);if(terminal.state()==BazaarPurchaseController.State.REJECTED)state=State.FAILED;else if(terminal.state()==BazaarPurchaseController.State.UNCERTAIN)state=State.UNCERTAIN;return;
  }
  if(state==State.OPENING){
   var button=BazaarNavigationView.search(menu,request);if(button.isPresent()){navigate(button.get(),State.WAIT_PRODUCT,now);return;}
   button=BazaarNavigationView.instant(menu,request);if(button.isPresent())navigate(button.get(),State.WAIT_QUANTITY,now);return;
  }
  if(state==State.WAIT_PRODUCT){var button=BazaarNavigationView.instant(menu,request);if(button.isPresent())navigate(button.get(),State.WAIT_QUANTITY,now);return;}
  if(state==State.WAIT_QUANTITY){var button=BazaarNavigationView.custom(menu,request);if(button.isPresent())navigate(button.get(),State.WAIT_SIGN,now);}
 }
 private void navigate(BazaarNavigationView.Button button,State next,long now){
  origin=button.ticket().observed();originIdentity=game.menuIdentity();
  if(!advance(next,now)){cancel();return;}
  var result=game.navigate(coordinator,parent.lease(),button.contract(),button.ticket(),()->guardedContext(now),()->guardedMenu(button.ticket().observed(),now),now);
  if(!active()||!root(now))return;if(result!=GuardedMenuAction.Result.SENT)cancel();
 }
 private AutomationContext guardedContext(long now){if(!root(now))throw new IllegalStateException("Acquisition parent changed");return observe.get();}
 private MenuSnapshot guardedMenu(MenuSnapshot expected,long now){var m=game.menu(parent.lease().worldId());return root(now)&&plan.stillMatches(game.inventory())&&expected.equals(m)&&applied(m,now)?m:null;}
 private boolean applied(MenuSnapshot m,long now){
  if(!BazaarNavigationView.complete(m)||m.worldId()!=parent.lease().worldId()||!cursors.emptyAfterOpening(cursorOpening,m.containerId(),now,1_000_000_000L))return false;
  var inventory=game.inventory();if(inventory==null)return false;
  for(int i=0;i<m.containerSlots();i++)if(!m.item(i).equals(items.after(opening,m.containerId(),i)))return false;
  for(int i=0;i<36;i++)if(!m.item(m.containerSlots()+i).equals(inventory.get(i<27?i+9:i-27)))return false;return root(now);
 }
 /** Read-only lease proof; this does not navigate, click or enter text. */
 public boolean ownsScreen(){
  try{
   if(!active()||!root(clock.getAsLong()))return false;
   var m=game.menu(parent.lease().worldId());var screen=game.screenIdentity();
   if(screen==null)return state==State.OPENING||state==State.WAIT_FINAL;
   if(game.sign())return state==State.WAIT_SIGN||state==State.WAIT_FINAL&&screen==signIdentity;
   if(m==null)return false;
   if(state==State.PURCHASING)return game.menuIdentity()==originIdentity&&m.containerId()==origin.containerId()&&m.title().equals(origin.title());
   if(origin!=null&&game.menuIdentity()==originIdentity&&origin.equals(m))return true;
   return switch(state){
    case OPENING->BazaarNavigationView.search(m,request).isPresent()||BazaarNavigationView.instant(m,request).isPresent();
    case WAIT_PRODUCT->BazaarNavigationView.instant(m,request).isPresent();case WAIT_QUANTITY->BazaarNavigationView.custom(m,request).isPresent();
    case WAIT_FINAL->BazaarPurchaseView.inspect(m,request).isPresent();default->false;
   };
  }catch(RuntimeException unavailable){return false;}
 }
 private boolean advance(State next,long now){state=next;phaseStart=now;return session.expectScreenSequence(parent,this::ownsScreen,now,10_000_000_000L);}
 private boolean root(long now){
  if(parent==null||!BUYERS.contains(parent.lease().owner())||!session.isCurrent(parent)||!coordinator.holdsIdentity(parent.lease())||level==null||game.level()!=level||!game.available()||!game.safe()||now-parent.lease().started()<0||now-parent.lease().started()>=parent.lease().lifetime())return false;
  var c=observe.get();return session.isCurrent(parent)&&coordinator.holdsIdentity(parent.lease())&&game.level()==level&&c!=null&&c.worldId()==parent.lease().worldId()&&(!parent.lease().owner().requiresGarden()||c.garden())&&(parent.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU);
 }
 public void onSystemMessage(long world,String text){if(state==State.PURCHASING&&terminal!=null)terminal.onSystemMessage(world,text);}
 public void cancel(){if(state==State.COMPLETE||state==State.UNCERTAIN||state==State.FAILED)return;if(terminal!=null){terminal.cancel();state=terminal.state()==BazaarPurchaseController.State.UNCERTAIN?State.UNCERTAIN:State.FAILED;}else state=State.FAILED;}
 private static final class NativeGame implements Game {
  private final Opener opener;private final LongSupplier purse;NativeGame(Opener opener,LongSupplier purse){this.opener=Objects.requireNonNull(opener);this.purse=Objects.requireNonNull(purse);}
  private Minecraft mc(){return Minecraft.getInstance();}
  public boolean available(){var m=mc();return m!=null&&m.isSameThread()&&m.player!=null&&m.level!=null&&m.gameMode!=null&&m.gui.overlay()==null;}
  public boolean safe(){var m=mc();return available()&&m.player.isAlive()&&!m.player.isSpectator()&&!m.player.isPassenger()&&!m.player.isSleeping()&&!m.player.isUsingItem()&&(m.gui.screen()==null||m.gui.screen() instanceof AbstractSignEditScreen||m.gui.screen() instanceof AbstractContainerScreen<?> s&&s.getMenu()==m.player.containerMenu);}
  public boolean cursorEmpty(){return available()&&mc().player.containerMenu.getCarried().isEmpty();}public Object level(){return available()?mc().level:null;}public Object menuIdentity(){return available()?mc().player.containerMenu:null;}public Object screenIdentity(){return available()?mc().gui.screen():null;}public boolean sign(){return screenIdentity() instanceof AbstractSignEditScreen;}
  public long purse(){return purse.getAsLong();}public Map<Integer,ItemSnapshot> inventory(){return VisitorGameView.inventory(mc());}public MenuSnapshot menu(long world){return AutomationGameView.menu(mc(),world);}
  public boolean open(AutomationSession.Handle h,BazaarAcquisitionPlan.Request r,long now){return opener.open(h,r,now);}
  public boolean amount(Object screen,int quantity){return AutomationMenuActions.enterBazaarAmountNative(screen,quantity);}
  public boolean close(Object expected,int id){if(!safe()||!cursorEmpty()||mc().player.containerMenu!=expected||mc().player.containerMenu.containerId!=id)return false;mc().player.closeContainer();return mc().gui.screen()==null;}
  public GuardedMenuAction.Result navigate(AutomationCoordinator c,AutomationCoordinator.Lease l,MenuContract contract,MenuContract.Ticket t,Supplier<AutomationContext> context,Supplier<MenuSnapshot> menus,long now){return AutomationMenuActions.purchaseClick(c,l,contract,t,context,menus,now);}
  public GuardedMenuAction.Result purchase(AutomationCoordinator c,AutomationCoordinator.Lease l,MenuContract contract,MenuContract.Ticket t,Supplier<AutomationContext> context,Supplier<MenuSnapshot> menus,long now){return AutomationMenuActions.purchaseClick(c,l,contract,t,context,menus,now);}
 }
}
