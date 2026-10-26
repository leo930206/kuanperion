// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** One authorized stack at a time, only at a configured Garden point, awaiting a server decrease. */
public final class JunkTask {
 public enum State {IDLE,OPENING,READY,WAITING_RECEIPT,COMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public interface Port {
  AutomationContext context();JunkPolicy.Location location();List<ItemSnapshot> inventory();MenuSnapshot menu();
  boolean open(AutomationSession.Handle handle);
  boolean drop(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection rules,long now);
  void closeOwnedMenu(int id);
 }
 private final AutomationSession session;private final InventoryReceipts receipts;private final Port port;
 private State state=State.IDLE;private JunkPolicy policy=JunkPolicy.defaults();private AutomationSession.Handle handle;private MenuContract contract;
 private long started,lastAction,world;private int removedStacks,removedItems,pendingSlot;private ItemSnapshot pending;private InventoryReceipts.Stamp stamp;
 public JunkTask(AutomationSession session,InventoryReceipts receipts,Port port){this.session=session;this.receipts=receipts;this.port=port;}
 public State state(){return state;}public int removedItems(){return removedItems;}public int removedStacks(){return removedStacks;}
 /** Already acquired data only; callers must not reread native context after a successful request. */
 public long worldId(){return session.isCurrent(handle)?handle.lease().worldId():0;}
 public boolean active(){return state==State.OPENING||state==State.READY||state==State.WAITING_RECEIPT;}
 public boolean request(JunkPolicy policy,boolean manual,long now){
  if(active()||policy==null||policy.allowedIds().isEmpty())return false;
  AutomationContext context;
  try{
   context=Objects.requireNonNull(port.context());
   if(!policy.atAnchor(port.location())||!context.garden()||!manual&&policy.onlyWhileFarming()&&!context.farmingActive()||List.copyOf(port.inventory()).stream().noneMatch(i->policy.protection().permits(i)&&i.count()<=policy.maxItems()))return false;
  }catch(RuntimeException unavailable){return false;}
  // Only preflight reads are caught; ownership and safety-stop failures must stay visible.
  var next=session.begin(AutomationFeature.JUNK,now,policy.timeoutSeconds()*1_000_000_000L);if(next==null)return false;
  handle=next;this.policy=policy;world=context.worldId();started=lastAction=now;removedItems=removedStacks=0;pending=null;contract=null;state=State.OPENING;return true;
 }
 public void tick(long now){
  if(!active())return;AutomationContext context;JunkPolicy.Location location;
  try{context=Objects.requireNonNull(port.context());location=port.location();}
  catch(RuntimeException unavailable){finish(State.FAILED,now);return;}
  if(!session.isCurrent(handle)||context.worldId()!=world||context.hardBlocked()||!context.garden()||!policy.atAnchor(location)){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=policy.timeoutSeconds()*1_000_000_000L){finish(State.TIMED_OUT,now);return;}
  if(context.blockers().contains(AutomationContext.Blocker.FOCUS))return;
  if(state==State.OPENING){
   if(!AutomationSession.forFeature(context,AutomationFeature.JUNK).canIssueCommand())return;
   boolean opened;try{opened=port.open(handle);}catch(RuntimeException failure){finish(State.FAILED,now);return;}
   MenuSnapshot openedMenu;
   try{openedMenu=port.menu();}catch(RuntimeException unavailable){finish(State.FAILED,now);return;}
   if(!opened||!session.isCurrent(handle)||!ownInventory(openedMenu)){finish(State.FAILED,now);return;}
   contract=new MenuContract(Set.of(openedMenu.title()),9);session.expectMenu(handle,contract,port::menu);state=State.READY;lastAction=now;return;
  }
  MenuSnapshot menu;
  try{menu=port.menu();}catch(RuntimeException unavailable){finish(State.FAILED,now);return;}
  if(!session.isCurrent(handle)||!ownInventory(menu)||!contract.matches(menu)){finish(State.FAILED,now);return;}
  if(state==State.WAITING_RECEIPT){
   if(receipts.confirmsDecrease(stamp,0,pendingSlot,pending,pending.count())){removedStacks++;removedItems+=pending.count();pending=null;state=State.READY;lastAction=now;}
   return;
  }
  if(now-lastAction<policy.delayMillis()*1_000_000L)return;
  if(removedStacks>=policy.maxStacks()||removedItems>=policy.maxItems()){finish(State.COMPLETE,now);return;}
  var candidate=menu.slots().stream().filter(s->s.playerInventory()&&s.index()>=9&&s.index()<=35&&policy.protection().permits(s.item())&&s.item().count()<=policy.maxItems()-removedItems).findFirst().orElse(null);
  if(candidate==null){finish(State.COMPLETE,now);return;}
  var ticket=contract.plan(menu,candidate.index(),MenuContract.Click.THROW);if(ticket==null){finish(State.FAILED,now);return;}
  pending=candidate.item();pendingSlot=candidate.index();stamp=receipts.stamp(world);
  boolean sent;try{sent=port.drop(handle,contract,ticket,policy.protection(),now);}catch(RuntimeException failure){sent=false;}
  if(!sent){finish(State.FAILED,now);return;}
  state=State.WAITING_RECEIPT;lastAction=now;
 }
 private boolean ownInventory(MenuSnapshot menu){return menu!=null&&menu.worldId()==world&&menu.containerId()==0&&menu.containerSlots()==9&&menu.slots().size()==46;}
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){
  state=terminal;
  try{if(contract!=null&&session.isCurrent(handle)&&ownInventory(port.menu())&&contract.matches(port.menu())){port.closeOwnedMenu(0);if(port.menu()!=null)state=State.FAILED;}}
  catch(RuntimeException failure){state=State.FAILED;}
  finally{session.finish(handle,state==State.COMPLETE,now);handle=null;pending=null;contract=null;}
 }
}
