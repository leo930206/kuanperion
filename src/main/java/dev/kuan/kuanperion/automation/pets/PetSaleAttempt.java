// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pets;

import dev.kuan.kuanperion.automation.*;
import java.util.*;
import java.util.function.Supplier;

/** One parent-owned sale outcome proof. Declared facts are NOT a George parser, transfer driver or click permission. */
public final class PetSaleAttempt {
 public enum State {IDLE,SENDING,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 /** An explicit minimum and actual native quote, never a market estimate or price inferred from a name. */
 public record Quote(PetSalePreflight.Pet pet,long minimumPayout,long payout) {
  public Quote {
   Objects.requireNonNull(pet);
   if(minimumPayout<=0||payout<minimumPayout||payout>1_000_000_000_000_000L)throw new IllegalArgumentException("Invalid exact pet payout");
  }
 }
 /** Complete simultaneous projection; menu is a declared current scope, not semantic sale-button authority. */
 public record Frame(MenuSnapshot menu,Map<Integer,ItemSnapshot> inventory,boolean cursorEmpty,long observedAt) {
  public Frame {inventory=Map.copyOf(inventory);}
 }
 private static final long SECOND=1_000_000_000L,DEADLINE=15*SECOND;
 private final AutomationSession session;
 private final AutomationCoordinator coordinator;
 private final InventoryReceipts items;
 private final CursorReceipts cursors;
 private final Supplier<AutomationContext> observe;
 private State state=State.IDLE;
 private AutomationSession.Handle owner;
 private Quote quote;
 private PetSaleCustody.Token custody;
 private Frame before;
 private long started,noticeAt;
 private boolean saleNotice,confirmationUsed;
 private InventoryReceipts.Stamp itemStamp;
 private CursorReceipts.Stamp cursorStamp;private CursorReceipts.OpeningStamp closedCursor;
 public PetSaleAttempt(AutomationSession session,AutomationCoordinator coordinator,InventoryReceipts items,CursorReceipts cursors,Supplier<AutomationContext> observe) {
  this.session=Objects.requireNonNull(session);this.coordinator=Objects.requireNonNull(coordinator);this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.observe=Objects.requireNonNull(observe);
 }
 public State state(){return state;}
 /** Native caller must independently authorize and atomically revalidate its exact sale operation. No acquisition, transfer, command or parent release here. */
 public boolean start(AutomationSession.Handle owner,PetSalePreflight.Plan plan,Quote quote,Frame frame,long now,Supplier<GuardedMenuAction.Result> guarded) {
  if(state!=State.IDLE||owner==null||plan==null||quote==null||guarded==null||!plan.pets().contains(quote.pet())||!fresh(frame,now)||!frame.cursorEmpty()||!complete(frame.inventory())||!PetSalePreflight.stillMatches(plan,frame.inventory())||!aliases(frame)||!root(owner,frame.menu().worldId(),now))return false;
  if(state!=State.IDLE)return false; // A context read may have cancelled or accepted a nested attempt.
  return send(owner,quote,frame,now,guarded);
 }
 public boolean startCustody(AutomationSession.Handle owner,PetSaleCustody.Token custody,Quote quote,Frame frame,long now,Supplier<GuardedMenuAction.Result> guarded){
  if(state!=State.IDLE||owner==null||custody==null||quote==null||guarded==null||!custody.pet().equals(quote.pet())||!fresh(frame,now)||!frame.cursorEmpty()||!complete(frame.inventory())||!aliases(frame)||!root(owner,frame.menu().worldId(),now)||state!=State.IDLE||!custody.claim(owner,frame,now))return false;
  this.custody=custody;return send(owner,quote,frame,now,guarded);
 }
 private boolean send(AutomationSession.Handle owner,Quote quote,Frame frame,long now,Supplier<GuardedMenuAction.Result> guarded){
  this.owner=owner;this.quote=quote;before=frame;started=now;state=State.SENDING;
  GuardedMenuAction.Result result;
  try{result=guarded.get();}catch(RuntimeException uncertain){result=GuardedMenuAction.Result.UNCERTAIN;}
  if(state!=State.SENDING)return false;
  if(!session.isCurrent(owner)||!coordinator.holdsIdentity(owner.lease())){state=State.UNCERTAIN;return false;}
  if(result!=GuardedMenuAction.Result.SENT){state=result==GuardedMenuAction.Result.REJECTED?State.REJECTED:State.UNCERTAIN;return false;}
  // Sample after the native boundary. Reentrant/pre-send receipts and notices grant no delivery evidence.
  itemStamp=items.stamp(frame.menu().worldId());cursorStamp=cursors.stamp(frame.menu().worldId(),frame.menu().containerId(),now);closedCursor=cursors.observationStamp(frame.menu().worldId(),now);state=State.AWAITING;return true;
 }
 /** One source-marked same-custody confirmation, never a second sale authorization or a changed payout. */
 public boolean continueCustody(Frame frame,CursorReceipts.OpeningStamp opening,long now,Supplier<GuardedMenuAction.Result> guarded){
  if(state!=State.AWAITING||custody==null||confirmationUsed||saleNotice||guarded==null||now-started<0||now-started>=DEADLINE||!fresh(frame,now)||!frame.cursorEmpty()||!complete(frame.inventory())||!aliases(frame)||!before.inventory().equals(frame.inventory())||GeorgeSaleView.confirmation(frame.menu(),quote).isEmpty())return false;
  var originalBefore=before;
  if(!root(owner,before.menu().worldId(),now)||state!=State.AWAITING||confirmationUsed||saleNotice||before!=originalBefore||frame.menu().worldId()!=before.menu().worldId()||!cursors.emptyAfterOpening(opening,frame.menu().containerId(),now,SECOND))return false;
  for(int i=0;i<frame.menu().containerSlots();i++)if(!applied(items.after(itemStamp,frame.menu().containerId(),i),frame.menu().item(i)))return false;
  confirmationUsed=true;long originalStarted=started;boolean sent=send(owner,quote,frame,now,guarded);started=originalStarted;return sent;
 }
 /** Future adapter only: an exact, independently validated server sale acknowledgement, never local/player chat. No string format is assumed here. */
 public void onServerSale(long world,String petUuid,long payout,long observedAt) {
  if(state!=State.AWAITING||!session.isCurrent(owner)||!coordinator.holdsIdentity(owner.lease())||world!=before.menu().worldId()||observedAt-started<0||observedAt-started>=DEADLINE||payout!=quote.payout()||!quote.pet().metadata().uuid().equals(uuid(petUuid)))return;
  saleNotice=true;noticeAt=observedAt;
 }
 public void poll(Frame current,long now) {
  if(state!=State.AWAITING)return;
  boolean owns=root(owner,before.menu().worldId(),now);
  if(state!=State.AWAITING)return; // Never overwrite a cancellation/terminal result made during the read.
  if(now-started<0||now-started>=DEADLINE||!owns||!fresh(current,now)||!sameMenu(current.menu())||!complete(current.inventory())||!aliases(current)){state=State.UNCERTAIN;return;}
  if(custody!=null){
   if(!before.inventory().equals(current.inventory())){state=State.UNCERTAIN;return;}
   if(!saleNotice||now-noticeAt<0||!current.cursorEmpty()||!cursors.emptyAfter(cursorStamp,now,DEADLINE)||!air(current.menu().item(custody.inputSlot())))return;
   for(var slot:current.menu().slots())if(quote.pet().metadata().uuid().equals(uuid(slot.item().uuid())))return;
   if(applied(items.after(itemStamp,current.menu().containerId(),custody.inputSlot()),current.menu().item(custody.inputSlot())))state=State.CONFIRMED;return;
  }
  int soldSlot=quote.pet().slot();
  for(int slot=0;slot<43;slot++) {
   var old=before.inventory().get(slot);var next=current.inventory().get(slot);
   if(old.equals(next))continue;
   if(slot!=soldSlot||!air(next)){state=State.UNCERTAIN;return;}
  }
  if(!air(current.inventory().get(soldSlot))||!saleNotice||now-noticeAt<0||!current.cursorEmpty()||!cursors.emptyAfter(cursorStamp,now,DEADLINE))return;
  // A pet parked in a menu input/result is not sold merely because its main slot is empty.
  for(var slot:current.menu().slots())if(quote.pet().metadata().uuid().equals(uuid(slot.item().uuid())))return;
  if(!applied(items.after(itemStamp,0,soldSlot),current.inventory().get(soldSlot))||!applied(items.after(itemStamp,before.menu().containerId(),before.menu().containerSlots()+soldSlot-9),current.inventory().get(soldSlot)))return;
  state=State.CONFIRMED;
 }
 /** Only after the caller paired its own native Close HEAD with an applied vanilla TAIL.
  * Native GUI absence alone is never this proof. All43 new canonical items and window0 cursor are mandatory. */
 public void pollAfterAppliedClose(Frame current,long now){
  if(state!=State.AWAITING||custody==null)return;boolean owns=root(owner,before.menu().worldId(),now);if(state!=State.AWAITING)return;
  if(!owns||now-started<0||now-started>=DEADLINE||current==null||current.menu()!=null||now-current.observedAt()<0||now-current.observedAt()>SECOND||!complete(current.inventory())||!before.inventory().equals(current.inventory())||!current.cursorEmpty()){state=State.UNCERTAIN;return;}
  if(!saleNotice||now-noticeAt<0||!cursors.emptyAfterWindowZero(closedCursor,now,DEADLINE))return;
  for(int i=0;i<43;i++){var item=current.inventory().get(i);if(quote.pet().metadata().uuid().equals(uuid(item.uuid()))||!applied(items.after(itemStamp,0,i),item))return;}
  state=State.CONFIRMED;
 }
 /** Cancellation never closes, retries, repairs the cursor, releases the caller or restarts Farming. */
 public void cancel(){if(state==State.IDLE)state=State.REJECTED;else if(state==State.SENDING||state==State.AWAITING)state=State.UNCERTAIN;}
 private boolean root(AutomationSession.Handle handle,long world,long now) {
  if(handle==null||handle.lease().owner()!=AutomationFeature.PET_SELL||!session.isCurrent(handle)||!coordinator.holdsIdentity(handle.lease())||handle.lease().worldId()!=world||now-handle.lease().started()<0||now-handle.lease().started()>=handle.lease().lifetime())return false;
  AutomationContext current;
  try{current=observe.get();}catch(RuntimeException unavailable){return false;}
  return session.isCurrent(handle)&&coordinator.holdsIdentity(handle.lease())&&current!=null&&current.worldId()==world&&(handle.hold()==null||current.farmingActive())&&current.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU||b==AutomationContext.Blocker.INVENTORY_FULL);
 }
 private boolean sameMenu(MenuSnapshot menu){return menu!=null&&menu.worldId()==before.menu().worldId()&&menu.containerId()==before.menu().containerId()&&menu.containerSlots()==before.menu().containerSlots()&&menu.title().equals(before.menu().title());}
 static boolean fresh(Frame frame,long now){return frame!=null&&frame.menu()!=null&&frame.menu().containerId()>0&&frame.menu().containerId()<=255&&now-frame.observedAt()>=0&&now-frame.observedAt()<=SECOND;}
 static boolean complete(Map<Integer,ItemSnapshot> inv) {
  if(inv==null||inv.size()!=43)return false;var ids=new HashSet<String>();
  for(int slot=0;slot<43;slot++){var item=inv.get(slot);if(item==null||item.count()>64||item.count()==0&&!air(item))return false;if(!item.uuid().isBlank()){var identity=uuid(item.uuid());if(identity==null||!ids.add(identity))return false;}}
  return true;
 }
 static boolean aliases(Frame frame) {
  var menu=frame.menu();int n=menu.containerSlots();if(n<9||n>54||n%9!=0||menu.slots().size()!=n+36)return false;
  for(int i=0;i<n;i++){var slot=menu.slot(i);if(slot==null||slot.playerInventory())return false;}
  for(int i=0;i<36;i++){var slot=menu.slot(n+i);if(slot==null||!slot.playerInventory()||!slot.item().equals(frame.inventory().get(i<27?i+9:i-27)))return false;}
  return true;
 }
 private static String uuid(String raw){if(raw==null||!raw.matches("(?:[a-fA-F0-9]{32}|[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"))return null;return raw.replace("-","").toLowerCase(Locale.ROOT);}
 static boolean air(ItemSnapshot item){return item!=null&&item.count()==0&&item.minecraftId().equals("minecraft:air")&&item.skyblockId().isEmpty()&&item.uuid().isEmpty();}
 static boolean applied(ItemSnapshot receipt,ItemSnapshot current){return receipt!=null&&current!=null&&receipt.skyblockId().equals(current.skyblockId())&&receipt.minecraftId().equals(current.minecraftId())&&receipt.uuid().equals(current.uuid())&&receipt.name().equals(current.name())&&receipt.fingerprint().equals(current.fingerprint())&&receipt.count()==current.count()&&receipt.lore().equals(current.lore());}
}
