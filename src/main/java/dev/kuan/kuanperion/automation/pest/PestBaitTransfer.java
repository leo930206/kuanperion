// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.BooleanSupplier;
/** Evidence protocol for a DECLARED direct-item bait slot, not a live Trap parser or permission to click.
 * Native callers MUST prove role/capacity/material/zero cost before pickup; a requested amount cannot control a left-click.
 * Only the exact own server-proven remainder may return to the original emptied source. Unknown outcomes never repair/retry/close. */
public final class PestBaitTransfer {
 public enum State{IDLE,READY,ARMED,AWAITING,WAIT_TRAP,CONFIRMED,REJECTED,UNCERTAIN}
 public enum Step{PICKUP,DEPOSIT,RETURN}
 public record Rules(String baitId,int units,int maxSourceStack,Set<String> protectedUuids){
  public Rules{Objects.requireNonNull(baitId);protectedUuids=Set.copyOf(protectedUuids);if(!baitId.isEmpty()&&!baitId.matches("[A-Z0-9_:-]{1,100}")||units<0||units>64||maxSourceStack<0||maxSourceStack>64||units>maxSourceStack||protectedUuids.size()>128||protectedUuids.stream().anyMatch(s->s.isBlank()||s.length()>128))throw new IllegalArgumentException("Invalid bait authorization");}
  public static Rules defaults(){return new Rules("",0,0,Set.of());}
 }
 /** Capacity is declared input data only. No default/live layout or capacity is inferred here. */
 public record Layout(String title,int size,int baitSlot,int remainingCapacity){
  public Layout{if(title==null||!title.matches("(?:Pest Trap|Mouse Trap|Vermin Trap)(?: #(?:[1-9]|1[0-9]|2[0-4]))?")||size<9||size>54||size%9!=0||baitSlot<0||baitSlot>=size||remainingCapacity<1||remainingCapacity>64)throw new IllegalArgumentException("Invalid declared bait layout");}
  public MenuContract contract(){return new MenuContract(Set.of(title),size);}
 }
 public record TrapFrame(long world,long observedAt,PestTrapObservation traps){public TrapFrame{Objects.requireNonNull(traps);}}
 public static final class Plan {
  private final Map<Integer,ItemSnapshot> baseline;private final int source,trapId,units;private final Layout layout;private final MenuSnapshot menu;
  private Plan(Map<Integer,ItemSnapshot> inv,int source,int trapId,int units,Layout layout,MenuSnapshot menu){baseline=Map.copyOf(inv);this.source=source;this.trapId=trapId;this.units=units;this.layout=layout;this.menu=menu;}
  public Map<Integer,ItemSnapshot> baseline(){return baseline;}public ItemSnapshot item(){return baseline.get(source);}public int source(){return source;}public int units(){return units;}public Layout layout(){return layout;}
 }
 public static boolean eligible(Rules rules,Map<Integer,ItemSnapshot> inv){if(rules==null||rules.baitId().isEmpty()||rules.units()==0||!complete(inv))return false;var protection=new InventoryProtection(Set.of(rules.baitId()),rules.protectedUuids());for(int i=9;i<36;i++){var item=inv.get(i);if(protection.permits(item)&&item.uuid().isEmpty()&&item.count()>=rules.units()&&item.count()<=rules.maxSourceStack())return true;}return false;}
 public static Optional<Plan> select(Rules rules,int trapId,Layout layout,MenuSnapshot menu,Map<Integer,ItemSnapshot> inv,PestTrapObservation traps){
  if(rules==null||rules.baitId().isEmpty()||rules.units()==0||layout==null||!complete(inv)||trapId<1||trapId>24||traps==null||!traps.emptyKnown()||!traps.empty().contains(trapId)||!shape(layout,menu)||!playerMatches(menu,inv)||!empty(menu.item(layout.baitSlot())))return Optional.empty();
  if(layout.title().contains(" #")&&!layout.title().endsWith(" #"+trapId))return Optional.empty();var protection=new InventoryProtection(Set.of(rules.baitId()),rules.protectedUuids());
  for(int i=9;i<36;i++){var item=inv.get(i);if(protection.permits(item)&&item.uuid().isEmpty()&&item.count()<=rules.maxSourceStack()&&rules.units()==Math.min(item.count(),layout.remainingCapacity()))return Optional.of(new Plan(inv,i,trapId,rules.units(),layout,menu));}return Optional.empty();
 }
 private static boolean empty(ItemSnapshot i){return i!=null&&i.count()==0&&i.minecraftId().equals("minecraft:air")&&i.skyblockId().isEmpty()&&i.uuid().isEmpty();}
 private static boolean complete(Map<Integer,ItemSnapshot> inv){if(inv==null||inv.size()!=43)return false;for(int i=0;i<43;i++){var v=inv.get(i);if(v==null||v.count()>64||v.count()==0&&!empty(v))return false;}return true;}
 private static boolean shape(Layout l,MenuSnapshot m){if(m==null||m.containerId()<1||m.containerId()>255||!l.contract().matches(m)||m.slots().size()!=l.size()+36)return false;for(int i=0;i<l.size()+36;i++){var slot=m.slot(i);if(slot==null||slot.playerInventory()!=(i>=l.size()))return false;}return true;}
 private static boolean playerMatches(MenuSnapshot menu,Map<Integer,ItemSnapshot> inv){for(int i=0;i<36;i++)if(!Objects.equals(menu.item(menu.containerSlots()+i),inv.get(i<27?i+9:i-27)))return false;return true;}
 private static boolean same(ItemSnapshot before,ItemSnapshot after,int count){return count==0?empty(after):after!=null&&after.count()==count&&before.skyblockId().equals(after.skyblockId())&&before.minecraftId().equals(after.minecraftId())&&before.uuid().equals(after.uuid())&&before.fingerprint().equals(after.fingerprint())&&before.name().equals(after.name())&&before.lore().equals(after.lore())&&before.protectedItem()==after.protectedItem();}
 private static final long SECOND=1_000_000_000L,WINDOW=8*SECOND;
 private final InventoryReceipts items;private final CursorReceipts cursors;private final BooleanSupplier owned;private State state=State.IDLE;private Step step=Step.PICKUP;private Plan plan;private MenuContract.Ticket ticket;private InventoryReceipts.Stamp itemStamp;private CursorReceipts.Stamp cursorStamp;private long started,phaseAt,armedAt,depositAt;
 public PestBaitTransfer(InventoryReceipts items,CursorReceipts cursors,BooleanSupplier owned){this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.owned=Objects.requireNonNull(owned);}
 public State state(){return state;}public Step step(){return step;}public MenuContract.Ticket ticket(){return state==State.ARMED?ticket:null;}
 public boolean begin(Plan plan,MenuSnapshot menu,Map<Integer,ItemSnapshot> inv,ItemSnapshot cursor,long now){if(state!=State.IDLE||plan==null||!owned.getAsBoolean()||!plan.menu.equals(menu)||!plan.baseline.equals(inv)||!empty(cursor))return false;this.plan=plan;started=phaseAt=now;state=State.READY;return true;}
 private int sourceSlot(){return plan.layout.size()+plan.source-9;}
 private boolean scoped(MenuSnapshot menu,long world){return menu!=null&&world==plan.menu.worldId()&&menu.worldId()==world&&menu.containerId()==plan.menu.containerId()&&shape(plan.layout,menu);}
 private boolean stable(Map<Integer,ItemSnapshot> inv){if(!complete(inv))return false;for(int i=0;i<43;i++)if(i!=plan.source&&!plan.baseline.get(i).equals(inv.get(i)))return false;return true;}
 public boolean armNext(MenuSnapshot menu,Map<Integer,ItemSnapshot> inv,ItemSnapshot cursor,long now){
  if(state!=State.READY)return false;int remainder=plan.item().count()-plan.units;boolean sourceOk=step==Step.PICKUP?same(plan.item(),inv==null?null:inv.get(plan.source),plan.item().count()):empty(inv==null?null:inv.get(plan.source));
  boolean cursorOk=step==Step.PICKUP?empty(cursor):same(plan.item(),cursor,step==Step.DEPOSIT?plan.item().count():remainder);boolean baitOk=menu!=null&&(step==Step.RETURN?same(plan.item(),menu.item(plan.layout.baitSlot()),plan.units):empty(menu.item(plan.layout.baitSlot())));
  boolean evidenceOk=step==Step.PICKUP||cursors.after(cursorStamp,now,WINDOW).filter(r->same(plan.item(),r.item(),step==Step.DEPOSIT?plan.item().count():remainder)).isPresent()&&(step==Step.DEPOSIT?sourceReceipt(0):same(plan.item(),items.after(itemStamp,plan.menu.containerId(),plan.layout.baitSlot()),plan.units));
  if(!evidenceOk||!owned.getAsBoolean()||now-started<0||now-started>=30*SECOND||now-phaseAt<0||now-phaseAt>=WINDOW||!scoped(menu,plan.menu.worldId())||!stable(inv)||!playerMatches(menu,inv)||!sourceOk||!cursorOk||!baitOk){state=State.UNCERTAIN;return false;}
  // MenuContract.plan intentionally rejects empty menu buttons. A declared item deposit/return is not a button:
  // this structural ticket is data only; the future native carried-item gate must separately prove role/capacity/ownership.
  ticket=step==Step.PICKUP?plan.layout.contract().plan(menu,sourceSlot(),MenuContract.Click.PICKUP):new MenuContract.Ticket(menu,step==Step.DEPOSIT?plan.layout.baitSlot():sourceSlot(),MenuContract.Click.PICKUP);if(ticket==null){state=State.UNCERTAIN;return false;}armedAt=now;state=State.ARMED;return true;
 }
 public boolean sent(GuardedMenuAction.Result result,long now){
  if(state!=State.ARMED)return false;if(!owned.getAsBoolean()||now-armedAt<0||now-armedAt>=SECOND){state=State.UNCERTAIN;return false;}
  state=result==GuardedMenuAction.Result.SENT?State.AWAITING:result==GuardedMenuAction.Result.REJECTED?State.REJECTED:State.UNCERTAIN;ticket=null;if(state!=State.AWAITING)return false;
  // Real native adapter must record SENT on the client thread before any server packet can be applied.
  itemStamp=items.stamp(plan.menu.worldId());cursorStamp=cursors.stamp(plan.menu.worldId(),plan.menu.containerId(),now);phaseAt=now;if(step==Step.DEPOSIT)depositAt=now;return true;
 }
 private boolean sourceReceipt(int count){var active=items.after(itemStamp,plan.menu.containerId(),sourceSlot());var vanilla=items.after(itemStamp,0,plan.source);return same(plan.item(),active,count)||same(plan.item(),vanilla,count);}
 public void poll(long world,MenuSnapshot menu,Map<Integer,ItemSnapshot> inv,ItemSnapshot physicalCursor,TrapFrame traps,boolean manual,long now){
  if(state!=State.READY&&state!=State.ARMED&&state!=State.AWAITING&&state!=State.WAIT_TRAP)return;
  if(manual||!owned.getAsBoolean()||!scoped(menu,world)||!stable(inv)||!playerMatches(menu,inv)||now-started<0||now-started>=30*SECOND||now-phaseAt<0||now-phaseAt>=WINDOW||state==State.ARMED&&(now-armedAt<0||now-armedAt>=SECOND)){state=State.UNCERTAIN;return;}
  int remainder=plan.item().count()-plan.units;var source=inv.get(plan.source);var bait=menu.item(plan.layout.baitSlot());
  boolean sourceOk=step==Step.PICKUP?(empty(source)||same(plan.item(),source,plan.item().count())):step==Step.RETURN?(empty(source)||same(plan.item(),source,remainder)):empty(source);
  boolean cursorOk=step==Step.PICKUP?(empty(physicalCursor)||same(plan.item(),physicalCursor,plan.item().count())):step==Step.DEPOSIT?(same(plan.item(),physicalCursor,plan.item().count())||same(plan.item(),physicalCursor,remainder)):(empty(physicalCursor)||same(plan.item(),physicalCursor,remainder));
  boolean baitOk=step==Step.PICKUP?empty(bait):step==Step.DEPOSIT?(empty(bait)||same(plan.item(),bait,plan.units)):same(plan.item(),bait,plan.units);
  if(!sourceOk||!cursorOk||!baitOk){state=State.UNCERTAIN;return;}
  if(state==State.AWAITING){var cursor=cursors.after(cursorStamp,now,WINDOW);if(cursor.isEmpty())return;
   if(step==Step.PICKUP){if(empty(source)&&sourceReceipt(0)&&same(plan.item(),physicalCursor,plan.item().count())&&same(plan.item(),cursor.get().item(),plan.item().count())){step=Step.DEPOSIT;state=State.READY;phaseAt=now;}return;}
   if(step==Step.DEPOSIT){if(!empty(source)||!same(plan.item(),bait,plan.units)||!same(plan.item(),items.after(itemStamp,menu.containerId(),plan.layout.baitSlot()),plan.units)||!same(plan.item(),physicalCursor,remainder)||!same(plan.item(),cursor.get().item(),remainder))return;if(remainder>0){step=Step.RETURN;state=State.READY;phaseAt=now;return;}state=State.WAIT_TRAP;}
   else{if(!same(plan.item(),source,remainder)||!sourceReceipt(remainder)||!empty(physicalCursor)||!empty(cursor.get().item()))return;state=State.WAIT_TRAP;}
  }
  if(state==State.WAIT_TRAP&&same(plan.item(),source,remainder)&&empty(physicalCursor)&&cursors.emptyAfter(cursorStamp,now,WINDOW)&&traps!=null&&traps.world()==world&&now-traps.observedAt()>=0&&now-traps.observedAt()<=SECOND&&traps.observedAt()>depositAt&&traps.traps().emptyKnown()&&!traps.traps().empty().contains(plan.trapId))state=State.CONFIRMED;
 }
 /** Revalidate the settled proof before a separate caller closes its exact owned menu; no close authority is granted. */
 public boolean confirmedStillMatches(long world,MenuSnapshot menu,Map<Integer,ItemSnapshot> inv,ItemSnapshot cursor,TrapFrame traps,long now){
  return state==State.CONFIRMED&&owned.getAsBoolean()&&now-started>=0&&now-started<30*SECOND&&now-phaseAt>=0&&now-phaseAt<WINDOW&&scoped(menu,world)&&stable(inv)&&playerMatches(menu,inv)&&same(plan.item(),inv.get(plan.source),plan.item().count()-plan.units)&&same(plan.item(),menu.item(plan.layout.baitSlot()),plan.units)&&empty(cursor)&&cursors.emptyAfter(cursorStamp,now,WINDOW)&&traps!=null&&traps.world()==world&&now-traps.observedAt()>=0&&now-traps.observedAt()<=SECOND&&traps.observedAt()>depositAt&&traps.traps().emptyKnown()&&!traps.traps().empty().contains(plan.trapId);
 }
 public void cancel(){if(state!=State.CONFIRMED&&state!=State.REJECTED)state=State.UNCERTAIN;ticket=null;}
}
