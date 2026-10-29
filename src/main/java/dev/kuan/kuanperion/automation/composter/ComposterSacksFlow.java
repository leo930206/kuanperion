// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.*;
/** One bounded original-parent Sacks insert. No client-predicted receipt, new root, inventory move, retry or Farm restart. */
public final class ComposterSacksFlow {
 public enum State{IDLE,WAIT_DETAIL,WAIT_RETURN,COMPLETE,FAILED,UNCERTAIN}
 public interface Port {
  boolean available();Object level();Object menuIdentity();MenuSnapshot menu();Map<Integer,ItemSnapshot> inventory();boolean cursorEmpty();AutomationContext context();
  GuardedMenuAction.Result click(MenuContract contract,MenuContract.Ticket ticket,Supplier<MenuSnapshot> proof,long now);
 }
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final AutomationSession.Handle parent;private final InventoryReceipts items;private final CursorReceipts cursors;private final Port port;private final LongSupplier clock;private final ComposterObservation.Kind kind;private final long ceiling;private final Object level;
 private InventoryReceipts.Stamp opening;private CursorReceipts.OpeningStamp cursorOpening;private State state=State.IDLE;private Map<Integer,ItemSnapshot> baseline;private ComposterObservation.Snapshot before;private MenuSnapshot origin;private Object originIdentity;private long phaseStart,increase;private boolean consumptionSent;
 public ComposterSacksFlow(AutomationCoordinator coordinator,AutomationSession session,AutomationSession.Handle parent,InventoryReceipts items,CursorReceipts cursors,InventoryReceipts.Stamp opening,CursorReceipts.OpeningStamp cursorOpening,Port port,LongSupplier clock,ComposterObservation.Kind kind,long ceiling){
  this.coordinator=Objects.requireNonNull(coordinator);this.session=Objects.requireNonNull(session);this.parent=parent;this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.opening=opening;this.cursorOpening=cursorOpening;this.port=Objects.requireNonNull(port);this.clock=Objects.requireNonNull(clock);this.kind=kind;this.ceiling=ceiling;level=port.level();
 }
 public State state(){return state;}public long increase(){return increase;}public boolean active(){return state==State.WAIT_DETAIL||state==State.WAIT_RETURN;}
 public boolean begin(long now){
  if(state!=State.IDLE)return false;
  try{
   if(!root(now)||kind==null||!port.cursorEmpty()){cancel();return false;}var inventory=port.inventory();if(inventory==null||inventory.size()!=43){cancel();return false;}
   for(int i=0;i<43;i++)if(inventory.get(i)==null){cancel();return false;}baseline=Map.copyOf(inventory);var m=port.menu();var button=ComposterSacksView.open(m,kind,ceiling).orElse(null);
   if(button==null||!applied(m,now)){cancel();return false;}before=ComposterObservation.inspect(m).orElseThrow();return navigate(button,State.WAIT_DETAIL,false,now);
  }catch(RuntimeException unavailable){cancel();return false;}
 }
 public void tick(long now){if(active())try{run(now);}catch(RuntimeException unavailable){cancel();}}
 private void run(long now){
  if(!root(now)||!port.cursorEmpty()||!baseline.equals(port.inventory())||now-phaseStart<0||now-phaseStart>=10_000_000_000L){cancel();return;}
  var menu=port.menu();if(menu==null||!applied(menu,now))return;
  if(state==State.WAIT_DETAIL){var button=ComposterSacksView.fill(menu,kind).orElse(null);if(button!=null)navigate(button,State.WAIT_RETURN,true,now);return;}
  var after=ComposterObservation.inspect(menu).orElse(null);if(after==null||!BazaarNavigationView.complete(menu)||after.organicSlot()!=before.organicSlot()||after.fuelSlot()!=before.fuelSlot())return;
  var old=kind==ComposterObservation.Kind.ORGANIC?before.organic():before.fuel();var current=kind==ComposterObservation.Kind.ORGANIC?after.organic():after.fuel();
  var otherBefore=kind==ComposterObservation.Kind.ORGANIC?before.fuel():before.organic();var otherAfter=kind==ComposterObservation.Kind.ORGANIC?after.fuel():after.organic();
  if(old.capacity()!=current.capacity()||!otherBefore.equals(otherAfter)||current.current()<old.current()){cancel();return;}
  long delta=current.current()-old.current();if(delta<=0)return;if(delta>ceiling){cancel();return;}increase=delta;state=State.COMPLETE;
 }
 private boolean navigate(ComposterSacksView.Button button,State next,boolean consuming,long now){
  var expected=button.ticket().observed();origin=expected;originIdentity=port.menuIdentity();state=next;phaseStart=now;
  if(originIdentity==null||!session.expectScreenSequence(parent,this::ownsScreen,now,10_000_000_000L)){cancel();return false;}
  GuardedMenuAction.Result result;
  try{result=port.click(button.contract(),button.ticket(),()->root(now)&&baseline.equals(port.inventory())&&port.menuIdentity()==originIdentity&&expected.equals(port.menu())&&applied(expected,now)?expected:null,now);}catch(RuntimeException unknown){if(consuming)consumptionSent=true;cancel();return false;}
  if(consuming&&result!=GuardedMenuAction.Result.REJECTED)consumptionSent=true;
  if(result!=GuardedMenuAction.Result.SENT||!root(now)){cancel();return false;}
  // Reset opening cursor scope only after the current-menu guard has used its receipt.
  opening=items.stamp(parent.lease().worldId());cursorOpening=cursors.openingStamp(parent.lease().worldId(),now);return true;
 }
 private boolean applied(MenuSnapshot menu,long now){
  if(!root(now)||!BazaarNavigationView.complete(menu)||menu.worldId()!=parent.lease().worldId()||!baseline.equals(port.inventory())||!cursors.emptyAfterOpening(cursorOpening,menu.containerId(),now,1_000_000_000L))return false;
  for(int i=0;i<menu.containerSlots();i++)if(!menu.item(i).equals(items.after(opening,menu.containerId(),i)))return false;
  for(int i=0;i<36;i++)if(!menu.item(menu.containerSlots()+i).equals(baseline.get(i<27?i+9:i-27)))return false;return true;
 }
 /** Read-only screen ownership, not click authorization. Real user close revokes the session before vanilla closes. */
 public boolean ownsScreen(){try{
  if(!active()||!root(clock.getAsLong())||!baseline.equals(port.inventory()))return false;var menu=port.menu();if(menu==null||port.menuIdentity()==null)return false;
  if(port.menuIdentity()==originIdentity&&origin.equals(menu))return true;
  return state==State.WAIT_DETAIL?ComposterSacksView.fill(menu,kind).isPresent():ComposterSacksView.open(menu,kind,ceiling).isPresent()||ComposterObservation.inspect(menu).filter(s->s.menu().title().equals("Composter")&&BazaarNavigationView.complete(menu)).isPresent();
 }catch(RuntimeException unavailable){return false;}}
 private boolean root(long now){
  if(parent==null||parent.lease().owner()!=AutomationFeature.COMPOSTER||!session.isCurrent(parent)||!coordinator.holdsIdentity(parent.lease())||!port.available()||level==null||port.level()!=level||now-parent.lease().started()<0||now-parent.lease().started()>=parent.lease().lifetime())return false;
  var c=port.context();return c!=null&&c.worldId()==parent.lease().worldId()&&c.garden()&&(parent.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU);
 }
 public void cancel(){if(state!=State.COMPLETE)state=consumptionSent?State.UNCERTAIN:State.FAILED;}
}
