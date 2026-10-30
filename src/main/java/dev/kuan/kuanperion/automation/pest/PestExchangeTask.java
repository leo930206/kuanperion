// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** One source-style exchange after cleanup, under the original Pest root. No root acquisition, retry or Farm restart. */
public final class PestExchangeTask {
 public enum State {IDLE,OPENING,SENDING_OPEN,WAIT_MENU,READY,SENDING_CLICK,AWAITING,CLOSING,SENDING_CLOSE,COMPLETE,SKIPPED,CANCELLED,TIMED_OUT,FAILED,UNCERTAIN}
 public enum Kind {CLICK,CLOSE}
 public record Frame(long world,long observedAt,Object level,Object menuIdentity,MenuSnapshot menu,Map<Integer,ItemSnapshot> inventory,ItemSnapshot cursor,int selected){public Frame{inventory=Map.copyOf(inventory);Objects.requireNonNull(cursor);}}
 public interface Port {AutomationContext context();boolean manualInput();Frame exchangeFrame(long now);boolean openExchange(AutomationSession.Handle h,long now);GuardedMenuAction.Result exchangeClick(AutomationSession.Handle h,Action action,long now);boolean closeExchange(AutomationSession.Handle h,Action action,long now);}
 /** Private one-use input/close capability; native edges must independently sample and claim it before the effect. */
 public final class Action {
  private final Kind kind;private final Frame frame;private final MenuContract.Ticket ticket;private boolean claimed;
  private Action(Kind k,Frame f){kind=k;frame=f;ticket=k==Kind.CLICK?role.contract().plan(f.menu(),role.slot(),MenuContract.Click.PICKUP):null;}
  public Kind kind(){return kind;}public Frame snapshot(){return frame;}public MenuContract.Ticket ticket(){return ticket;}public AutomationSession.Handle owner(){return owner;}
  public boolean claim(AutomationSession.Handle h,Frame current,long now){
   long expected=revision;if(claimed||pending!=this||!Objects.equals(h,owner)||state!=(kind==Kind.CLICK?State.SENDING_CLICK:State.SENDING_CLOSE)||!root(now)||revision!=expected||claimed||pending!=this||!scope(current,now)||!same(frame,current))return false;
   boolean valid=kind==Kind.CLICK?openingReady(current,now):settled(current,now);
   if(!valid||revision!=expected||claimed||pending!=this)return false;claimed=true;return true;
  }
 }
 private static final long S=1_000_000_000L;
 private final AutomationSession session;private final AutomationCoordinator coordinator;private final InventoryReceipts items;private final CursorReceipts cursors;private final Port port;
 private State state=State.IDLE;private long revision,started,phaseAt;private AutomationSession.Handle owner;private PestIdentity.Vacuum tool;private Object level,identity;private int container=-1;private Map<Integer,ItemSnapshot> baseline=Map.of();private PestExchangeView.Role role;private Action pending;private boolean exchanged;
 private final MenuContract openingContract=new MenuContract(PestExchangeView.TITLES,Set.of(9,18,27,36,45,54));
 private InventoryReceipts.Stamp opening,itemStamp;private CursorReceipts.OpeningStamp openingCursor;private CursorReceipts.Stamp cursorStamp;
 public PestExchangeTask(AutomationSession s,AutomationCoordinator c,InventoryReceipts i,CursorReceipts u,Port p){session=Objects.requireNonNull(s);coordinator=Objects.requireNonNull(c);items=Objects.requireNonNull(i);cursors=Objects.requireNonNull(u);port=Objects.requireNonNull(p);}
 public State state(){return state;}public boolean exchanged(){return exchanged;}public boolean active(){return switch(state){case OPENING,SENDING_OPEN,WAIT_MENU,READY,SENDING_CLICK,AWAITING,CLOSING,SENDING_CLOSE->true;default->false;};}
 public boolean ownsMenu(){try{var f=port.exchangeFrame(System.nanoTime());return active()&&session.isCurrent(owner)&&f!=null&&f.level()==level&&f.world()==owner.lease().worldId()&&shape(f)&&(identity==null||identity==f.menuIdentity()&&container==f.menu().containerId());}catch(RuntimeException unavailable){return false;}}
 public boolean begin(AutomationSession.Handle h,boolean authorized,PestIdentity.Vacuum tool,long now){
  if(active()||!authorized||h==null||tool==null||h.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime())return false;long expected=revision,assigned=-1;
  try{var c=port.context();var f=port.exchangeFrame(now);boolean manual=port.manualInput();if(revision!=expected||active()||!session.isCurrent(h)||c==null||!c.canIssueCommand()||!c.garden()||c.worldId()!=h.lease().worldId()||h.hold()!=null&&!c.farmingActive()||manual||f==null||f.world()!=c.worldId()||f.level()==null||f.menu()!=null||!air(f.cursor())||!complete(f.inventory())||f.selected()!=tool.slot()||!PestIdentity.sameVacuum(tool,f.inventory().get(tool.slot()))||now-f.observedAt()<0||now-f.observedAt()>S)return false;
   owner=h;this.tool=tool;level=f.level();identity=null;container=-1;baseline=f.inventory();role=null;pending=null;exchanged=false;opening=itemStamp=null;openingCursor=null;cursorStamp=null;started=phaseAt=now;state=State.OPENING;assigned=++revision;
   session.expectMenu(h,openingContract,()->{var current=port.exchangeFrame(System.nanoTime());return current==null?null:current.menu();});return true;
  }catch(RuntimeException unavailable){if(assigned>=0&&revision==assigned&&active())finish(State.FAILED);return false;}
 }
 public void tick(long now){if(!active())return;long expected=revision;try{run(now,expected);}catch(RuntimeException unavailable){if(revision==expected)finish(State.FAILED);}}
 private void run(long now,long expected){
  boolean valid=root(now);var f=port.exchangeFrame(now);if(revision!=expected)return;
  if(!valid||f==null||f.world()!=owner.lease().worldId()||f.level()!=level||now-f.observedAt()<0||now-f.observedAt()>S||!inventoryShape(f)||f.menu()!=null&&!shape(f)||identity!=null&&(f.menu()==null||f.menuIdentity()!=identity||f.menu().containerId()!=container)){finish(State.CANCELLED);return;}
  if(now-started<0||now-started>=20*S){finish(State.TIMED_OUT);return;}
  if(state==State.SENDING_OPEN||state==State.SENDING_CLICK||state==State.SENDING_CLOSE)return; // Reentrant callbacks may validate/cancel but never replay an in-flight effect.
  if(state!=State.AWAITING&&!air(f.cursor())){finish(State.CANCELLED);return;}
  if(state==State.OPENING){
   var c=port.context();if(revision!=expected)return;if(c==null||!c.canIssueCommand()||f.menu()!=null){finish(State.CANCELLED);return;}
   baseline=f.inventory();opening=items.stamp(f.world());openingCursor=cursors.openingStamp(f.world(),now);phaseAt=now;state=State.SENDING_OPEN;long effect=++revision;
   boolean sent;try{sent=port.openExchange(owner,now);}catch(RuntimeException ambiguous){sent=false;}if(revision!=effect)return;if(!sent){finish(State.FAILED);return;}state=State.WAIT_MENU;revision++;return;
  }
  if(state==State.WAIT_MENU){
   if(now-phaseAt<0||now-phaseAt>=5*S){finish(State.TIMED_OUT);return;}if(f.menu()==null)return;
   if(identity==null){identity=f.menuIdentity();container=f.menu().containerId();}
   var candidate=PestExchangeView.read(f.menu());if(candidate.isEmpty()||!inventoryProof(f,opening)||!openingReceipts(f,now))return;
   role=candidate.get();state=role.status()==PestExchangeView.Status.ALREADY_EXCHANGED?State.CLOSING:State.READY;revision++;return;
  }
  if(!scope(f,now)){finish(State.CANCELLED);return;}
  if(state==State.READY){
   if(now-phaseAt<0||now-phaseAt>=8*S||!openingReady(f,now)){finish(State.FAILED);return;}
   pending=new Action(Kind.CLICK,f);var action=pending;itemStamp=items.stamp(f.world());cursorStamp=cursors.stamp(f.world(),container,now);phaseAt=now;state=State.SENDING_CLICK;long effect=++revision;
   GuardedMenuAction.Result result;try{result=port.exchangeClick(owner,action,now);}catch(RuntimeException ambiguous){result=GuardedMenuAction.Result.UNCERTAIN;}
   if(revision!=effect)return;if(result!=GuardedMenuAction.Result.SENT||!action.claimed){finish(result==GuardedMenuAction.Result.UNCERTAIN?State.UNCERTAIN:State.FAILED);return;}pending=null;state=State.AWAITING;revision++;return;
  }
  if(state==State.AWAITING){
   if(now-phaseAt<0||now-phaseAt>=8*S){finish(State.TIMED_OUT);return;}
   var cursor=cursors.after(cursorStamp,now,8*S).orElse(null);
   if(!air(f.cursor())&&!raw(f.cursor(),role.marker())||cursor!=null&&!air(cursor.item())){finish(State.UNCERTAIN);return;}
   var candidate=PestExchangeView.read(f.menu());
   if(candidate.isEmpty()||candidate.get().slot()!=role.slot()||candidate.get().status()!=PestExchangeView.Status.ALREADY_EXCHANGED||!raw(candidate.get().marker(),items.after(itemStamp,container,role.slot()))||!air(f.cursor())||!cursors.emptyAfter(cursorStamp,now,8*S)||!inventoryProof(f,itemStamp))return;
   role=candidate.get();exchanged=true;state=State.CLOSING;revision++;return;
  }
  if(state==State.CLOSING){
   if(!settled(f,now)){finish(State.FAILED);return;}pending=new Action(Kind.CLOSE,f);var action=pending;state=State.SENDING_CLOSE;long effect=++revision;
   boolean closed;try{closed=port.closeExchange(owner,action,now);}catch(RuntimeException unavailable){closed=false;}if(revision!=effect)return;
   var c=port.context();var after=port.exchangeFrame(now);boolean manual=port.manualInput();if(revision!=effect)return;
   if(!closed||!action.claimed||!session.isCurrent(owner)||c==null||!c.canIssueCommand()||!c.garden()||c.worldId()!=owner.lease().worldId()||owner.hold()!=null&&!c.farmingActive()||manual||after==null||after.level()!=level||after.world()!=f.world()||now-after.observedAt()<0||now-after.observedAt()>S||after.selected()!=tool.slot()||after.menu()!=null||!air(after.cursor())||!f.inventory().equals(after.inventory())){finish(State.FAILED);return;}
   finish(exchanged?State.COMPLETE:State.SKIPPED);
  }
 }
 private boolean openingReady(Frame f,long now){var r=PestExchangeView.read(f.menu());return r.isPresent()&&r.get().slot()==role.slot()&&r.get().status()==PestExchangeView.Status.READY&&raw(r.get().marker(),role.marker())&&openingReceipts(f,now)&&inventoryProof(f,opening);}
 private boolean openingReceipts(Frame f,long now){if(!cursors.emptyAfterOpening(openingCursor,f.menu().containerId(),now,8*S))return false;for(var s:f.menu().slots())if(!raw(s.item(),items.after(opening,f.menu().containerId(),s.index())))return false;return true;}
 private boolean settled(Frame f,long now){
  var r=PestExchangeView.read(f.menu());if(r.isEmpty()||r.get().slot()!=role.slot()||r.get().status()!=PestExchangeView.Status.ALREADY_EXCHANGED||!raw(r.get().marker(),role.marker())||!air(f.cursor()))return false;
  var stamp=exchanged?itemStamp:opening;return raw(role.marker(),items.after(stamp,container,role.slot()))&&inventoryProof(f,stamp)&&(exchanged?cursors.emptyAfter(cursorStamp,now,8*S):cursors.emptyAfterOpening(openingCursor,container,now,8*S));
 }
 private boolean inventoryProof(Frame f,InventoryReceipts.Stamp stamp){var current=f.inventory().get(tool.slot());return baseline.get(tool.slot()).equals(current)||raw(current,items.after(stamp,0,tool.slot()))&&raw(current,items.after(stamp,f.menu().containerId(),f.menu().containerSlots()+27+tool.slot()));}
 private boolean inventoryShape(Frame f){if(!complete(f.inventory())||f.selected()!=tool.slot()||!PestIdentity.sameVacuum(tool,f.inventory().get(tool.slot())))return false;for(int i=0;i<43;i++)if(i!=tool.slot()&&!baseline.get(i).equals(f.inventory().get(i)))return false;return true;}
 private boolean scope(Frame f,long now){return f!=null&&f.world()==owner.lease().worldId()&&f.level()==level&&f.menuIdentity()==identity&&identity!=null&&f.menu()!=null&&f.menu().containerId()==container&&now-f.observedAt()>=0&&now-f.observedAt()<=S&&shape(f)&&(role==null||f.menu().title().equals(role.menu().title()))&&inventoryShape(f);}
 private boolean shape(Frame f){var m=f.menu();if(f.menuIdentity()==null||m==null||m.worldId()!=f.world()||!openingContract.matches(m)||m.slots().size()!=m.containerSlots()+36)return false;for(int i=0;i<m.slots().size();i++){var s=m.slot(i);if(s==null||s.playerInventory()!=(i>=m.containerSlots()))return false;}for(int i=0;i<36;i++)if(!m.item(m.containerSlots()+i).equals(f.inventory().get(i<27?i+9:i-27)))return false;return true;}
 private boolean root(long now){
  long expected=revision;var h=owner;if(h==null||h.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime())return false;
  try{var c=port.context();boolean manual=port.manualInput();return revision==expected&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&!manual&&c!=null&&c.worldId()==h.lease().worldId()&&c.garden()&&(h.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU);}catch(RuntimeException unavailable){return false;}
 }
 private static boolean complete(Map<Integer,ItemSnapshot> inv){if(inv.size()!=43)return false;for(int i=0;i<43;i++)if(inv.get(i)==null||inv.get(i).count()>64)return false;return true;}
 private static boolean air(ItemSnapshot x){return x!=null&&x.count()==0&&x.minecraftId().equals("minecraft:air")&&x.skyblockId().isEmpty()&&x.uuid().isEmpty();}
 private static boolean raw(ItemSnapshot a,ItemSnapshot b){return a!=null&&b!=null&&a.skyblockId().equals(b.skyblockId())&&a.minecraftId().equals(b.minecraftId())&&a.uuid().equals(b.uuid())&&a.name().equals(b.name())&&a.fingerprint().equals(b.fingerprint())&&a.count()==b.count()&&a.lore().equals(b.lore());}
 private static boolean same(Frame a,Frame b){return a.world()==b.world()&&a.level()==b.level()&&a.menuIdentity()==b.menuIdentity()&&a.menu().equals(b.menu())&&a.inventory().equals(b.inventory())&&raw(a.cursor(),b.cursor())&&a.selected()==b.selected();}
 public void cancel(){if(active())finish(State.CANCELLED);}
 private void finish(State terminal){state=terminal;pending=null;revision++;if(session.isCurrent(owner))session.clearExpectedMenu(owner);}
}
