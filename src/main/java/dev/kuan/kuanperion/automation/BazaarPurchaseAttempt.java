// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;import java.util.function.Supplier;
/** Parent-owned, one irreversible purchase boundary. Declared GUI facts are NOT a native parser or click authority. */
public final class BazaarPurchaseAttempt {
 public enum State{IDLE,ARMED,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 /** The future native adapter must independently prove this exact product/quantity/total from the current GUI. Not an API estimate. */
 public record Offer(BazaarPurchaseLedger.Order order,String minecraftId,int stackLimit,long purse){
  public Offer{Objects.requireNonNull(order);if(minecraftId==null||!minecraftId.matches("minecraft:[a-z0-9_]{1,100}")||minecraftId.equals("minecraft:air")||stackLimit<1||stackLimit>64||purse<0)throw new IllegalArgumentException("Invalid native purchase facts");}
 }
 public record Frame(long world,long observedAt,Map<Integer,ItemSnapshot> inventory,MenuSnapshot menu,boolean cursorEmpty){public Frame{inventory=Map.copyOf(inventory);}}
 private static final long SECOND=1_000_000_000L,DEADLINE=15*SECOND;
 private static final Set<AutomationFeature> BUYERS=Set.of(AutomationFeature.PEST,AutomationFeature.COMPOSTER,AutomationFeature.VISITOR,AutomationFeature.SUPERCRAFT);
 private final AutomationSession session;private final InventoryReceipts items;private final CursorReceipts cursors;private final SpendingBudget budget;private final Supplier<AutomationContext> observe;
 private State state=State.IDLE;private AutomationSession.Handle parent;private Offer offer;private MenuContract contract;private MenuContract.Ticket ticket;private Frame before;private long armedAt,sentAt;private InventoryReceipts.Stamp itemStamp;private CursorReceipts.Stamp cursorStamp;private CursorReceipts.OpeningStamp opening;private BazaarPurchaseLedger ledger;private boolean confirmationSent;private CursorReceipts.OpeningStamp afterSend;
 public BazaarPurchaseAttempt(AutomationSession session,InventoryReceipts items,CursorReceipts cursors,SpendingBudget budget,Supplier<AutomationContext> observe){this.session=Objects.requireNonNull(session);this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.budget=Objects.requireNonNull(budget);this.observe=Objects.requireNonNull(observe);}
 public State state(){return state;}
 /** No lease acquisition, menu claim, command, money hold or inventory effect. */
 public boolean arm(AutomationSession.Handle parent,Offer offer,MenuContract contract,MenuContract.Ticket ticket,CursorReceipts.OpeningStamp opening,Frame frame,long now){
  if(state!=State.IDLE||parent==null||offer==null||contract==null||ticket==null||ticket.click()!=MenuContract.Click.PICKUP||!session.isCurrent(parent)||!BUYERS.contains(parent.lease().owner())||!fresh(frame,now)||frame.world()!=parent.lease().worldId()||!frame.cursorEmpty()||!complete(frame.inventory())||!contract.revalidate(ticket,frame.menu())||ticket.slot()>=frame.menu().containerSlots()||!aliases(frame)||!cursors.emptyAfterOpening(opening,frame.menu().containerId(),now,SECOND)||!root(parent,now,false))return false;
  int empty=0;for(int slot=0;slot<43;slot++){var item=frame.inventory().get(slot);if(slot>=9&&slot<36&&item.count()==0)empty++;if(item.count()>0&&item.skyblockId().equals(offer.order().productId())&&(slot<9||slot>=36||item.protectedItem()||!item.uuid().isBlank()||!item.minecraftId().equals(offer.minecraftId())||item.count()>offer.stackLimit()))return false;}
  // Deliberately reserve capacity in explicitly empty main slots; never assume merging/hotbar/storage delivery.
  if((long)empty*offer.stackLimit()<offer.order().quantity())return false;
  this.parent=parent;this.opening=opening;this.offer=offer;this.contract=contract;this.ticket=ticket;before=frame;armedAt=now;state=State.ARMED;return true;
 }
 /** Caller supplies the actual guarded native result; that boundary MUST revalidate the same exact GUI immediately before send. */
 public boolean send(Frame current,long now,Supplier<GuardedMenuAction.Result> guarded){
  if(state!=State.ARMED)return false;
  if(guarded==null||now-armedAt<0||now-armedAt>=SECOND||!root(parent,now,false)||!fresh(current,now)||!current.cursorEmpty()||!before.inventory().equals(current.inventory())||!contract.revalidate(ticket,current.menu())||!aliases(current)||!cursors.emptyAfterOpening(opening,current.menu().containerId(),now,SECOND)){state=State.REJECTED;return false;}
  state=State.AWAITING;sentAt=now;ledger=new BazaarPurchaseLedger(budget,items,()->session.isCurrent(parent));
  var vanilla=new HashMap<Integer,ItemSnapshot>();for(int i=9;i<=44;i++)vanilla.put(i,current.inventory().get(i<36?i:i-36));
  boolean sent=ledger.begin(offer.order(),before.world(),offer.purse(),vanilla,now,guarded);
  if(state!=State.AWAITING){ledger.cancel();return false;}
  if(!sent){state=ledger.state()==BazaarPurchaseLedger.State.UNCERTAIN?State.UNCERTAIN:State.REJECTED;return false;}
  // Client-thread boundary: no predicted or reentrant pre-send receipt can prove delivery.
  itemStamp=items.stamp(before.world());afterSend=cursors.openingStamp(before.world(),now);cursorStamp=cursors.stamp(before.world(),before.menu().containerId(),now);return true;
 }
 /** One optional native confirmation of the same reserved transaction. Not a second order or refund. */
 public boolean canConfirm(){return state==State.AWAITING&&!confirmationSent&&ledger!=null&&ledger.awaitingWithoutReceipt();}
 public boolean confirm(Frame current,MenuContract next,MenuContract.Ticket button,java.math.BigDecimal total,long now,Supplier<GuardedMenuAction.Result> guarded){
  if(!canConfirm())return false;
  if(guarded==null||next==null||button==null||total==null||total.signum()<=0||total.compareTo(offer.order().maximumCost())>0||now-sentAt<0||now-sentAt>=DEADLINE||!root(parent,now,false)||!fresh(current,now)||!current.cursorEmpty()||current.world()!=before.world()||!before.inventory().equals(current.inventory())||current.menu().containerId()==before.menu().containerId()||!aliases(current)||button.click()!=MenuContract.Click.PICKUP||button.slot()>=current.menu().containerSlots()||!next.revalidate(button,current.menu())||!cursors.emptyAfterOpening(afterSend,current.menu().containerId(),now,DEADLINE)){cancel();return false;}
  confirmationSent=true;GuardedMenuAction.Result result;
  try{result=guarded.get();}catch(RuntimeException uncertain){result=GuardedMenuAction.Result.UNCERTAIN;}
  if(state!=State.AWAITING)return false;
  if(result!=GuardedMenuAction.Result.SENT||!root(parent,now,false)){cancel();return false;}
  before=current;contract=next;ticket=button;itemStamp=items.stamp(current.world());cursorStamp=cursors.stamp(current.world(),current.menu().containerId(),now);return true;
 }
 /** Only native server GAME messages, never local/player chat. Native routing remains the caller's responsibility. */
 public void onSystemMessage(long world,String text){if(state==State.AWAITING)ledger.onSystemMessage(world,text);}
 public void poll(Frame current,long now){
  if(state!=State.ARMED&&state!=State.AWAITING)return;
  long elapsed=now-(state==State.ARMED?armedAt:sentAt);
  if(elapsed<0||elapsed>=(state==State.ARMED?SECOND:DEADLINE)||!root(parent,now,state==State.AWAITING)){cancel();return;}
  if(state==State.ARMED)return;
  if(ledger.state()==BazaarPurchaseLedger.State.UNCERTAIN){state=State.UNCERTAIN;return;}
  if(!fresh(current,now)||current.world()!=before.world()||!complete(current.inventory())||!sameMenu(current.menu())||!aliases(current)){cancel();return;}
  if(!current.cursorEmpty()||!cursors.emptyAfter(cursorStamp,now,DEADLINE))return;
  int gained=0;for(int slot=0;slot<43;slot++){
   var old=before.inventory().get(slot);var next=current.inventory().get(slot);if(old.equals(next))continue;
   if(slot<9||slot>=36||next.count()<=old.count()||!next.identified()||next.protectedItem()||!next.uuid().isBlank()||!next.skyblockId().equals(offer.order().productId())||!next.minecraftId().equals(offer.minecraftId())||next.count()>offer.stackLimit()||old.count()>0&&!identity(old,next)){cancel();return;}
   if(old.count()>0&&!old.skyblockId().equals(offer.order().productId())){cancel();return;}
   gained+=next.count()-old.count();
   if(!applied(items.after(itemStamp,0,slot),next)||!applied(items.after(itemStamp,before.menu().containerId(),before.menu().containerSlots()+slot-9),next))return;
  }
  if(gained>offer.order().quantity()){cancel();return;}if(gained!=offer.order().quantity())return;
  ledger.poll(now);if(ledger.state()==BazaarPurchaseLedger.State.CONFIRMED)state=State.CONFIRMED;else if(ledger.state()==BazaarPurchaseLedger.State.UNCERTAIN)state=State.UNCERTAIN;
 }
 /** Never finishes/releases parent, closes a menu, repairs a cursor or refunds possibly spent coins. */
 public void cancel(){if(state==State.ARMED||state==State.IDLE)state=State.REJECTED;else if(state==State.AWAITING){ledger.cancel();state=State.UNCERTAIN;}}
 private boolean root(AutomationSession.Handle handle,long now,boolean delivered){
  if(!session.isCurrent(handle)||handle==null||now-handle.lease().started()<0||now-handle.lease().started()>=handle.lease().lifetime())return false;
  AutomationContext context;try{context=observe.get();}catch(RuntimeException unavailable){return false;}
  return session.isCurrent(handle)&&context!=null&&context.worldId()==handle.lease().worldId()&&(!handle.lease().owner().requiresGarden()||context.garden())&&(handle.hold()==null||context.farmingActive())&&context.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU||delivered&&b==AutomationContext.Blocker.INVENTORY_FULL);
 }
 private boolean sameMenu(MenuSnapshot menu){return menu!=null&&menu.worldId()==before.world()&&menu.containerId()==before.menu().containerId()&&menu.title().equals(before.menu().title())&&menu.containerSlots()==before.menu().containerSlots();}
 private static boolean fresh(Frame frame,long now){return frame!=null&&frame.world()>0&&frame.menu()!=null&&frame.menu().worldId()==frame.world()&&frame.menu().containerId()>0&&frame.menu().containerId()<=255&&now-frame.observedAt()>=0&&now-frame.observedAt()<=SECOND;}
 private static boolean complete(Map<Integer,ItemSnapshot> inventory){if(inventory==null||inventory.size()!=43)return false;var ids=new HashSet<String>();for(int i=0;i<43;i++){var x=inventory.get(i);if(x==null||x.count()>64||x.count()==0&&(!x.minecraftId().equals("minecraft:air")||!x.skyblockId().isEmpty()||!x.uuid().isEmpty())||!x.uuid().isBlank()&&!ids.add(x.uuid().replace("-","").toLowerCase(Locale.ROOT)))return false;}return true;}
 private static boolean aliases(Frame frame){var menu=frame.menu();if(menu==null||menu.containerSlots()<9||menu.containerSlots()>54||menu.containerSlots()%9!=0||menu.slots().size()!=menu.containerSlots()+36)return false;for(int i=0;i<menu.containerSlots();i++){var slot=menu.slot(i);if(slot==null||slot.playerInventory())return false;}for(int i=0;i<36;i++){var slot=menu.slot(menu.containerSlots()+i);if(slot==null||!slot.playerInventory()||!slot.item().equals(frame.inventory().get(i<27?i+9:i-27)))return false;}return true;}
 private static boolean identity(ItemSnapshot a,ItemSnapshot b){return a.skyblockId().equals(b.skyblockId())&&a.minecraftId().equals(b.minecraftId())&&a.uuid().equals(b.uuid())&&a.name().equals(b.name())&&a.fingerprint().equals(b.fingerprint())&&a.lore().equals(b.lore());}
 private static boolean applied(ItemSnapshot receipt,ItemSnapshot current){return receipt!=null&&identity(receipt,current)&&receipt.count()==current.count();}
}
