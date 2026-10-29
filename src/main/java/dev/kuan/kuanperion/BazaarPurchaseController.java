// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.*;import net.minecraft.client.Minecraft;
/** Parent-owned terminal native buyer using provisional Aether roles; U-BZ-BUY/PRICE remain unverified live.
 * Does not open/navigate Bazaar, enter a sign, acquire/release roots, close/repair menus or save settings. */
public final class BazaarPurchaseController {
 public enum State{READY,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 /** Opening stamps must precede the actual parent's owned Bazaar opening; purse is the parent's observed balance. */
 public record Purchase(AutomationSession.Handle parent,BazaarAcquisitionPlan.Plan plan,BazaarAcquisitionPlan.Request request,SpendingBudget budget,long purse,InventoryReceipts.Stamp opening,CursorReceipts.OpeningStamp cursorOpening){}
 interface Game {
  boolean available();boolean safe();boolean cursorEmpty();Object level();Object menuIdentity();Map<Integer,ItemSnapshot> inventory();MenuSnapshot menu(long world);
  GuardedMenuAction.Result purchase(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,Supplier<AutomationContext> context,Supplier<MenuSnapshot> menus,long now);
 }
 private static final Set<AutomationFeature> BUYERS=Set.of(AutomationFeature.PEST,AutomationFeature.COMPOSTER,AutomationFeature.VISITOR,AutomationFeature.SUPERCRAFT);
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final InventoryReceipts items;private final CursorReceipts cursors;private final Purchase purchase;private final Game game;private final LongSupplier clock;private final Object level;private Object menuIdentity;
 private State state=State.READY;private BazaarPurchaseAttempt attempt;private boolean used;private BazaarPurchaseView.Quote quote;private InventoryReceipts.Stamp afterFirstSend;
 public record CompletionMenu(Object identity,int container){}
 private record Confirmation(BazaarPurchaseAttempt.Frame frame,BazaarPurchaseView.Quote quote,Object identity){}
 public CompletionMenu confirmedMenu(){return state==State.CONFIRMED?new CompletionMenu(menuIdentity,quote.ticket().observed().containerId()):null;}
 public BazaarPurchaseController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,InventoryReceipts items,CursorReceipts cursors,Purchase purchase){this(coordinator,session,observe,items,cursors,purchase,new NativeGame(),System::nanoTime);}
 BazaarPurchaseController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,InventoryReceipts items,CursorReceipts cursors,Purchase purchase,Game game,LongSupplier clock){
  this.coordinator=Objects.requireNonNull(coordinator);this.session=Objects.requireNonNull(session);this.observe=Objects.requireNonNull(observe);this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.purchase=Objects.requireNonNull(purchase);this.game=Objects.requireNonNull(game);this.clock=Objects.requireNonNull(clock);level=game.level();menuIdentity=game.menuIdentity();
 }
 public State state(){return state;}
 public GuardedMenuAction.Result buy(long now){
  if(used)return GuardedMenuAction.Result.REJECTED;used=true;state=State.REJECTED;
  try{
   var p=purchase;if(p.plan()==null||p.request()==null||p.budget()==null||!p.plan().requests().contains(p.request())||!p.request().usesBudget(p.budget())||p.purse()<1||p.opening()==null||p.cursorOpening()==null)return GuardedMenuAction.Result.REJECTED;
   var frame=frame(now);if(frame==null||!p.plan().stillMatches(frame.inventory())||!frame.cursorEmpty())return GuardedMenuAction.Result.REJECTED;
   quote=BazaarPurchaseView.inspect(frame.menu(),p.request()).orElse(null);if(quote==null)return GuardedMenuAction.Result.REJECTED;
   for(int i=0;i<frame.menu().containerSlots();i++)if(!frame.menu().item(i).equals(items.after(p.opening(),frame.menu().containerId(),i)))return GuardedMenuAction.Result.REJECTED;
   var request=p.request();attempt=new BazaarPurchaseAttempt(session,items,cursors,p.budget(),observe);
   var offer=new BazaarPurchaseAttempt.Offer(new BazaarPurchaseLedger.Order(request.productId(),request.displayName(),request.quantity(),quote.total()),request.material().minecraftId(),request.material().stackLimit(),p.purse());
   if(!attempt.arm(p.parent(),offer,quote.contract(),quote.ticket(),p.cursorOpening(),frame,now))return GuardedMenuAction.Result.REJECTED;
   state=State.AWAITING;
   attempt.send(frame,now,()->game.purchase(coordinator,p.parent().lease(),quote.contract(),quote.ticket(),this::guardedContext,this::guardedMenu,clock.getAsLong()));
   if(state==State.AWAITING)afterFirstSend=items.stamp(frame.world());
   update();return state==State.AWAITING?GuardedMenuAction.Result.SENT:state==State.UNCERTAIN?GuardedMenuAction.Result.UNCERTAIN:GuardedMenuAction.Result.REJECTED;
  }catch(RuntimeException unavailable){if(attempt!=null){attempt.cancel();update();}return state==State.UNCERTAIN?GuardedMenuAction.Result.UNCERTAIN:GuardedMenuAction.Result.REJECTED;}
 }
 /** Only the owning parent's actual server GAME event may route a message here, never local/player chat. */
 public void onSystemMessage(long world,String text){if(state!=State.AWAITING)return;if(!root(clock.getAsLong())&&confirmation(clock.getAsLong())==null){cancel();return;}attempt.onSystemMessage(world,text);}
 public void poll(long now){
  if(state!=State.AWAITING)return;
  try{
   if(!root(now)){
    var pending=confirmation(now);if(pending==null){cancel();return;}
    menuIdentity=pending.identity();quote=pending.quote();
    attempt.confirm(pending.frame(),quote.contract(),quote.ticket(),quote.total(),now,()->game.purchase(coordinator,purchase.parent().lease(),quote.contract(),quote.ticket(),this::guardedContext,this::guardedMenu,clock.getAsLong()));update();return;
   }
   var frame=frame(now);if(frame==null){cancel();return;}if(state!=State.AWAITING)return;attempt.poll(frame,now);update();}catch(RuntimeException unavailable){cancel();}
 }
 public void cancel(){if(state==State.CONFIRMED||state==State.UNCERTAIN||state==State.REJECTED)return;used=true;if(attempt==null){state=State.REJECTED;return;}attempt.cancel();update();}
 private void update(){state=switch(attempt.state()){case CONFIRMED->State.CONFIRMED;case UNCERTAIN->State.UNCERTAIN;case AWAITING->State.AWAITING;default->State.REJECTED;};}
 /** Passive ownership of the frozen transaction menu or one fully server-applied pending confirmation. */
 public boolean ownsScreen(){
  long now=clock.getAsLong();try{
   if(root(now)){var m=game.menu(purchase.parent().lease().worldId());return quote!=null&&m!=null&&m.worldId()==purchase.parent().lease().worldId()&&m.containerId()==quote.ticket().observed().containerId()&&m.title().equals(quote.ticket().observed().title());}
   return confirmation(now)!=null;
  }catch(RuntimeException unavailable){return false;}
 }
 private Confirmation confirmation(long now){
  if(state!=State.AWAITING||attempt==null||!attempt.canConfirm()||afterFirstSend==null||!baseRoot(now)||!game.cursorEmpty())return null;
  var identity=game.menuIdentity();var h=purchase.parent();var inventory=game.inventory();var menu=game.menu(h.lease().worldId());
  if(identity==null||identity==menuIdentity||menu==null||menu.worldId()!=h.lease().worldId()||menu.containerId()==quote.ticket().observed().containerId()||!purchase.plan().stillMatches(inventory)||!baseRoot(now)||game.menuIdentity()!=identity)return null;
  var next=BazaarPurchaseView.confirmation(menu,purchase.request(),quote).orElse(null);if(next==null)return null;
  for(int i=0;i<menu.containerSlots();i++)if(!menu.item(i).equals(items.after(afterFirstSend,menu.containerId(),i)))return null;
  return new Confirmation(new BazaarPurchaseAttempt.Frame(h.lease().worldId(),now,inventory,menu,true),next,identity);
 }
 private boolean root(long now){if(game.menuIdentity()!=menuIdentity)return false;return baseRoot(now);}
 private boolean baseRoot(long now){
  var h=purchase.parent();if(h==null||!BUYERS.contains(h.lease().owner())||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime()||!game.available()||!game.safe()||level==null||game.level()!=level||menuIdentity==null)return false;
  AutomationContext c;try{c=observe.get();}catch(RuntimeException unavailable){return false;}
  return session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&game.level()==level&&c!=null&&c.worldId()==h.lease().worldId()&&(!h.lease().owner().requiresGarden()||c.garden())&&(h.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU||state==State.AWAITING&&b==AutomationContext.Blocker.INVENTORY_FULL);
 }
 private BazaarPurchaseAttempt.Frame frame(long now){
  if(!root(now))return null;var h=purchase.parent();var inventory=game.inventory();var menu=game.menu(h.lease().worldId());boolean empty=game.cursorEmpty();
  if(inventory==null||menu==null||!root(now))return null;return new BazaarPurchaseAttempt.Frame(h.lease().worldId(),now,inventory,menu,empty);
 }
 private AutomationContext guardedContext(){if(!root(clock.getAsLong()))throw new IllegalStateException("Original Bazaar parent unavailable");var c=observe.get();if(!root(clock.getAsLong()))throw new IllegalStateException("Original Bazaar parent changed");return c;}
 private MenuSnapshot guardedMenu(){
  long now=clock.getAsLong();var f=frame(now);if(f==null||state!=State.AWAITING||!f.cursorEmpty()||!purchase.plan().stillMatches(f.inventory())||!f.menu().equals(quote.ticket().observed())||!cursors.emptyAfterOpening(purchase.cursorOpening(),f.menu().containerId(),now,1_000_000_000L))return null;
  // Exact immutable whole-menu equality preserves the parsed quote. Its own reservation is already held; do not count that budget twice.
  for(int i=0;i<f.menu().containerSlots();i++)if(!f.menu().item(i).equals(items.after(purchase.opening(),f.menu().containerId(),i)))return null;
  return root(now)?f.menu():null;
 }
 private static final class NativeGame implements Game {
  private Minecraft mc(){return Minecraft.getInstance();}
  public boolean available(){var m=mc();return m!=null&&m.isSameThread()&&m.player!=null&&m.level!=null&&m.gameMode!=null&&m.gui.overlay()==null;}
  public boolean safe(){var m=mc();return available()&&m.player.isAlive()&&!m.player.isSpectator()&&!m.player.isPassenger()&&!m.player.isSleeping()&&!m.player.isUsingItem()&&m.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen&&screen.getMenu()==m.player.containerMenu;}
  public boolean cursorEmpty(){return available()&&mc().player.containerMenu.getCarried().isEmpty();}
  public Object level(){return available()?mc().level:null;}public Object menuIdentity(){return available()?mc().player.containerMenu:null;}
  public Map<Integer,ItemSnapshot> inventory(){return VisitorGameView.inventory(mc());}public MenuSnapshot menu(long world){return AutomationGameView.menu(mc(),world);}
  public GuardedMenuAction.Result purchase(AutomationCoordinator c,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,Supplier<AutomationContext> context,Supplier<MenuSnapshot> menus,long now){return AutomationMenuActions.purchaseClick(c,lease,contract,ticket,context,menus,now);}
 }
}
