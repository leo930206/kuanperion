// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.flight.FlightAnchor;
import dev.kuan.kuanperion.automation.pest.PestTargets.Point;
import java.util.*;import java.util.function.*;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
/** Fixed-owner native Book port. Declared layout/policy and a typed opener are mandatory; command permission and live menu compatibility must still be verified. */
public final class BookMenuController implements BookCombineTask.Port {
 @FunctionalInterface public interface AnvilSender {boolean send(AutomationSession.Handle handle,long now);}
 /** Package-private fixture seam; production always uses the pinned native client implementation below. */
 interface Game {
  boolean available();Object level();Object menuIdentity();boolean idle();boolean safePlayer();boolean cursorEmpty();ItemSnapshot cursor();FlightAnchor pose();
  BookInventorySnapshot inventory();MenuSnapshot menu(long world);BookAnvilView anvil(long world,BookAnvilView.Layout layout);
  boolean click(MenuContract.Ticket ticket);boolean deposit(MenuContract.Ticket ticket,ItemSnapshot ownBook);void close();
 }
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;
 private final InventoryReceipts receipts;private final CursorReceipts cursors;private final BookAnvilView.Layout layout;private final BookCombineTask.Policy policy;private final BookCombinePlanner planner;
 private final AnvilSender sender;private final Game game;private final LongSupplier clock;
 private AutomationSession.Handle owner;private Object level,menuIdentity;private FlightAnchor pose;private BookInventorySnapshot lastInventory;private BookCombinePlanner.Plan plan;
 private InventoryReceipts.Stamp opening;private CursorReceipts.OpeningStamp openingCursor;private CursorReceipts.Stamp actionCursor;private int container=-1,transfers,reclaimed;private boolean combined,collected,uncertain;
 private BookReturnTransfer returning;private BookReturnTransfer.Plan returnPlan;
 public BookMenuController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,InventoryReceipts receipts,CursorReceipts cursors,BookAnvilView.Layout layout,BookCombineTask.Policy policy,AnvilSender sender){this(coordinator,session,observe,receipts,cursors,layout,policy,sender,new NativeGame(),System::nanoTime);}
 BookMenuController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,InventoryReceipts receipts,CursorReceipts cursors,BookAnvilView.Layout layout,BookCombineTask.Policy policy,AnvilSender sender,Game game,LongSupplier clock){
  this.coordinator=Objects.requireNonNull(coordinator);this.session=Objects.requireNonNull(session);this.observe=Objects.requireNonNull(observe);this.receipts=Objects.requireNonNull(receipts);this.cursors=Objects.requireNonNull(cursors);this.layout=Objects.requireNonNull(layout);this.policy=Objects.requireNonNull(policy);this.sender=Objects.requireNonNull(sender);this.game=Objects.requireNonNull(game);this.clock=Objects.requireNonNull(clock);planner=new BookCombinePlanner(policy.ceilings(),policy.protectedUuids());
 }
 public AutomationContext context(){if(!game.available())return new AutomationContext(0,false,false,Set.of(AutomationContext.Blocker.DISCONNECTED));return Objects.requireNonNull(observe.get());}
 public boolean cursorEmpty(){return game.available()&&game.cursorEmpty();}
 public BookInventorySnapshot inventorySnapshot(){if(!game.available())throw new IllegalStateException("Book client unavailable");lastInventory=Objects.requireNonNull(game.inventory());return lastInventory;}
 public MenuSnapshot menu(){return game.available()?game.menu(context().worldId()):null;}
 private boolean root(AutomationSession.Handle h,long now){if(h==null||h.lease().owner()!=AutomationFeature.BOOK_COMBINE||!game.available()||!session.isCurrent(h))return false;var c=context();return c.worldId()==h.lease().worldId()&&coordinator.isOwner(h.lease(),c,now);}
 private boolean owns(AutomationSession.Handle h,long now){return h!=null&&h.equals(owner)&&root(h,now)&&game.level()==level&&game.safePlayer()&&pose.equals(game.pose());}
 private AutomationContext ownedContext(){var c=AutomationSession.forFeature(context(),AutomationFeature.BOOK_COMBINE);var flags=EnumSet.noneOf(AutomationContext.Blocker.class);flags.addAll(c.blockers());flags.remove(AutomationContext.Blocker.MENU);return new AutomationContext(c.worldId(),c.farmingActive(),c.garden(),flags);}
 public boolean open(AutomationSession.Handle h){
  long now=clock.getAsLong();if(!root(h,now)||owner!=null&&session.isCurrent(owner)||!game.idle()||!game.safePlayer()||!cursorEmpty()||lastInventory==null||!lastInventory.equals(game.inventory())||!coordinator.permits(h.lease(),AutomationCoordinator.Resource.COMMAND,AutomationSession.forFeature(context(),AutomationFeature.BOOK_COMBINE),now))return false;
  var selected=planner.select(new ArrayList<>(lastInventory.books().values()));if(selected.isEmpty())return false;
  owner=h;level=game.level();pose=game.pose();plan=selected.get();opening=receipts.stamp(h.lease().worldId());openingCursor=cursors.openingStamp(h.lease().worldId(),now);actionCursor=null;menuIdentity=null;container=-1;transfers=reclaimed=0;combined=collected=uncertain=false;returning=null;returnPlan=null;
  // Bind before the only callback; even a possibly-sent failure cannot be replayed under this root.
  try{boolean sent=session.isCurrent(h)&&sender.send(h,now);uncertain=!sent;return sent;}catch(RuntimeException failure){uncertain=true;throw failure;}
 }
 public BookAnvilView view(){
  long now=clock.getAsLong();if(!owns(owner,now)||uncertain||!cursorEmpty())return null;
  var v=game.anvil(owner.lease().worldId(),layout);if(v==null||!layout.equals(v.layout())||!v.menu().equals(menu())||container>=0&&(container!=v.menu().containerId()||menuIdentity!=game.menuIdentity())||!neutralGrid(v))return null;
  // Bind only a complete server opening. Once bound, pending observations remain readable: the task waits for its own fresh receipts.
  if(container<0&&(!cursors.emptyAfterOpening(openingCursor,v.menu().containerId(),now,10_000_000_000L)||!serverMatches(v)))return null;
  if(game.menuIdentity()==null||!session.isCurrent(owner))return null;if(container<0)menuIdentity=game.menuIdentity();container=v.menu().containerId();return v;
 }
 private boolean serverMatches(BookAnvilView v){for(var slot:v.menu().slots())if(!sameServerItem(slot,receipts.after(opening,v.menu().containerId(),slot.index())))return false;return true;}
 /** Packet snapshots lack slot-derived protection. Normalize only canonical hotbar aliases, retaining every item field. */
 static boolean sameServerItem(MenuSnapshot.Slot slot,ItemSnapshot packet){
  if(packet==null)return false;if(slot.playerInventory()&&slot.index()>=81&&slot.index()<90)packet=new ItemSnapshot(packet.skyblockId(),packet.minecraftId(),packet.uuid(),packet.name(),packet.fingerprint(),packet.count(),packet.lore(),true);
  return slot.item().equals(packet);
 }
 static boolean neutralGrid(BookAnvilView v){
  var l=v.layout();for(int i=0;i<54;i++){
   if(i==l.firstInput()||i==l.secondInput()||i==l.preview()||i==l.combine()||i==l.result())continue;
   var item=v.menu().item(i);if(item==null||!item.skyblockId().isEmpty()||!item.uuid().isEmpty())return false;
   boolean blank=item.name().isBlank()&&item.lore().stream().allMatch(String::isBlank);
   if(item.count()==0){if(!item.minecraftId().equals("minecraft:air")||!blank)return false;continue;}
   boolean pane=item.count()==1&&item.minecraftId().matches("minecraft:[a-z_]+_stained_glass_pane")&&blank;
   boolean close=item.count()==1&&item.minecraftId().equals("minecraft:barrier")&&item.name().equals("Close")&&item.lore().stream().allMatch(String::isBlank);
   if(!pane&&!close)return false;
  }return true;
 }
 private BookAnvilView ready(AutomationSession.Handle h,MenuContract.Ticket ticket,long now){
  if(!owns(h,now)||uncertain||lastInventory==null||!lastInventory.equals(game.inventory())||!cursorEmpty()||!ownedContext().canIssueCommand()||actionCursor!=null&&!cursors.emptyAfter(actionCursor,now,10_000_000_000L))return null;
  var v=view();return v!=null&&serverMatches(v)&&ticket!=null&&v.menu().equals(ticket.observed())&&layout.contract().revalidate(ticket,v.menu())?v:null;
 }
 private static boolean same(BookCombinePlanner.Book a,BookCombinePlanner.Book b){return a!=null&&b!=null&&a.item().equals(b.item())&&a.enchantments().equals(b.enchantments())&&a.compatibility().equals(b.compatibility());}
 private GuardedMenuAction.Result send(AutomationSession.Handle h,MenuContract.Ticket ticket,long now){
  var result=GuardedMenuAction.perform(coordinator,h.lease(),layout.contract(),ticket,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,this::context,this::menu,t->{
   // Final native boundary repeats complete43/menu/root/pose/cursor checks after the structural gate's observations.
   long sendAt=clock.getAsLong();if(!owns(h,sendAt)||!ownedContext().canIssueCommand()||!cursorEmpty()||!lastInventory.equals(game.inventory())||actionCursor!=null&&!cursors.emptyAfter(actionCursor,sendAt,10_000_000_000L))return false;
   var current=game.anvil(h.lease().worldId(),layout);if(current==null||menuIdentity!=game.menuIdentity()||!current.menu().equals(t.observed())||!neutralGrid(current)||!serverMatches(current))return false;
   if(actionCursor==null&&!cursors.emptyAfterOpening(openingCursor,container,sendAt,10_000_000_000L)||!session.isCurrent(h))return false;
   actionCursor=cursors.stamp(h.lease().worldId(),container,sendAt);
   return game.click(t);
  },now);uncertain=result!=GuardedMenuAction.Result.SENT;return result;
 }
 public GuardedMenuAction.Result transfer(AutomationSession.Handle h,MenuContract.Ticket ticket,InventoryProtection rules,long now){
  var v=ready(h,ticket,now);if(v==null||!policy.protection().equals(rules)||combined||reclaimed!=0||transfers>=2||ticket.click()!=MenuContract.Click.QUICK_MOVE)return GuardedMenuAction.Result.REJECTED;
  var wanted=transfers==0?plan.first():plan.second();if(!Objects.equals(v.inputTicket(wanted),ticket)||!rules.permits(wanted.item())||transfers==0&&(!v.emptyInput(layout.firstInput())||!v.emptyInput(layout.secondInput())||v.book(layout.preview())!=null||layout.resultSlots().stream().anyMatch(slot->v.book(slot)!=null))||transfers==1&&(!v.emptyInput(layout.secondInput())||!same(plan.first(),v.book(layout.firstInput()))))return GuardedMenuAction.Result.REJECTED;
  transfers++;return send(h,ticket,now);
 }
 public GuardedMenuAction.Result combine(AutomationSession.Handle h,MenuContract.Ticket ticket,long now){
  var v=ready(h,ticket,now);if(v==null||transfers!=2||combined||reclaimed!=0||!same(plan.first(),v.book(layout.firstInput()))||!same(plan.second(),v.book(layout.secondInput()))||!planner.isExpectedResult(plan,v.book(layout.preview()))||!Objects.equals(ticket,v.freeCombine()))return GuardedMenuAction.Result.REJECTED;
  combined=true;return send(h,ticket,now);
 }
 public GuardedMenuAction.Result collect(AutomationSession.Handle h,MenuContract.Ticket ticket,long now){
  var v=ready(h,ticket,now);if(v==null||!combined||collected||ticket.slot()!=v.resultSlot(planner,plan)||ticket.click()!=MenuContract.Click.PICKUP||!v.emptyInput(layout.firstInput())||!v.emptyInput(layout.secondInput())||!planner.isExpectedResult(plan,v.book(ticket.slot())))return GuardedMenuAction.Result.REJECTED;
  int destination=-1;for(int raw=9;raw<36;raw++)if(lastInventory.inventory().get(raw).count()==0){destination=raw;break;}
  var result=beginReturn(h,ticket,v.book(ticket.slot()),destination,now);if(result==GuardedMenuAction.Result.SENT)collected=true;return result;
 }
 public GuardedMenuAction.Result reclaim(AutomationSession.Handle h,MenuContract.Ticket ticket,InventoryProtection rules,long now){
  var v=ready(h,ticket,now);if(v==null||combined||!policy.protection().equals(rules)||ticket.click()!=MenuContract.Click.PICKUP)return GuardedMenuAction.Result.REJECTED;
  boolean first=ticket.slot()==layout.firstInput();int bit=first?1:2;var expected=first?plan.first():plan.second();
  if(!first&&ticket.slot()!=layout.secondInput()||(reclaimed&bit)!=0||transfers<(first?1:2)||!same(expected,v.book(ticket.slot()))||!rules.permits(v.book(ticket.slot()).item()))return GuardedMenuAction.Result.REJECTED;
  var result=beginReturn(h,ticket,expected,expected.inventorySlot(),now);if(result==GuardedMenuAction.Result.SENT)reclaimed|=bit;return result;
 }
 private BookAnvilView returnView(AutomationSession.Handle h,long now){
  if(!owns(h,now)||uncertain||!ownedContext().canIssueCommand()||menuIdentity!=game.menuIdentity())return null;
  var v=game.anvil(h.lease().worldId(),layout);return v!=null&&v.layout().equals(layout)&&v.menu().containerId()==container&&v.menu().equals(menu())&&neutralGrid(v)?v:null;
 }
 private GuardedMenuAction.Result beginReturn(AutomationSession.Handle h,MenuContract.Ticket ticket,BookCombinePlanner.Book expected,int destination,long now){
  var v=returnView(h,now);if(v==null||returning!=null&&!returning.confirmedStillMatches(v,lastInventory,game.cursor(),now))return GuardedMenuAction.Result.REJECTED;
  var selected=BookReturnTransfer.select(v,lastInventory,ticket.slot(),expected,destination,policy.protection());if(selected.isEmpty())return GuardedMenuAction.Result.REJECTED;
  var next=new BookReturnTransfer(receipts,cursors,()->owns(h,clock.getAsLong()));var p=selected.get();
  if(!next.begin(p,v,lastInventory,game.cursor(),now)||!next.armNext(v,lastInventory,game.cursor(),now)||!ticket.equals(next.ticket()))return GuardedMenuAction.Result.REJECTED;
  returning=next;returnPlan=p;var result=sendReturn(h,next.ticket(),now);return next.sent(result,now)?result:GuardedMenuAction.Result.UNCERTAIN;
 }
 private GuardedMenuAction.Result sendReturn(AutomationSession.Handle h,MenuContract.Ticket ticket,long now){
  boolean deposit=returning.step()==BookReturnTransfer.Step.DEPOSIT;
  var result=GuardedMenuAction.perform(coordinator,h.lease(),layout.contract(),ticket,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,this::context,this::menu,t->{
   long at=clock.getAsLong();var v=returnView(h,at);var current=game.inventory();var cursor=game.cursor();
   if(v==null||!lastInventory.equals(current)||!serverMatches(v)||!returning.armedStillMatches(v,current,cursor,at)||!t.equals(returning.ticket()))return false;
   if(!deposit&&(actionCursor==null?!cursors.emptyAfterOpening(openingCursor,container,at,10_000_000_000L):!cursors.emptyAfter(actionCursor,at,10_000_000_000L)))return false;
   if(!session.isCurrent(h)||menuIdentity!=game.menuIdentity())return false;actionCursor=cursors.stamp(h.lease().worldId(),container,at);
   return deposit?game.deposit(t,returnPlan.book().item()):game.click(t);
  },now);uncertain=result!=GuardedMenuAction.Result.SENT;return result;
 }
 public BookCombineTask.ReturnProgress progressReturn(AutomationSession.Handle h,long now){
  if(returning==null||returnPlan==null)return BookCombineTask.ReturnProgress.FAILED;
  var v=returnView(h,now);if(v==null){returning.cancel();return BookCombineTask.ReturnProgress.FAILED;}
  var current=game.inventory();var cursor=game.cursor();if(!lastInventory.equals(current)){returning.cancel();return BookCombineTask.ReturnProgress.FAILED;}
  returning.poll(v,current,cursor,false,now);
  if(returning.state()==BookReturnTransfer.State.CONFIRMED)return returning.confirmedStillMatches(v,current,cursor,now)?BookCombineTask.ReturnProgress.COMPLETE:BookCombineTask.ReturnProgress.FAILED;
  if(returning.state()==BookReturnTransfer.State.UNCERTAIN||returning.state()==BookReturnTransfer.State.REJECTED)return BookCombineTask.ReturnProgress.FAILED;
  if(returning.state()==BookReturnTransfer.State.READY){
   if(returning.step()!=BookReturnTransfer.Step.DEPOSIT||!returning.armNext(v,current,cursor,now))return BookCombineTask.ReturnProgress.FAILED;
   var result=sendReturn(h,returning.ticket(),now);if(!returning.sent(result,now))return BookCombineTask.ReturnProgress.FAILED;
  }return BookCombineTask.ReturnProgress.WAITING;
 }
 public void closeOwnedMenu(int id){
  long now=clock.getAsLong();var h=owner;if(!owns(h,now)||uncertain||id!=container||lastInventory==null||!lastInventory.equals(game.inventory())||!ownedContext().canIssueCommand()||!cursorEmpty()||actionCursor!=null&&!cursors.emptyAfter(actionCursor,now,10_000_000_000L))return;
  var v=view();if(v==null||!serverMatches(v)||v.menu().containerId()!=id||!v.emptyInput(layout.firstInput())||!v.emptyInput(layout.secondInput())||layout.resultSlots().stream().anyMatch(slot->v.book(slot)!=null)||returning!=null&&!returning.confirmedStillMatches(v,lastInventory,game.cursor(),now)||!coordinator.permits(h.lease(),AutomationCoordinator.Resource.SCREEN,ownedContext(),now))return;
  if(session.isCurrent(h)&&menuIdentity==game.menuIdentity())game.close();
 }
 private static final class NativeGame implements Game {
  private Minecraft mc(){return Minecraft.getInstance();}
  public boolean available(){var m=mc();return m!=null&&m.isSameThread()&&m.player!=null&&m.level!=null&&m.gameMode!=null;}
  public Object level(){return mc().level;}public Object menuIdentity(){return mc().player.containerMenu;}public boolean idle(){var m=mc();return available()&&m.gui.screen()==null&&m.gui.overlay()==null&&m.player.containerMenu==m.player.inventoryMenu;}
  public boolean cursorEmpty(){return available()&&mc().player.containerMenu.getCarried().isEmpty();}
  public ItemSnapshot cursor(){return AutomationGameView.item(mc().player.containerMenu.getCarried(),false);}
  public boolean safePlayer(){var m=mc();return available()&&!m.player.isSpectator()&&!m.player.isPassenger()&&!m.player.isUsingItem()&&!m.player.isSleeping()&&!manualInput(m);}
  private boolean manualInput(Minecraft m){
   if(m.options==null||m.getWindow()==null)return true;if(m.gui.screen()!=null)return false;
   for(var binding:List.of(m.options.keyUp,m.options.keyDown,m.options.keyLeft,m.options.keyRight,m.options.keyJump,m.options.keyShift,m.options.keySprint,m.options.keyAttack,m.options.keyUse)){
    if(binding.isUnbound())continue;var key=InputConstants.getKey(binding.saveString());long window=m.getWindow().handle();
    if(key.getType()==InputConstants.Type.KEYSYM){if(key.getValue()<0||GLFW.glfwGetKey(window,key.getValue())==GLFW.GLFW_PRESS)return true;}
    else if(key.getType()==InputConstants.Type.MOUSE){if(GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS)return true;}else return true;
   }return false;
  }
  public FlightAnchor pose(){var p=mc().player;return new FlightAnchor(new Point(p.getX(),p.getY(),p.getZ()),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),AutomationGameView.item(p.getMainHandItem(),true),p.getAbilities().flying);}
  public BookInventorySnapshot inventory(){return BookGameView.inventory(mc()).orElseThrow(()->new IllegalStateException("Unknown native book inventory"));}
  public MenuSnapshot menu(long world){return AutomationGameView.menu(mc(),world);}public BookAnvilView anvil(long world,BookAnvilView.Layout layout){return BookGameView.anvil(mc(),world,layout).orElse(null);}
  public boolean click(MenuContract.Ticket ticket){return AutomationMenuActions.sendNative(ticket);}public boolean deposit(MenuContract.Ticket ticket,ItemSnapshot ownBook){return AutomationMenuActions.depositBookNative(ticket,ownBook);}public void close(){mc().player.closeContainer();}
 }
}
