// SPDX-License-Identifier: GPL-3.0-only
// NPC sell entry adapted from Aether AutoSellManager; rewritten with exact authorization/receipts.
package dev.kuan.kuanperion.automation;
import java.util.*;
/** Bounded command/menu/receipt state machine. Never treats local prediction as a completed sale. */
public final class AutoSellTask {
 public enum State {IDLE,OPENING,WAITING_MENU,READY,WAITING_RECEIPT,COMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public interface Port {
  AutomationContext context();MenuSnapshot menu();List<ItemSnapshot> inventory();
  boolean command(AutomationSession.Handle handle,AutomationCommand command);
  boolean sell(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection protection,long now);
  void closeOwnedMenu(int containerId);
 }
 private static final long MS=1000000L,SECOND=1000000000L;
 public static final MenuContract COOKIE=new MenuContract(Set.of("Booster Cookie"),Set.of(9,18,27,36,45,54));
 private final AutomationSession session;private final InventoryReceipts receipts;private final Port port;
 private AutomationSession.Handle handle;private AutoSellPolicy policy=AutoSellPolicy.defaults();private State state=State.IDLE;
 private long started,lastAction,world;private int ownedContainer=-1,soldStacks,soldItems,pendingSlot;private ItemSnapshot pending;private InventoryReceipts.Stamp stamp;
 public AutoSellTask(AutomationSession session,InventoryReceipts receipts,Port port){this.session=session;this.receipts=receipts;this.port=port;}
 public State state(){return state;}public int soldItems(){return soldItems;}public int soldStacks(){return soldStacks;}
 public boolean active(){return state==State.OPENING||state==State.WAITING_MENU||state==State.READY||state==State.WAITING_RECEIPT;}
 public boolean request(AutoSellPolicy policy,boolean manual,long now){
  var context=port.context();if(active()||policy.allowedIds().isEmpty()||!manual&&policy.onlyWhileFarming()&&!context.farmingActive()||port.inventory().stream().noneMatch(item->policy.protection().permits(item)&&item.count()<=policy.maxItems()))return false;
  var next=session.begin(AutomationFeature.AUTO_SELL,now,policy.timeoutSeconds()*SECOND);if(next==null)return false;
  this.policy=policy;handle=next;world=context.worldId();started=lastAction=now;soldStacks=soldItems=0;pending=null;ownedContainer=-1;state=State.OPENING;
  session.expectMenu(handle,COOKIE,port::menu);return true;
 }
 public void tick(long now){if(!active())return;try{runTick(now);}catch(RuntimeException failure){finish(State.FAILED,now);}}
 private void runTick(long now){
  if(!active())return;var context=port.context();
  if(!session.isCurrent(handle)||context.worldId()!=world||context.hardBlocked()){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=policy.timeoutSeconds()*SECOND){finish(State.TIMED_OUT,now);return;}
  if(context.blockers().contains(AutomationContext.Blocker.FOCUS))return;
  if(state==State.OPENING){if(!AutomationSession.forFeature(context,AutomationFeature.AUTO_SELL).canIssueCommand())return;
   if(!port.command(handle,AutomationCommand.BOOSTER_COOKIE)){finish(State.FAILED,now);return;}state=State.WAITING_MENU;lastAction=now;return;}
  var menu=port.menu();if(menu==null){if(ownedContainer>=0)finish(State.CANCELLED,now);return;}
  if(!COOKIE.matches(menu)||menu.worldId()!=world||menu.slots().size()!=menu.containerSlots()+36||ownedContainer>=0&&ownedContainer!=menu.containerId()){finish(State.FAILED,now);return;}
  ownedContainer=menu.containerId();
  if(state==State.WAITING_RECEIPT){
   if(receipts.confirmsDecrease(stamp,ownedContainer,pendingSlot,pending,pending.count())){soldStacks++;soldItems+=pending.count();pending=null;state=State.READY;lastAction=now;}
   return;
  }
  state=State.READY;
  if(now-lastAction<policy.delayMillis()*MS)return;
  if(soldStacks>=policy.maxStacks()||soldItems>=policy.maxItems()){finish(State.COMPLETE,now);return;}
  var candidate=menu.slots().stream().filter(s->s.playerInventory()&&policy.protection().permits(s.item())&&s.item().count()<=policy.maxItems()-soldItems).findFirst().orElse(null);
  if(candidate==null){finish(State.COMPLETE,now);return;}
  var ticket=COOKIE.plan(menu,candidate.index(),MenuContract.Click.QUICK_MOVE);if(ticket==null){finish(State.FAILED,now);return;}
  pending=candidate.item();pendingSlot=candidate.index();stamp=receipts.stamp(world);
  if(!port.sell(handle,COOKIE,ticket,policy.protection(),now)){finish(State.FAILED,now);return;}
  state=State.WAITING_RECEIPT;lastAction=now;
 }
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){
  state=terminal;
  try{
   var visible=port.menu();
   boolean owns=ownedContainer>=0&&session.isCurrent(handle)&&visible!=null&&visible.worldId()==world&&visible.containerId()==ownedContainer&&COOKIE.matches(visible)&&visible.slots().size()==visible.containerSlots()+36;
   if(owns){port.closeOwnedMenu(ownedContainer);if(port.menu()!=null)state=State.FAILED;}
   else if(state==State.COMPLETE)state=State.FAILED;
  }catch(RuntimeException failure){state=State.FAILED;}
  finally{boolean completed=session.finish(handle,state==State.COMPLETE,now);if(state==State.COMPLETE&&!completed)state=State.FAILED;handle=null;pending=null;ownedContainer=-1;}
 }
}
