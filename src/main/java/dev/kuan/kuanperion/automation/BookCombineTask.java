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
 public enum ReturnProgress{WAITING,COMPLETE,FAILED}
 public interface Port{
  AutomationContext context();MenuSnapshot menu();BookAnvilView view();BookInventorySnapshot inventorySnapshot();boolean cursorEmpty();
  boolean open(AutomationSession.Handle handle);
  GuardedMenuAction.Result transfer(AutomationSession.Handle handle,MenuContract.Ticket ticket,InventoryProtection rules,long now);
  GuardedMenuAction.Result combine(AutomationSession.Handle handle,MenuContract.Ticket ticket,long now);
  GuardedMenuAction.Result collect(AutomationSession.Handle handle,MenuContract.Ticket ticket,long now);
  GuardedMenuAction.Result reclaim(AutomationSession.Handle handle,MenuContract.Ticket ticket,InventoryProtection rules,long now);
  ReturnProgress progressReturn(AutomationSession.Handle handle,long now);
  void closeOwnedMenu(int id);
 }
 private final AutomationSession session;private final InventoryReceipts receipts;private final CursorReceipts cursors;private final Port port;private final BookAnvilView.Layout layout;
 private State state=State.IDLE,recoveryOutcome;private Policy policy;private BookCombinePlanner planner;private BookCombinePlanner.Plan plan;private AutomationSession.Handle handle;
 private long started,lastAction,recoveryStarted,world;private int container=-1,pendingSource,pendingRaw,resultSlot,recoverSlot;private boolean inputPending,combineIssued,mayHoldBooks;
 private InventoryReceipts.Stamp stamp;private CursorReceipts.Stamp cursorStamp;private ItemSnapshot pendingItem;private BookInventorySnapshot expectedInventory;private final Map<Integer,BookCombinePlanner.Book> knownInputs=new LinkedHashMap<>();private BookCombinePlanner.Book recovering;
 public BookCombineTask(AutomationSession session,InventoryReceipts receipts,CursorReceipts cursors,Port port,BookAnvilView.Layout layout){this.session=Objects.requireNonNull(session);this.receipts=Objects.requireNonNull(receipts);this.cursors=Objects.requireNonNull(cursors);this.port=Objects.requireNonNull(port);this.layout=Objects.requireNonNull(layout);}
 public State state(){return state;}public boolean manualRecoveryRequired(){return mayHoldBooks;}public boolean active(){return state!=State.IDLE&&state!=State.COMPLETE&&state!=State.CANCELLED&&state!=State.TIMED_OUT&&state!=State.FAILED;}
 public boolean request(Policy policy,boolean manual,long now){
  if(active()||policy==null)return false;AutomationContext context;BookInventorySnapshot initial;
  var planner=new BookCombinePlanner(policy.ceilings(),policy.protectedUuids());Optional<BookCombinePlanner.Plan> selected;
  try{context=Objects.requireNonNull(port.context());if(!port.cursorEmpty()||!manual&&(mayHoldBooks||policy.onlyWhileFarming()&&!context.farmingActive()))return false;initial=inventory();selected=planner.select(new ArrayList<>(initial.books().values()));}catch(RuntimeException unavailable){return false;}
  if(selected.isEmpty())return false;
  var next=session.begin(AutomationFeature.BOOK_COMBINE,now,(policy.timeoutSeconds()+10)*1_000_000_000L);if(next==null)return false;
  this.policy=policy;this.planner=planner;plan=selected.get();handle=next;world=context.worldId();started=lastAction=now;container=-1;knownInputs.clear();inputPending=combineIssued=mayHoldBooks=false;expectedInventory=initial;cursorStamp=null;state=State.OPENING;
  session.expectMenu(handle,layout.contract(),port::menu);return true;
 }
 public void tick(long now){if(!active())return;try{runTick(now);}catch(RuntimeException invalid){if(!active())throw invalid;finish(State.FAILED,now,false);}}
 private void runTick(long now){
  if(!active())return;var context=port.context();
  if(!session.isCurrent(handle)||context.worldId()!=world||context.hardBlocked()){finish(State.CANCELLED,now,false);return;}
  var current=inventory();if(!allowedInventory(current)){finish(State.FAILED,now,false);return;}
  boolean recoveringState=state==State.RECOVERING||state==State.WAIT_RECOVERY;
  if(recoveringState){if(now-recoveryStarted<0||now-recoveryStarted>=10_000_000_000L){finish(recoveryOutcome,now,false);return;}}
  else if(now-started<0||now-started>=policy.timeoutSeconds()*1_000_000_000L){abort(State.TIMED_OUT,now);return;}
  if(context.blockers().contains(AutomationContext.Blocker.FOCUS))return;
  // A separately guarded return may hold only its own server-proven book between pickup and explicit main-slot deposit.
  if(state==State.WAIT_RESULT||state==State.WAIT_RECOVERY){var progress=port.progressReturn(handle,now);if(progress==null||progress==ReturnProgress.FAILED){finish(State.FAILED,now,false);return;}if(progress==ReturnProgress.WAITING)return;}
  boolean waitingCursor=state==State.WAIT_FIRST||state==State.WAIT_SECOND||state==State.WAIT_COMBINE||state==State.WAIT_RESULT||state==State.WAIT_RECOVERY;
  if(!port.cursorEmpty()){if(!waitingCursor)finish(State.CANCELLED,now,false);return;}

  if(state==State.OPENING){if(!AutomationSession.forFeature(context,AutomationFeature.BOOK_COMBINE).canIssueCommand())return;boolean sent;try{sent=port.open(handle);}catch(RuntimeException failure){sent=false;}if(!sent){finish(State.FAILED,now,false);return;}state=State.WAIT_MENU;lastAction=now;return;}
  var view=observe();if(view==null){if(container>=0)finish(State.FAILED,now,false);return;}
  if(container<0)container=view.menu().containerId();if(view.menu().containerId()!=container||!playerViewMatches(view,current)){finish(State.FAILED,now,false);return;}
  if(waitingCursor&&!confirmedCursor(now))return;
  if(state==State.WAIT_FIRST||state==State.WAIT_SECOND){
   if(!receipts.confirmsDecrease(stamp,container,pendingSource,pendingItem,1)||!receipts.confirmsDecrease(stamp,0,pendingRaw,pendingItem,1)||!Objects.equals(receipts.after(stamp,0,pendingRaw),current.inventory().get(pendingRaw)))return;
   var target=state==State.WAIT_FIRST?layout.firstInput():layout.secondInput();var expected=state==State.WAIT_FIRST?plan.first():plan.second();
   if(!acknowledgedBook(stamp,target,view)||!sameBook(expected,view.book(target)))return;
   expectedInventory=current;inputPending=false;knownInputs.put(target,view.book(target));state=state==State.WAIT_FIRST?State.SECOND_READY:State.COMBINE_READY;lastAction=now;return;
  }
  if(state==State.WAIT_COMBINE){
   if(!cleared(stamp,layout.firstInput(),view)||!cleared(stamp,layout.secondInput(),view))return;
   if(resultArrived(stamp,current)){mayHoldBooks=false;finish(State.COMPLETE,now,true);return;}
   var result=view.book(layout.result());var acknowledged=receipts.after(stamp,container,layout.result());
   if(expectedResult(result)&&acknowledged!=null&&acknowledged.equals(result.item())){resultSlot=layout.result();state=State.COLLECT_READY;lastAction=now;}return;
  }
  if(state==State.WAIT_RESULT){if(cleared(stamp,resultSlot,view)&&resultArrived(stamp,current)){mayHoldBooks=false;finish(State.COMPLETE,now,true);}return;}
  if(state==State.WAIT_RECOVERY){
   if(cleared(stamp,recoverSlot,view)&&arrivedInput(stamp,recovering,current)){expectedInventory=current;knownInputs.remove(recoverSlot);if(knownInputs.isEmpty()){mayHoldBooks=false;finish(recoveryOutcome,now,true);}else{state=State.RECOVERING;lastAction=now;}}
   return;
  }
  if(now-lastAction<policy.delayMillis()*1_000_000L)return;
  if(!confirmedCursor(now)){finish(State.FAILED,now,false);return;}
  if(state==State.RECOVERING){
   if(knownInputs.isEmpty()){mayHoldBooks=false;finish(recoveryOutcome,now,true);return;}
   var e=knownInputs.entrySet().iterator().next();recoverSlot=e.getKey();recovering=e.getValue();if(!sameBook(recovering,view.book(recoverSlot))){finish(recoveryOutcome,now,false);return;}
   arm(now);var ticket=layout.contract().plan(view.menu(),recoverSlot,MenuContract.Click.PICKUP);
   if(action(()->port.reclaim(handle,ticket,policy.protection(),now))!=GuardedMenuAction.Result.SENT){finish(recoveryOutcome,now,false);return;}state=State.WAIT_RECOVERY;lastAction=now;return;
  }
  if(state==State.WAIT_MENU||state==State.SECOND_READY){
   boolean first=state==State.WAIT_MENU;var wanted=first?plan.first():plan.second();
   if(first&&(!view.emptyInput(layout.firstInput())||!view.emptyInput(layout.secondInput())||!planner.stillMatches(plan,new ArrayList<>(current.books().values())))||!first&&!sameBook(plan.first(),view.book(layout.firstInput()))){abort(State.FAILED,now);return;}
   var ticket=view.inputTicket(wanted);if(ticket==null||!policy.protection().permits(ticket.observed().item(ticket.slot()))){abort(State.FAILED,now);return;}
   pendingRaw=wanted.inventorySlot();pendingSource=ticket.slot();pendingItem=ticket.observed().item(ticket.slot());arm(now);inputPending=mayHoldBooks=true;
   var sent=action(()->port.transfer(handle,ticket,policy.protection(),now));if(sent!=GuardedMenuAction.Result.SENT){finish(State.FAILED,now,false);return;}state=first?State.WAIT_FIRST:State.WAIT_SECOND;lastAction=now;return;
  }
  if(state==State.COMBINE_READY){
   if(!acknowledgedBook(stamp,layout.preview(),view)||!acknowledgedSlot(stamp,layout.combine(),view))return;
   var ticket=view.freeCombine();if(!sameBook(plan.first(),view.book(layout.firstInput()))||!sameBook(plan.second(),view.book(layout.secondInput()))||!expectedResult(view.book(layout.preview()))||ticket==null){abort(State.FAILED,now);return;}
   arm(now);combineIssued=true;
   if(action(()->port.combine(handle,ticket,now))!=GuardedMenuAction.Result.SENT){finish(State.FAILED,now,false);return;}state=State.WAIT_COMBINE;lastAction=now;return;
  }
  if(state==State.COLLECT_READY){
   if(!expectedInventory.stillMatches(current.inventory())||!expectedResult(view.book(resultSlot))){finish(State.FAILED,now,false);return;}var ticket=layout.contract().plan(view.menu(),resultSlot,MenuContract.Click.PICKUP);arm(now);
   if(action(()->port.collect(handle,ticket,now))!=GuardedMenuAction.Result.SENT){finish(State.FAILED,now,false);return;}state=State.WAIT_RESULT;lastAction=now;
  }
 }
 private void arm(long now){stamp=receipts.stamp(world);cursorStamp=cursors.stamp(world,container,now);}
 private boolean confirmedCursor(long now){return cursorStamp==null||cursors.emptyAfter(cursorStamp,now,10_000_000_000L);}
 private boolean safeCursor(long now){try{return port.cursorEmpty()&&confirmedCursor(now);}catch(RuntimeException unavailable){return false;}}
 private BookInventorySnapshot inventory(){var snapshot=Objects.requireNonNull(port.inventorySnapshot());if(snapshot.books().values().stream().anyMatch(b->b.compatibility().length()>16384))throw new IllegalArgumentException("Invalid canonical book inventory");return snapshot;}
 private boolean playerViewMatches(BookAnvilView view,BookInventorySnapshot current){for(int raw=0;raw<36;raw++){int slot=raw<9?layout.size()+27+raw:layout.size()+raw-9;if(!Objects.equals(view.menu().item(slot),current.inventory().get(raw)))return false;}return true;}
 private boolean allowedInventory(BookInventorySnapshot current){
  if(state==State.WAIT_FIRST||state==State.WAIT_SECOND){
   for(int raw=0;raw<43;raw++)if(raw!=pendingRaw&&!Objects.equals(expectedInventory.inventory().get(raw),current.inventory().get(raw)))return false;
   var source=current.inventory().get(pendingRaw);return source.equals(pendingItem)||source.equals(emptyItem());
  }
  if(state==State.WAIT_COMBINE||state==State.COLLECT_READY||state==State.WAIT_RESULT)return gainSlot(current,this::resultStack)!=-2;
  if(state==State.WAIT_RECOVERY)return gainSlot(current,b->sameBook(recovering,unit(b)))!=-2;
  return expectedInventory.stillMatches(current.inventory());
 }
 private static ItemSnapshot emptyItem(){return new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);}
 /** -1: unchanged; -2: invalid delta; otherwise the sole permitted main-inventory gain slot. */
 private int gainSlot(BookInventorySnapshot current,java.util.function.Predicate<BookCombinePlanner.Book> matches){
  int changed=-1;
  for(int raw=0;raw<43;raw++){
   var old=expectedInventory.inventory().get(raw);var next=current.inventory().get(raw);if(old.equals(next))continue;
   if(changed!=-1||raw<9||raw>=36)return -2;var book=current.books().get(raw);var before=expectedInventory.books().get(raw);
   if(book==null||!matches.test(book)||next.count()!=old.count()+1||old.count()>0&&(before==null||!matches.test(before)||!old.fingerprint().equals(next.fingerprint())||!old.uuid().equals(next.uuid())))return -2;
   changed=raw;
  }return changed;
 }

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
 /** A single exact gain, with every other main/equipment slot unchanged and its own server receipt. */
 private boolean inventoryGain(InventoryReceipts.Stamp since,BookInventorySnapshot current,java.util.function.Predicate<BookCombinePlanner.Book> matches){int raw=gainSlot(current,matches);return raw>=9&&Objects.equals(receipts.after(since,0,raw),current.inventory().get(raw));}
 private boolean resultArrived(InventoryReceipts.Stamp since,BookInventorySnapshot current){return inventoryGain(since,current,this::resultStack);}
 private boolean arrivedInput(InventoryReceipts.Stamp since,BookCombinePlanner.Book wanted,BookInventorySnapshot current){return inventoryGain(since,current,b->sameBook(wanted,unit(b)));}

 private static GuardedMenuAction.Result action(java.util.function.Supplier<GuardedMenuAction.Result> action){try{var result=action.get();return result==null?GuardedMenuAction.Result.UNCERTAIN:result;}catch(RuntimeException failure){return GuardedMenuAction.Result.UNCERTAIN;}}
 public void cancel(long now){if(active())abort(State.CANCELLED,now);}
 private void abort(State terminal,long now){
  if(session.isCurrent(handle)&&safeCursor(now)&&!inputPending&&!combineIssued&&!knownInputs.isEmpty()&&observe()!=null){recoveryOutcome=terminal;recoveryStarted=now;state=State.RECOVERING;return;}
  finish(terminal,now,!mayHoldBooks);
 }
 private void finish(State terminal,long now,boolean safeClose){
  state=terminal;try{var view=observe();if(safeClose&&safeCursor(now)&&session.isCurrent(handle)&&container>=0&&view!=null&&view.menu().containerId()==container){port.closeOwnedMenu(container);if(port.menu()!=null)state=State.FAILED;}else if(state==State.COMPLETE)state=State.FAILED;}catch(RuntimeException failure){state=State.FAILED;}
  finally{var done=handle;handle=null;knownInputs.clear();boolean completed=session.finish(done,state==State.COMPLETE,now);if(state==State.COMPLETE&&!completed)state=State.FAILED;}
 }
}
