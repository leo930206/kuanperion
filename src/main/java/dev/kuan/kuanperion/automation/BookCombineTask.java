// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** One protected pair, explicit zero-cost preview, server-acknowledged moves/result and owned recovery.
 * Native layout binding/UI remain a separate release gate; this class has no runtime caller. */
public final class BookCombineTask {
 public record Policy(Map<String,Integer> ceilings,Set<String> protectedUuids,int delayMillis,int timeoutSeconds,boolean onlyWhileFarming){
  public Policy{ceilings=Map.copyOf(ceilings);protectedUuids=Set.copyOf(protectedUuids);new BookCombinePlanner(ceilings,protectedUuids);if(protectedUuids.size()>128||delayMillis<500||delayMillis>5000||timeoutSeconds<10||timeoutSeconds>180)throw new IllegalArgumentException("Invalid book policy");}
  public static Policy defaults(){return new Policy(Map.of(),Set.of(),1000,60,true);}
  public InventoryProtection protection(){return new InventoryProtection(Set.of("ENCHANTED_BOOK"),protectedUuids);}
 }
 public enum State{IDLE,OPENING,WAIT_MENU,WAIT_FIRST,SECOND_READY,WAIT_SECOND,COMBINE_READY,WAIT_COMBINE,COLLECT_READY,WAIT_RESULT,RECOVERING,WAIT_RECOVERY,COMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public interface Port{
  AutomationContext context();MenuSnapshot menu();BookAnvilView view();Map<Integer,BookCombinePlanner.Book> inventoryBooks();
  boolean open(AutomationSession.Handle handle);
  GuardedMenuAction.Result transfer(AutomationSession.Handle handle,MenuContract.Ticket ticket,InventoryProtection rules,long now);
  GuardedMenuAction.Result combine(AutomationSession.Handle handle,MenuContract.Ticket ticket,long now);
  GuardedMenuAction.Result collect(AutomationSession.Handle handle,MenuContract.Ticket ticket,long now);
  GuardedMenuAction.Result reclaim(AutomationSession.Handle handle,MenuContract.Ticket ticket,InventoryProtection rules,long now);
  void closeOwnedMenu(int id);
 }
 private final AutomationSession session;private final InventoryReceipts receipts;private final Port port;private final BookAnvilView.Layout layout;
 private State state=State.IDLE,recoveryOutcome;private Policy policy;private BookCombinePlanner planner;private BookCombinePlanner.Plan plan;private AutomationSession.Handle handle;
 private long started,lastAction,recoveryStarted,world;private int container=-1,pendingSource,resultSlot,recoverSlot;private boolean inputPending,combineIssued,mayHoldBooks;
 private InventoryReceipts.Stamp stamp;private ItemSnapshot pendingItem;private Map<Integer,BookCombinePlanner.Book> baseline=Map.of();private final Map<Integer,BookCombinePlanner.Book> knownInputs=new LinkedHashMap<>();private BookCombinePlanner.Book recovering;
 public BookCombineTask(AutomationSession session,InventoryReceipts receipts,Port port,BookAnvilView.Layout layout){this.session=session;this.receipts=receipts;this.port=port;this.layout=layout;}
 public State state(){return state;}public boolean manualRecoveryRequired(){return mayHoldBooks;}public boolean active(){return state!=State.IDLE&&state!=State.COMPLETE&&state!=State.CANCELLED&&state!=State.TIMED_OUT&&state!=State.FAILED;}
 public boolean request(Policy policy,boolean manual,long now){
  if(policy==null)return false;var context=port.context();if(active()||!manual&&(mayHoldBooks||policy.onlyWhileFarming()&&!context.farmingActive()))return false;
  var planner=new BookCombinePlanner(policy.ceilings(),policy.protectedUuids());Optional<BookCombinePlanner.Plan> selected;try{selected=planner.select(new ArrayList<>(inventory().values()));}catch(RuntimeException invalid){return false;}if(selected.isEmpty())return false;
  var next=session.begin(AutomationFeature.BOOK_COMBINE,now,(policy.timeoutSeconds()+10)*1_000_000_000L);if(next==null)return false;
  this.policy=policy;this.planner=planner;plan=selected.get();handle=next;world=context.worldId();started=lastAction=now;container=-1;knownInputs.clear();inputPending=combineIssued=mayHoldBooks=false;baseline=Map.of();state=State.OPENING;
  session.expectMenu(handle,layout.contract(),port::menu);return true;
 }
 public void tick(long now){if(!active())return;try{runTick(now);}catch(RuntimeException invalid){finish(State.FAILED,now,false);}}
 private void runTick(long now){
  if(!active())return;var context=port.context();
  if(!session.isCurrent(handle)||context.worldId()!=world||context.hardBlocked()){finish(State.CANCELLED,now,false);return;}
  boolean recoveringState=state==State.RECOVERING||state==State.WAIT_RECOVERY;
  if(recoveringState){if(now-recoveryStarted<0||now-recoveryStarted>=10_000_000_000L){finish(recoveryOutcome,now,false);return;}}
  else if(now-started<0||now-started>=policy.timeoutSeconds()*1_000_000_000L){abort(State.TIMED_OUT,now);return;}
  if(context.blockers().contains(AutomationContext.Blocker.FOCUS))return;
  if(state==State.OPENING){if(!AutomationSession.forFeature(context,AutomationFeature.BOOK_COMBINE).canIssueCommand())return;boolean sent;try{sent=port.open(handle);}catch(RuntimeException failure){sent=false;}if(!sent){finish(State.FAILED,now,false);return;}state=State.WAIT_MENU;lastAction=now;return;}
  var view=observe();if(view==null){if(container>=0)finish(State.FAILED,now,false);return;}
  if(container<0)container=view.menu().containerId();if(view.menu().containerId()!=container){finish(State.FAILED,now,false);return;}
  if(state==State.WAIT_FIRST||state==State.WAIT_SECOND){
   if(!receipts.confirmsDecrease(stamp,container,pendingSource,pendingItem,1))return;
   var target=state==State.WAIT_FIRST?layout.firstInput():layout.secondInput();var expected=state==State.WAIT_FIRST?plan.first():plan.second();
   if(!acknowledgedBook(stamp,target,view)||!sameBook(expected,view.book(target)))return;
   inputPending=false;knownInputs.put(target,view.book(target));state=state==State.WAIT_FIRST?State.SECOND_READY:State.COMBINE_READY;lastAction=now;return;
  }
  if(state==State.WAIT_COMBINE){
   if(!cleared(stamp,layout.firstInput(),view)||!cleared(stamp,layout.secondInput(),view))return;
   if(resultArrived(stamp)){mayHoldBooks=false;finish(State.COMPLETE,now,true);return;}
   var result=view.book(layout.result());var acknowledged=receipts.after(stamp,container,layout.result());
   if(expectedResult(result)&&acknowledged!=null&&acknowledged.equals(result.item())){resultSlot=layout.result();state=State.COLLECT_READY;lastAction=now;}return;
  }
  if(state==State.WAIT_RESULT){if(cleared(stamp,resultSlot,view)&&resultArrived(stamp)){mayHoldBooks=false;finish(State.COMPLETE,now,true);}return;}
  if(state==State.WAIT_RECOVERY){
   if(cleared(stamp,recoverSlot,view)&&arrivedInput(stamp,recovering)){knownInputs.remove(recoverSlot);if(knownInputs.isEmpty()){mayHoldBooks=false;finish(recoveryOutcome,now,true);}else{state=State.RECOVERING;lastAction=now;}}
   return;
  }
  if(now-lastAction<policy.delayMillis()*1_000_000L)return;
  if(state==State.RECOVERING){
   if(knownInputs.isEmpty()){mayHoldBooks=false;finish(recoveryOutcome,now,true);return;}
   var e=knownInputs.entrySet().iterator().next();recoverSlot=e.getKey();recovering=e.getValue();if(!sameBook(recovering,view.book(recoverSlot))){finish(recoveryOutcome,now,false);return;}
   baseline=inventory();stamp=receipts.stamp(world);var ticket=layout.contract().plan(view.menu(),recoverSlot,MenuContract.Click.QUICK_MOVE);
   if(action(()->port.reclaim(handle,ticket,policy.protection(),now))!=GuardedMenuAction.Result.SENT){finish(recoveryOutcome,now,false);return;}state=State.WAIT_RECOVERY;lastAction=now;return;
  }
  if(state==State.WAIT_MENU||state==State.SECOND_READY){
   boolean first=state==State.WAIT_MENU;var wanted=first?plan.first():plan.second();
   if(first&&(!view.emptyInput(layout.firstInput())||!view.emptyInput(layout.secondInput())||!planner.stillMatches(plan,new ArrayList<>(inventory().values())))||!first&&!sameBook(plan.first(),view.book(layout.firstInput()))){abort(State.FAILED,now);return;}
   var ticket=view.inputTicket(wanted);if(ticket==null||!policy.protection().permits(ticket.observed().item(ticket.slot()))){abort(State.FAILED,now);return;}
   pendingSource=ticket.slot();pendingItem=ticket.observed().item(ticket.slot());stamp=receipts.stamp(world);inputPending=mayHoldBooks=true;
   var sent=action(()->port.transfer(handle,ticket,policy.protection(),now));if(sent!=GuardedMenuAction.Result.SENT){finish(State.FAILED,now,false);return;}state=first?State.WAIT_FIRST:State.WAIT_SECOND;lastAction=now;return;
  }
  if(state==State.COMBINE_READY){
   if(!acknowledgedBook(stamp,layout.preview(),view)||!acknowledgedSlot(stamp,layout.combine(),view))return;
   var ticket=view.freeCombine();if(!sameBook(plan.first(),view.book(layout.firstInput()))||!sameBook(plan.second(),view.book(layout.secondInput()))||!expectedResult(view.book(layout.preview()))||ticket==null){abort(State.FAILED,now);return;}
   baseline=inventory();stamp=receipts.stamp(world);combineIssued=true;
   if(action(()->port.combine(handle,ticket,now))!=GuardedMenuAction.Result.SENT){finish(State.FAILED,now,false);return;}state=State.WAIT_COMBINE;lastAction=now;return;
  }
  if(state==State.COLLECT_READY){
   if(!expectedResult(view.book(resultSlot))){finish(State.FAILED,now,false);return;}var ticket=layout.contract().plan(view.menu(),resultSlot,MenuContract.Click.QUICK_MOVE);stamp=receipts.stamp(world);
   if(action(()->port.collect(handle,ticket,now))!=GuardedMenuAction.Result.SENT){finish(State.FAILED,now,false);return;}state=State.WAIT_RESULT;lastAction=now;
  }
 }
 private Map<Integer,BookCombinePlanner.Book> inventory(){var items=Map.copyOf(port.inventoryBooks());if(items.size()>36||items.entrySet().stream().anyMatch(e->e.getKey()<0||e.getKey()>35||e.getValue().inventorySlot()!=e.getKey()||e.getValue().item().count()<1||e.getValue().item().count()>64||e.getValue().compatibility().length()>16384))throw new IllegalArgumentException("Invalid canonical book inventory");return items;}
 private BookAnvilView observe(){try{var v=port.view();return v!=null&&v.layout().equals(layout)&&v.menu().worldId()==world&&v.menu().equals(port.menu())?v:null;}catch(RuntimeException invalid){return null;}}
 private boolean cleared(InventoryReceipts.Stamp since,int slot,BookAnvilView view){var fresh=receipts.after(since,container,slot);if(fresh==null||!fresh.equals(view.menu().item(slot))||view.book(slot)!=null)return false;return fresh.count()==0||!fresh.identified()&&(layout.emptyMaterials().contains(fresh.minecraftId())||slot==layout.combine()&&fresh.minecraftId().equals("minecraft:anvil")&&fresh.name().equals("Combine Items"));}
 private static BookCombinePlanner.Book unit(BookCombinePlanner.Book b){if(b==null||b.item().count()<1||b.item().count()>64)return null;var i=b.item();return new BookCombinePlanner.Book(b.inventorySlot(),new ItemSnapshot(i.skyblockId(),i.minecraftId(),i.uuid(),i.name(),i.fingerprint(),1,i.lore(),i.protectedItem()),b.enchantments(),b.compatibility());}
 private boolean expectedResult(BookCombinePlanner.Book b){return b!=null&&b.item().count()==1&&planner.isExpectedResult(plan,b);}
 private boolean resultStack(BookCombinePlanner.Book b){return planner.isExpectedResult(plan,unit(b));}
 private static boolean sameBook(BookCombinePlanner.Book a,BookCombinePlanner.Book b){return a!=null&&b!=null&&b.item().count()==1&&!b.item().protectedItem()&&a.item().skyblockId().equals(b.item().skyblockId())&&a.item().minecraftId().equals(b.item().minecraftId())&&a.item().uuid().equals(b.item().uuid())&&a.item().name().equals(b.item().name())&&a.enchantments().equals(b.enchantments())&&a.compatibility().equals(b.compatibility());}
 private boolean acknowledgedSlot(InventoryReceipts.Stamp since,int slot,BookAnvilView view){
  var fresh=receipts.after(since,container,slot);return fresh!=null&&fresh.equals(view.menu().item(slot));
 }
 private boolean acknowledgedBook(InventoryReceipts.Stamp since,int slot,BookAnvilView view){return view.book(slot)!=null&&acknowledgedSlot(since,slot,view);}
 /** Only changed, server-observed destination slots contribute; predicted slots never borrow another receipt. */
 private boolean inventoryGain(InventoryReceipts.Stamp since,java.util.function.Predicate<BookCombinePlanner.Book> matches){
  var current=inventory();long delta=0;
  for(int raw=0;raw<36;raw++){
   var fresh=receipts.after(since,0,raw<9?36+raw:raw);if(fresh==null)continue;
   var next=current.get(raw);if(next!=null&&!fresh.equals(next.item()))return false;
   var old=baseline.get(raw);if(old!=null&&matches.test(old))delta-=old.item().count();
   if(next!=null&&matches.test(next))delta+=next.item().count();
  }
  return delta==1;
 }
 private boolean resultArrived(InventoryReceipts.Stamp since){return inventoryGain(since,this::resultStack);}
 private boolean arrivedInput(InventoryReceipts.Stamp since,BookCombinePlanner.Book wanted){return inventoryGain(since,b->sameBook(wanted,unit(b)));}
 private static GuardedMenuAction.Result action(java.util.function.Supplier<GuardedMenuAction.Result> action){try{var result=action.get();return result==null?GuardedMenuAction.Result.UNCERTAIN:result;}catch(RuntimeException failure){return GuardedMenuAction.Result.UNCERTAIN;}}
 public void cancel(long now){if(active())abort(State.CANCELLED,now);}
 private void abort(State terminal,long now){
  if(session.isCurrent(handle)&&!inputPending&&!combineIssued&&!knownInputs.isEmpty()&&observe()!=null){recoveryOutcome=terminal;recoveryStarted=now;state=State.RECOVERING;return;}
  finish(terminal,now,!mayHoldBooks);
 }
 private void finish(State terminal,long now,boolean safeClose){
  state=terminal;try{var view=observe();if(safeClose&&session.isCurrent(handle)&&container>=0&&view!=null&&view.menu().containerId()==container){port.closeOwnedMenu(container);if(port.menu()!=null)state=State.FAILED;}}catch(RuntimeException failure){state=State.FAILED;}
  finally{boolean completed=session.finish(handle,state==State.COMPLETE,now);if(state==State.COMPLETE&&!completed)state=State.FAILED;handle=null;knownInputs.clear();}
 }
}
