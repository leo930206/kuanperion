// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.Supplier;
/** Source-style NONEMPTY button protocol. Bounds by the whole owned stack, never an invented server capacity.
 * No native input, cursor repair, menu close, root acquisition/release or parent/Farming restart here. */
public final class PestBaitButtonTransfer {
 public enum State{IDLE,READY,ARMED,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 public enum Step{PICKUP,DEPOSIT,RETURN}
 public record Frame(long world,long observedAt,Object level,Object menuIdentity,MenuSnapshot menu,Map<Integer,ItemSnapshot> inventory,ItemSnapshot cursor,PestTrapObservation traps,List<PestTrapTarget> targets){
  public Frame{inventory=Map.copyOf(inventory);Objects.requireNonNull(cursor);Objects.requireNonNull(traps);targets=List.copyOf(targets);}
 }
 /** Minted only by this original-root proof; native input must independently sample and claim it exactly once. */
 public final class Action {
  private final Step step;private final Frame snapshot;private final MenuContract contract;private final MenuContract.Ticket ticket;private boolean claimed;
  private Action(Frame f){step=PestBaitButtonTransfer.this.step;snapshot=f;contract=new MenuContract(Set.of(f.menu().title()),f.menu().containerSlots());int slot=step==Step.DEPOSIT?role.slot():f.menu().containerSlots()+plan.source()-9;ticket=new MenuContract.Ticket(f.menu(),slot,MenuContract.Click.PICKUP);}
  public Step step(){return step;}public Frame snapshot(){return snapshot;}public MenuContract contract(){return contract;}public MenuContract.Ticket ticket(){return ticket;}public AutomationSession.Handle owner(){return owner;}public ItemSnapshot originalItem(){return plan.item();}
  public boolean claim(AutomationSession.Handle h,Frame current,long now){
   long expected=revision;if(claimed||state!=State.ARMED||pending!=this||!Objects.equals(owner,h)||!root(now)||!scope(current,now)||!sameFrame(snapshot,current)||!actionReady(current,now)||claimed||revision!=expected||state!=State.ARMED||pending!=this)return false;claimed=true;return true;
  }
 }
 private static final long S=1_000_000_000L,WINDOW=8*S;private final AutomationSession session;private final AutomationCoordinator coordinator;private final InventoryReceipts items;private final CursorReceipts cursors;private final Supplier<AutomationContext> observe;
 private long revision;private ItemSnapshot settledRole;private State state=State.IDLE;private Step step=Step.PICKUP;private AutomationSession.Handle owner;private PestBaitButtonPlan plan;private PestTrapTarget target;private PestBaitButtonView.Role role;private Object level,identity;private long started,phaseAt;private InventoryReceipts.Stamp opening,itemStamp,pickupStamp;private CursorReceipts.OpeningStamp openingCursor;private CursorReceipts.Stamp cursorStamp;private int consumed=-1,expectedReturn=-1;private Action pending;
 public PestBaitButtonTransfer(AutomationSession session,AutomationCoordinator coordinator,InventoryReceipts items,CursorReceipts cursors,Supplier<AutomationContext> observe){this.session=session;this.coordinator=coordinator;this.items=items;this.cursors=cursors;this.observe=observe;}
 public State state(){return state;}public Step step(){return step;}public int consumed(){return consumed;}
 public boolean begin(AutomationSession.Handle h,PestBaitButtonPlan plan,PestTrapTarget target,PestBaitButtonView.Role role,Frame frame,InventoryReceipts.Stamp opening,CursorReceipts.OpeningStamp openingCursor,long now){
  if(state!=State.IDLE||h==null||plan==null||target==null||role==null||frame==null||!plan.stillMatches(frame.inventory())||!role.menu().equals(frame.menu()))return false;
  owner=h;this.plan=plan;this.target=target;this.role=role;level=frame.level();identity=frame.menuIdentity();this.opening=opening;this.openingCursor=openingCursor;started=phaseAt=now;
  long expected=++revision;boolean valid=root(now);if(revision!=expected)return false;
  if(!valid||!scope(frame,now)||!air(frame.cursor())||!cursors.emptyAfterOpening(openingCursor,frame.menu().containerId(),now,S)||PestBaitButtonView.initial(frame.menu(),target,frame.traps()).filter(r->r.slot()==role.slot()&&r.marker().equals(role.marker())).isEmpty()){state=State.REJECTED;return false;}
  for(var slot:frame.menu().slots())if(!raw(slot.item(),items.after(opening,frame.menu().containerId(),slot.index()))){state=State.REJECTED;return false;}
  state=State.READY;revision++;return true;
 }
 public Optional<Action> arm(Frame frame,long now){
  if(state!=State.READY)return Optional.empty();long expected=revision;boolean valid=root(now);if(revision!=expected)return Optional.empty();if(!valid||!scope(frame,now)||now-phaseAt<0||now-phaseAt>=WINDOW||!actionReady(frame,now)){cancel();return Optional.empty();}
  pending=new Action(frame);state=State.ARMED;revision++;return Optional.of(pending);
 }
 private boolean actionReady(Frame frame,long now){
  if(!unchangedOthers(frame))return false;var source=frame.inventory().get(plan.source());
  if(step==Step.PICKUP)return plan.stillMatches(frame.inventory())&&air(frame.cursor())&&cursors.emptyAfterOpening(openingCursor,frame.menu().containerId(),now,WINDOW)&&PestBaitButtonView.initial(frame.menu(),target,frame.traps()).filter(r->r.slot()==role.slot()).isPresent();
  var receipt=cursors.after(cursorStamp,now,WINDOW).orElse(null);if(receipt==null||!raw(frame.cursor(),receipt.item())||!own(frame.cursor())||frame.cursor().count()==0)return false;
  if(step==Step.DEPOSIT)return frame.cursor().count()==plan.maximumConsumption()&&air(source)&&sourceMatches(pickupStamp,frame)&&PestBaitButtonView.initial(frame.menu(),target,frame.traps()).filter(r->r.slot()==role.slot()).isPresent();
  return (air(source)||own(source))&&source.count()+frame.cursor().count()==expectedReturn&&expectedReturn<=plan.maximumConsumption()&&sourceMatches(pickupStamp,frame);
 }
 public boolean sent(Action action,GuardedMenuAction.Result result,long now){
  if(state!=State.ARMED||pending!=action)return false;long expected=revision;boolean valid=action.claimed&&root(now);if(revision!=expected)return false;if(!valid){cancel();return false;}
  if(result!=GuardedMenuAction.Result.SENT){state=result==GuardedMenuAction.Result.REJECTED&&step==Step.PICKUP?State.REJECTED:State.UNCERTAIN;pending=null;revision++;return false;}
  itemStamp=items.stamp(owner.lease().worldId());cursorStamp=cursors.stamp(owner.lease().worldId(),action.snapshot.menu().containerId(),now);if(step==Step.PICKUP)pickupStamp=itemStamp;phaseAt=now;state=State.AWAITING;pending=null;revision++;return true;
 }
 public void poll(Frame frame,long now){
  if(state!=State.AWAITING)return;long expected=revision;boolean valid=root(now);if(revision!=expected)return;if(!valid||!scope(frame,now)||!unchangedOthers(frame)||now-phaseAt<0||now-phaseAt>=WINDOW){cancel();return;}
  var source=frame.inventory().get(plan.source());if(!air(source)&&(!own(source)||source.count()>plan.maximumConsumption())){cancel();return;}
  var receipt=cursors.after(cursorStamp,now,WINDOW).orElse(null);
  if(receipt==null){
   // A normal left click on a nonempty fake GUI button can locally predict a marker swap.
   // This is wait-only: no marker is ever claimed as owned material or used for another action.
   if(!air(frame.cursor())&&!own(frame.cursor())&&!(step==Step.DEPOSIT&&raw(role.marker(),frame.cursor())))cancel();return;
  }
  if(!air(receipt.item())&&!own(receipt.item())){cancel();return;}if(!raw(frame.cursor(),receipt.item()))return;
  if(step==Step.PICKUP){
   if(air(source)&&sameCount(plan.item(),frame.cursor(),plan.maximumConsumption())&&sourceMatches(pickupStamp,frame)){step=Step.DEPOSIT;state=State.READY;phaseAt=now;revision++;}return;
  }
  if(step==Step.DEPOSIT){
   if(!sourceMatches(pickupStamp,frame))return;if(!air(source)&&!sourceMatches(itemStamp,frame))return;
   int used=plan.maximumConsumption()-source.count()-frame.cursor().count();if(used<0||used>plan.maximumConsumption()){cancel();return;}
   if(!knownRole(frame)||!frame.traps().emptyKnown()||frame.traps().empty().contains(target.trapId())||frame.observedAt()<=phaseAt)return;
   settledRole=frame.menu().item(role.slot());revision++;consumed=used;expectedReturn=plan.maximumConsumption()-used;
   if(frame.cursor().count()>0){step=Step.RETURN;state=State.READY;phaseAt=now;}else state=State.CONFIRMED;return;
  }
  if(air(frame.cursor())&&source.count()==expectedReturn&&sourceMatches(itemStamp,frame)&&frame.traps().emptyKnown()&&!frame.traps().empty().contains(target.trapId())&&knownRole(frame)){state=State.CONFIRMED;revision++;}
 }
 /** A terminal bit is not closing authority: all current world/menu/item/cursor evidence must still agree. */
 public boolean confirmedStillMatches(Frame frame,long now){
  if(state!=State.CONFIRMED)return false;long expected=revision;boolean valid=root(now);
  return revision==expected&&state==State.CONFIRMED&&valid&&scope(frame,now)&&unchangedOthers(frame)&&air(frame.cursor())&&cursors.emptyAfter(cursorStamp,now,WINDOW)&&frame.inventory().get(plan.source()).count()==expectedReturn&&sourceMatches(step==Step.RETURN?itemStamp:pickupStamp,frame)&&frame.traps().emptyKnown()&&!frame.traps().empty().contains(target.trapId())&&knownRole(frame);
 }
 private boolean sourceMatches(InventoryReceipts.Stamp stamp,Frame frame){int source=plan.source();var current=frame.inventory().get(source);return raw(current,items.after(stamp,0,source))&&raw(current,items.after(stamp,frame.menu().containerId(),frame.menu().containerSlots()+source-9));}
 private boolean knownRole(Frame frame){
  var item=frame.menu().item(role.slot());if(item==null||item.count()<1||item.count()>64||!item.name().equals("Trap Bait")||!item.uuid().isEmpty()||!item.skyblockId().isEmpty()&&!item.skyblockId().equals(plan.item().skyblockId()))return false;
  return raw(item,role.marker())||raw(item,settledRole)||raw(item,items.after(itemStamp,frame.menu().containerId(),role.slot()));
 }
 private boolean root(long now){
  var h=owner;if(h==null||h.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime())return false;
  AutomationContext c;try{c=observe.get();}catch(RuntimeException unavailable){return false;}
  return state!=State.UNCERTAIN&&state!=State.REJECTED&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&c!=null&&c.worldId()==h.lease().worldId()&&c.garden()&&(h.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU);
 }
 private boolean scope(Frame frame,long now){
  if(frame==null||frame.world()!=owner.lease().worldId()||frame.level()!=level||level==null||frame.menuIdentity()!=identity||identity==null||now-frame.observedAt()<0||now-frame.observedAt()>S||frame.menu()==null||frame.menu().worldId()!=frame.world()||frame.menu().containerId()!=role.menu().containerId()||frame.menu().containerSlots()!=role.menu().containerSlots()||!frame.menu().title().equals(role.menu().title())||!PestTrapTarget.unique(frame.targets()).contains(target)||frame.inventory().size()!=43||frame.menu().slots().size()!=frame.menu().containerSlots()+36)return false;
  for(int i=0;i<43;i++)if(frame.inventory().get(i)==null||frame.inventory().get(i).count()>64)return false;
  for(int i=0;i<frame.menu().containerSlots()+36;i++){var slot=frame.menu().slot(i);if(slot==null||slot.playerInventory()!=(i>=frame.menu().containerSlots()))return false;}
  for(int i=0;i<36;i++)if(!frame.menu().item(frame.menu().containerSlots()+i).equals(frame.inventory().get(i<27?i+9:i-27)))return false;return true;
 }
 private boolean unchangedOthers(Frame frame){for(int i=0;i<43;i++)if(i!=plan.source()&&!plan.baseline().get(i).equals(frame.inventory().get(i)))return false;return true;}
 private boolean sameFrame(Frame a,Frame b){return a.world()==b.world()&&a.level()==b.level()&&a.menuIdentity()==b.menuIdentity()&&a.menu().equals(b.menu())&&a.inventory().equals(b.inventory())&&raw(a.cursor(),b.cursor())&&a.traps().equals(b.traps())&&a.targets().equals(b.targets());}
 private boolean own(ItemSnapshot item){return item!=null&&item.count()>0&&item.count()<=plan.maximumConsumption()&&sameCount(plan.item(),item,item.count());}
 private static boolean sameCount(ItemSnapshot expected,ItemSnapshot item,int count){return item!=null&&item.count()==count&&expected.skyblockId().equals(item.skyblockId())&&expected.minecraftId().equals(item.minecraftId())&&expected.uuid().equals(item.uuid())&&expected.name().equals(item.name())&&expected.fingerprint().equals(item.fingerprint())&&expected.lore().equals(item.lore());}
 private static boolean raw(ItemSnapshot a,ItemSnapshot b){return a!=null&&b!=null&&sameCount(a,b,a.count());}
 private static boolean air(ItemSnapshot item){return item!=null&&item.count()==0&&item.minecraftId().equals("minecraft:air")&&item.skyblockId().isEmpty()&&item.uuid().isEmpty();}
 public void cancel(){if(state!=State.CONFIRMED){state=state==State.IDLE?State.REJECTED:State.UNCERTAIN;pending=null;revision++;}}
}
