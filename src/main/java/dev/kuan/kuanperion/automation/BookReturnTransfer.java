// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;import java.util.function.BooleanSupplier;
/** Exact one-book external pickup -> explicit empty main-slot deposit evidence. Native caller must additionally prove root/menu/role/policy; no effects here. */
public final class BookReturnTransfer {
 public enum State{IDLE,READY,ARMED,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 public enum Step{PICKUP,DEPOSIT}
 public static final class Plan {
  private final BookAnvilView view;private final BookInventorySnapshot inventory;private final BookCombinePlanner.Book book;private final int source,destination;
  private Plan(BookAnvilView view,BookInventorySnapshot inventory,BookCombinePlanner.Book book,int source,int destination){this.view=view;this.inventory=inventory;this.book=book;this.source=source;this.destination=destination;}
  public BookAnvilView view(){return view;}public BookInventorySnapshot inventory(){return inventory;}public BookCombinePlanner.Book book(){return book;}public int source(){return source;}public int destination(){return destination;}
 }
 public static Optional<Plan> select(BookAnvilView view,BookInventorySnapshot inventory,int source,BookCombinePlanner.Book expected,int destination,InventoryProtection rules){
  try{
   if(view==null||inventory==null||rules==null||expected==null||destination<9||destination>35||!playersMatch(view,inventory)||!empty(inventory.inventory().get(destination)))return Optional.empty();
   var l=view.layout();if(source!=l.firstInput()&&source!=l.secondInput()&&source!=l.result())return Optional.empty();
   var book=view.book(source);if(book==null||book.enchantments().isEmpty()||!same(expected,book)||!rules.permits(book.item())||!book.item().skyblockId().equals("ENCHANTED_BOOK")||!book.item().minecraftId().equals("minecraft:enchanted_book")||!book.item().name().equals("Enchanted Book")||book.item().count()!=1||book.compatibility().isBlank()||book.compatibility().length()>16384)return Optional.empty();
   String uuid=uuid(book.item());if(!uuid.isBlank()&&inventory.inventory().values().stream().anyMatch(i->uuid.equals(uuid(i))))return Optional.empty();
   return Optional.of(new Plan(view,inventory,book,source,destination));
  }catch(RuntimeException malformed){return Optional.empty();}
 }
 private static String uuid(ItemSnapshot i){return i.uuid().replace("-","").toLowerCase(Locale.ROOT);}
 private static boolean empty(ItemSnapshot i){return i!=null&&i.count()==0&&i.minecraftId().equals("minecraft:air")&&i.skyblockId().isEmpty()&&i.uuid().isEmpty();}
 private static boolean same(BookCombinePlanner.Book a,BookCombinePlanner.Book b){return a!=null&&b!=null&&a.item().equals(b.item())&&a.enchantments().equals(b.enchantments())&&a.compatibility().equals(b.compatibility());}
 private static boolean playersMatch(BookAnvilView view,BookInventorySnapshot inventory){
  var m=view.menu();if(m.containerId()<=0||m.containerId()>255||m.slots().size()!=90)return false;
  for(int i=0;i<90;i++){var s=m.slot(i);if(s==null||s.playerInventory()!=(i>=54))return false;}
  for(int raw=0;raw<36;raw++)if(!inventory.inventory().get(raw).equals(m.item(raw<9?81+raw:54+raw-9)))return false;return true;
 }
 private static final long S=1_000_000_000L,WINDOW=8*S;
 private final InventoryReceipts items;private final CursorReceipts cursors;private final BooleanSupplier owned;
 private State state=State.IDLE;private Step step=Step.PICKUP;private Plan plan;private MenuContract.Ticket ticket;
 private InventoryReceipts.Stamp itemStamp,pickupStamp;private CursorReceipts.Stamp cursorStamp,pickupCursor;private long started,phaseAt,armedAt;
 public BookReturnTransfer(InventoryReceipts items,CursorReceipts cursors,BooleanSupplier owned){this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.owned=Objects.requireNonNull(owned);}
 public State state(){return state;}public Step step(){return step;}public MenuContract.Ticket ticket(){return state==State.ARMED?ticket:null;}
 private boolean owns(){try{return owned.getAsBoolean();}catch(RuntimeException unavailable){return false;}}
 public boolean begin(Plan plan,BookAnvilView view,BookInventorySnapshot inventory,ItemSnapshot cursor,long now){
  if(state!=State.IDLE||plan==null||!owns()||!plan.view.equals(view)||!plan.inventory.equals(inventory)||!empty(cursor))return false;
  this.plan=plan;started=phaseAt=now;state=State.READY;return true;
 }
 private boolean scope(BookAnvilView view){return view!=null&&view.layout().equals(plan.view.layout())&&view.menu().worldId()==plan.view.menu().worldId()&&view.menu().containerId()==plan.view.menu().containerId();}
 private boolean stable(BookInventorySnapshot inventory){if(inventory==null)return false;for(int raw=0;raw<43;raw++)if(raw!=plan.destination&&!plan.inventory.inventory().get(raw).equals(inventory.inventory().get(raw)))return false;return true;}
 private boolean gain(BookInventorySnapshot inventory){return same(plan.book,inventory.books().get(plan.destination));}
 private boolean cleared(BookAnvilView view){
  var item=view.menu().item(plan.source);if(view.book(plan.source)!=null||item==null)return false;if(empty(item))return true;
  return item.count()==1&&!item.identified()&&item.uuid().isEmpty()&&(view.layout().emptyMaterials().contains(item.minecraftId())||plan.source==view.layout().combine()&&item.minecraftId().equals("minecraft:anvil")&&item.name().equals("Combine Items"));
 }
 private boolean sourceReceipt(BookAnvilView view){return cleared(view)&&Objects.equals(items.after(pickupStamp,view.menu().containerId(),plan.source),view.menu().item(plan.source));}
 private boolean destinationReceipt(BookAnvilView view,BookInventorySnapshot inventory){return gain(inventory)&&Objects.equals(items.after(itemStamp,view.menu().containerId(),54+plan.destination-9),inventory.inventory().get(plan.destination))&&Objects.equals(items.after(itemStamp,0,plan.destination),inventory.inventory().get(plan.destination));}
 private boolean timely(long now){return now-started>=0&&now-started<30*S&&now-phaseAt>=0&&now-phaseAt<WINDOW;}
 public boolean armNext(BookAnvilView view,BookInventorySnapshot inventory,ItemSnapshot physicalCursor,long now){
  if(state!=State.READY)return false;
  boolean evidence=step==Step.PICKUP?same(plan.book,view==null?null:view.book(plan.source))&&empty(physicalCursor):scope(view)&&sourceReceipt(view)&&plan.book.item().equals(physicalCursor)&&cursors.after(cursorStamp,now,WINDOW).filter(r->plan.book.item().equals(r.item())).isPresent();
  if(!owns()||!timely(now)||!scope(view)||!stable(inventory)||!playersMatch(view,inventory)||!empty(inventory.inventory().get(plan.destination))||!evidence){state=State.UNCERTAIN;return false;}
  int slot=step==Step.PICKUP?plan.source:54+plan.destination-9;
  // An empty explicit destination is item placement, not a MenuContract button permission. The native carried-item gate must authorize it separately.
  ticket=step==Step.PICKUP?view.layout().contract().plan(view.menu(),slot,MenuContract.Click.PICKUP):new MenuContract.Ticket(view.menu(),slot,MenuContract.Click.PICKUP);
  if(ticket==null){state=State.UNCERTAIN;return false;}itemStamp=items.stamp(view.menu().worldId());cursorStamp=cursors.stamp(view.menu().worldId(),view.menu().containerId(),now);if(step==Step.PICKUP){pickupStamp=itemStamp;pickupCursor=cursorStamp;}armedAt=now;state=State.ARMED;return true;
 }
 /** Revalidate the exact armed ticket immediately before a separately guarded native effect. This method sends nothing. */
 public boolean armedStillMatches(BookAnvilView view,BookInventorySnapshot inventory,ItemSnapshot physicalCursor,long now){
  if(state!=State.ARMED||!owns()||!timely(now)||now-armedAt<0||now-armedAt>=S||!scope(view)||!stable(inventory)||!playersMatch(view,inventory)||!empty(inventory.inventory().get(plan.destination))||ticket==null||!ticket.observed().equals(view.menu()))return false;
  return step==Step.PICKUP?same(plan.book,view.book(plan.source))&&empty(physicalCursor):sourceReceipt(view)&&plan.book.item().equals(physicalCursor)&&cursors.after(pickupCursor,now,WINDOW).filter(r->plan.book.item().equals(r.item())).isPresent();
 }
 public boolean sent(GuardedMenuAction.Result result,long now){
  if(state!=State.ARMED)return false;ticket=null;if(!owns()||now-armedAt<0||now-armedAt>=S){state=State.UNCERTAIN;return false;}
  state=result==GuardedMenuAction.Result.SENT?State.AWAITING:result==GuardedMenuAction.Result.REJECTED?State.REJECTED:State.UNCERTAIN;return state==State.AWAITING;
 }
 public void poll(BookAnvilView view,BookInventorySnapshot inventory,ItemSnapshot physicalCursor,boolean manual,long now){
  if(state!=State.READY&&state!=State.ARMED&&state!=State.AWAITING)return;
  if(manual||!owns()||!timely(now)||!scope(view)||!stable(inventory)||!playersMatch(view,inventory)||state==State.ARMED&&(now-armedAt<0||now-armedAt>=S)){state=State.UNCERTAIN;ticket=null;return;}
  boolean targetEmpty=empty(inventory.inventory().get(plan.destination)),targetGain=gain(inventory);
  boolean cursorEmpty=empty(physicalCursor),cursorBook=plan.book.item().equals(physicalCursor);
  if(!targetEmpty&&!targetGain||!cursorEmpty&&!cursorBook||step==Step.PICKUP&&!same(plan.book,view.book(plan.source))&&!cleared(view)||step==Step.DEPOSIT&&!cleared(view)){state=State.UNCERTAIN;ticket=null;return;}
  if(state!=State.AWAITING)return;var server=cursors.after(cursorStamp,now,WINDOW);if(server.isEmpty())return;
  // Direct fulfillment is accepted only when the server actually proves the exact authorized state; no such behavior is assumed.
  if(sourceReceipt(view)&&destinationReceipt(view,inventory)&&cursorEmpty&&empty(server.get().item())){state=State.CONFIRMED;return;}
  if(step==Step.PICKUP&&targetEmpty&&sourceReceipt(view)&&cursorBook&&plan.book.item().equals(server.get().item())){step=Step.DEPOSIT;state=State.READY;phaseAt=now;}
 }
 public boolean confirmedStillMatches(BookAnvilView view,BookInventorySnapshot inventory,ItemSnapshot physicalCursor,long now){return state==State.CONFIRMED&&owns()&&timely(now)&&scope(view)&&stable(inventory)&&playersMatch(view,inventory)&&sourceReceipt(view)&&destinationReceipt(view,inventory)&&empty(physicalCursor)&&cursors.emptyAfter(cursorStamp,now,WINDOW);}
 public void cancel(){if(state!=State.IDLE&&state!=State.REJECTED)state=State.UNCERTAIN;ticket=null;}
}
