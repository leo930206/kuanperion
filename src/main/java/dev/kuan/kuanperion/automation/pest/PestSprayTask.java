// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.regex.Pattern;
/** One in-place selected-material use. No commands, movement, look, flight, retries, buys or Farm rearm. */
public final class PestSprayTask {
 public enum State {IDLE,ACQUIRING,SETTLING,WAIT_DATA,STARTING_ACQUISITION,ACQUIRING_STOCK,WAIT_STOCK_TAB,READY_EQUIP,SENDING_EQUIP,READY_SELECTION,STARTING_SELECTION,SELECTING,WAIT_SELECTION_TAB,READY_USE,SENDING_USE,AWAITING,READY_RESTORE,SENDING_RESTORE,COMPLETE,CANCELLED,TIMED_OUT,FAILED,UNCERTAIN}
 public enum Acquisition {RUNNING,COMPLETE,FAILED,UNCERTAIN}
 public enum Kind {EQUIP,USE,RESTORE}public enum Effect {SENT,REJECTED,UNCERTAIN}
 public record Frame(long world,long observedAt,Object level,Object player,Map<Integer,ItemSnapshot> inventory,ItemSnapshot cursor,int selected,boolean idleInventory,boolean usingItem,double x,double y,double z,double vx,double vy,double vz,float yaw,float pitch,PestSprayObservation spray,long positionEpoch,long positionSequence,PestSprayTabReceipts.Proof tab,boolean groundResidual){
 public Frame(long world,long observedAt,Object level,Object player,Map<Integer,ItemSnapshot> inventory,ItemSnapshot cursor,int selected,boolean idleInventory,boolean usingItem,double x,double y,double z,double vx,double vy,double vz,float yaw,float pitch,PestSprayObservation spray,long positionEpoch,long positionSequence,PestSprayTabReceipts.Proof tab){this(world,observedAt,level,player,inventory,cursor,selected,idleInventory,usingItem,x,y,z,vx,vy,vz,yaw,pitch,spray,positionEpoch,positionSequence,tab,false);}
 public Frame{inventory=Map.copyOf(inventory);Objects.requireNonNull(cursor);Objects.requireNonNull(spray);}}
 public interface Port {AutomationContext context();boolean manualInput();Frame sprayFrame(long now);Effect sprayEffect(AutomationSession.Handle h,Action action,long now);void requestSprayData(AutomationSession.Handle h);Optional<Map<String,String>> sprayCatalog(AutomationSession.Handle h);void disposeSprayData(AutomationSession.Handle h);
 default boolean beginSprayAcquisition(AutomationSession.Handle h,PestSprayPolicy p,PestSprayBuying buying,SpendingBudget budget,long now){return false;}
 default Acquisition pollSprayAcquisition(AutomationSession.Handle h,long now){return Acquisition.FAILED;}
 default boolean ownsSprayAcquisitionScreen(){return false;}default void cancelSprayAcquisition(){}default void sprayAcquisitionMessage(long world,String text){}
 }
 /** Unforgeable one-use kind-scoped action, valid only while its owning effect is being sent. */
 public final class Action {
  private final Kind kind;private final Frame before;private final AutomationSession.Handle bound;private final int target;private boolean claimed;
  private Action(Kind k,Frame f){kind=k;before=f;bound=owner;target=k==Kind.RESTORE?original:plan.tool().slot();}public Kind kind(){return kind;}public Frame snapshot(){return before;}public AutomationSession.Handle owner(){return bound;}public int targetSlot(){return target;}public boolean matchesSnapshot(Frame current){return claimed&&pending==this&&state==sending(kind)&&Objects.equals(bound,owner)&&session.isCurrent(bound)&&coordinator.holdsIdentity(bound.lease())&&current!=null&&same(before,current);}
  public boolean claim(AutomationSession.Handle h,Frame current,long now){
   long expected=revision;State sending=sending(kind);
   if(claimed||pending!=this||state!=sending||!Objects.equals(h,owner)||!root(now)||revision!=expected||claimed||pending!=this||state!=sending||!scope(current,now)||!tab(current,policy,now)||!same(before,current)||!effectReady(kind,current))return false;
   if(revision!=expected||claimed||pending!=this)return false;if(kind==Kind.USE){useTab=current.tab();useClaimAt=now;}claimed=true;return true;
  }
 }
 private static final long S=1_000_000_000L;
 private static final Pattern SUCCESS=Pattern.compile("(?i)^sprayonator! you sprayed plot #?([1-9]|1[0-9]|2[0-4])(?:[ !.:,].*)?$");
 private final AutomationSession session;private final AutomationCoordinator coordinator;private final InventoryReceipts receipts;private final Port port;private final PestSpraySelectionTask selection;
 private State state=State.IDLE;private long revision,started,phaseAt,stableAt=-1;private AutomationSession.Handle owner;private PestSprayPlan plan;private Frame initial,anchor,last;private PestSprayPolicy policy;private int original,plot,reserved;private Action pending;private InventoryReceipts.Stamp stamp;private Map<Integer,ItemSnapshot> confirmed=Map.of();private boolean successAck,failureAck,conflicting;private PestSprayTabReceipts.Proof useTab;private long useClaimAt;private PestSprayBuying buying=PestSprayBuying.defaults();private SpendingBudget budget;private boolean buyNeeded;private long lifetime;private InventoryReceipts.Stamp buyStamp;
 public PestSprayTask(AutomationSession session,AutomationCoordinator coordinator,InventoryReceipts receipts,Port port){this(session,coordinator,receipts,null,port);}
 public PestSprayTask(AutomationSession session,AutomationCoordinator coordinator,InventoryReceipts receipts,CursorReceipts cursors,Port port){this.session=Objects.requireNonNull(session);this.coordinator=Objects.requireNonNull(coordinator);this.receipts=Objects.requireNonNull(receipts);this.port=Objects.requireNonNull(port);if(cursors!=null){if(!(port instanceof PestSpraySelectionTask.Port p))throw new IllegalArgumentException("Selection port required");selection=new PestSpraySelectionTask(session,coordinator,receipts,cursors,p);}else selection=null;}
 public boolean ownsMenu(){return state==State.ACQUIRING_STOCK?port.ownsSprayAcquisitionScreen():selection!=null&&selection.ownsMenu();}
 public State state(){return state;}public int reservedItems(){return reserved;}public int plot(){return plot;}public boolean active(){return switch(state){case ACQUIRING,SETTLING,WAIT_DATA,STARTING_ACQUISITION,ACQUIRING_STOCK,WAIT_STOCK_TAB,READY_EQUIP,SENDING_EQUIP,READY_SELECTION,STARTING_SELECTION,SELECTING,WAIT_SELECTION_TAB,READY_USE,SENDING_USE,AWAITING,READY_RESTORE,SENDING_RESTORE->true;default->false;};}
 public boolean request(PestSprayPolicy policy,long now){return request(policy,PestSprayBuying.defaults(),now);}
 public boolean request(PestSprayPolicy policy,PestSprayBuying buying,long now){
  if(active()||session.active()||policy==null||!policy.configured()||buying==null)return false;long expected=revision,assigned=-1;
  try{
   var c=port.context();var f=port.sprayFrame(now);boolean manual=port.manualInput();var candidate=f==null?Optional.<PestSprayPlan>empty():(selection==null?PestSprayPlan.select(policy,f.inventory()):PestSprayPlan.prepare(policy,f.inventory()));
   boolean missing=candidate.isEmpty();if(missing&&buying.configured()&&f!=null){candidate=PestSprayPlan.prepareForBuying(policy,f.inventory());if(selection==null&&candidate.isPresent()&&!candidate.get().tool().selectedMaterial().equals(policy.materialName()))candidate=Optional.empty();}
   if(revision!=expected||active()||session.active()||manual||c==null||!c.farmingActive()||!c.garden()||!c.canIssueCommand()||!base(f,now)||!tab(f,policy,now)||f.world()!=c.worldId()||f.spray().state()!=PestSprayObservation.State.NONE||f.usingItem()||candidate.isEmpty())return false;
   this.policy=policy;this.buying=buying;buyNeeded=missing;budget=new SpendingBudget(buying.maximumCoins());buyStamp=null;lifetime=missing?120*S:35*S;plan=candidate.get();initial=last=f;anchor=null;original=f.selected();plot=PestPlot.at(f.x(),f.z()).orElseThrow().id();owner=null;reserved=0;useTab=null;pending=null;confirmed=Map.of();successAck=failureAck=conflicting=false;stamp=null;started=phaseAt=now;stableAt=-1;state=State.ACQUIRING;assigned=++revision;
   var acquired=session.begin(AutomationFeature.PEST_SPRAY,now,lifetime);
   if(revision!=assigned||state!=State.ACQUIRING){session.finish(acquired,false,now);return false;}owner=acquired;
   if(acquired==null){initial=anchor=last=null;useTab=null;state=State.FAILED;revision++;return false;}
   boolean valid=root(now);if(revision!=assigned)return false;if(!valid||!session.isCurrent(acquired)){finish(State.CANCELLED,now);return false;}port.requestSprayData(acquired);if(revision!=assigned)return false;if(!session.isCurrent(acquired)){finish(State.CANCELLED,now);return false;}state=State.SETTLING;revision++;return true;
  }catch(RuntimeException unavailable){if(assigned>=0&&revision==assigned&&active())finish(State.FAILED,now);return false;}
 }
 public void tick(long now){if(!active()||state==State.ACQUIRING||state==State.STARTING_SELECTION||state==State.STARTING_ACQUISITION)return;long expected=revision;try{run(now,expected);}catch(RuntimeException unavailable){if(revision==expected&&active())finish(State.FAILED,now);}}
 private void run(long now,long expected){
  if(now-started<0||now-started>=lifetime){finish(State.TIMED_OUT,now);return;}
  if(state==State.ACQUIRING_STOCK){
   boolean valid=root(now);var before=port.sprayFrame(now);boolean owned=port.ownsSprayAcquisitionScreen();if(revision!=expected)return;
   if(!valid||!scope(before,now,owned)||before.selected()!=original||before.usingItem()){finish(State.CANCELLED,now);return;}
   var result=port.pollSprayAcquisition(owner,now);if(revision!=expected)return;if(result==Acquisition.RUNNING)return;
   if(result!=Acquisition.COMPLETE){finish(result==Acquisition.UNCERTAIN?State.UNCERTAIN:State.FAILED,now);return;}
   valid=root(now);var after=port.sprayFrame(now);if(revision!=expected)return;
   if(!valid||!scope(after,now)||!after.idleInventory()||after.selected()!=original||after.usingItem()||!plan.acquired(after.inventory(),buying,receipts,buyStamp)){finish(State.FAILED,now);return;}
   plan=(selection==null?PestSprayPlan.select(policy,after.inventory()):PestSprayPlan.prepare(policy,after.inventory())).orElseThrow();buyNeeded=false;phaseAt=now;state=State.WAIT_STOCK_TAB;revision++;return;
  }
  if(state==State.SELECTING){
   selection.tick(now);if(revision!=expected||selection.active())return;if(selection.state()!=PestSpraySelectionTask.State.COMPLETE){finish(selection.state()==PestSpraySelectionTask.State.UNCERTAIN?State.UNCERTAIN:selection.state()==PestSpraySelectionTask.State.TIMED_OUT?State.TIMED_OUT:State.FAILED,now);return;}
   boolean valid=root(now);var selected=port.sprayFrame(now);if(revision!=expected)return;var next=selected==null?Optional.<PestSprayPlan>empty():PestSprayPlan.select(policy,selected.inventory());
   if(!valid||!scope(selected,now)||!selection.selectedInventory().equals(selected.inventory())||next.isEmpty()||next.get().tool().slot()!=plan.tool().slot()){finish(State.FAILED,now);return;}plan=next.get();phaseAt=now;state=State.WAIT_SELECTION_TAB;revision++;return;
  }
  boolean valid=root(now);var f=port.sprayFrame(now);if(revision!=expected)return;
  if(!valid||!scope(f,now)){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=lifetime){finish(State.TIMED_OUT,now);return;}
  if(state==State.SENDING_EQUIP||state==State.SENDING_USE||state==State.SENDING_RESTORE)return;
  if(state==State.SETTLING){
   if(now-phaseAt<0||now-phaseAt>=3*S){finish(State.TIMED_OUT,now);return;}
   if(!plan.stillMatches(f.inventory())||f.selected()!=original||f.usingItem()||f.spray().state()!=PestSprayObservation.State.NONE||distance(initial,f)>4){finish(State.CANCELLED,now);return;}
   if(speed(f)>.0001||distance(last,f)>.0001){stableAt=-1;last=f;return;}last=f;if(stableAt<0)stableAt=now;if(now-stableAt<150_000_000L)return;
   anchor=f;state=State.WAIT_DATA;revision++;return;
  }
  if(state==State.WAIT_DATA){
   if(!plan.stillMatches(f.inventory())||f.selected()!=original||f.usingItem()||f.spray().state()!=PestSprayObservation.State.NONE){finish(State.CANCELLED,now);return;}
   if(now-started<0||now-started>=10*S){finish(State.TIMED_OUT,now);return;}var catalog=port.sprayCatalog(owner);if(revision!=expected)return;if(catalog.isEmpty())return;
   if(!policy.materialId().equals(catalog.get().get(policy.materialName()))){finish(State.FAILED,now);return;}if(buyNeeded){
    buyStamp=receipts.stamp(f.world());state=State.STARTING_ACQUISITION;long transition=++revision;
    boolean begun;try{begun=port.beginSprayAcquisition(owner,policy,buying,budget,now);}catch(RuntimeException unavailable){if(revision==transition)finish(State.FAILED,now);return;}if(revision!=transition)return;if(!begun){finish(State.FAILED,now);return;}state=State.ACQUIRING_STOCK;revision++;return;
   }
   state=original==plan.tool().slot()?selectedState():State.READY_EQUIP;revision++;return;
  }
  if(state==State.WAIT_STOCK_TAB){
   if(!plan.stillMatches(f.inventory())||f.selected()!=original||f.usingItem()||f.spray().state()==PestSprayObservation.State.ACTIVE){finish(State.FAILED,now);return;}
   if(now-phaseAt<0||now-phaseAt>=3*S){finish(State.TIMED_OUT,now);return;}if(tab(f,policy,now)&&f.spray().state()==PestSprayObservation.State.NONE){state=original==plan.tool().slot()?selectedState():State.READY_EQUIP;revision++;}return;
  }
  if(state==State.READY_EQUIP){send(Kind.EQUIP,f,now);return;}
  if(state==State.WAIT_SELECTION_TAB){if(!plan.stillMatches(f.inventory())||f.selected()!=plan.tool().slot()||f.spray().state()==PestSprayObservation.State.ACTIVE){finish(State.FAILED,now);return;}if(now-phaseAt<0||now-phaseAt>=3*S){finish(State.TIMED_OUT,now);return;}if(tab(f,policy,now)&&f.spray().state()==PestSprayObservation.State.NONE){state=State.READY_USE;revision++;}return;}
  if(state==State.READY_SELECTION){if(!tab(f,policy,now)||f.spray().state()!=PestSprayObservation.State.NONE){finish(State.FAILED,now);return;}state=State.STARTING_SELECTION;long transition=++revision;boolean accepted=selection!=null&&selection.begin(owner,policy,plan.tool(),f,now);if(revision!=transition)return;if(!accepted){finish(State.FAILED,now);return;}state=State.SELECTING;revision++;return;}
  if(state==State.READY_USE){send(Kind.USE,f,now);return;}
  if(state==State.AWAITING){
   if(!plan.possibleAfter(f.inventory())){finish(State.UNCERTAIN,now);return;}
   if(conflicting){finish(State.UNCERTAIN,now);return;}if(failureAck){finish(State.FAILED,now);return;}
   if(now-phaseAt<0||now-phaseAt>=8*S){finish(State.TIMED_OUT,now);return;}
   if(!successAck||!freshAfterUse(f,now)||f.spray().state()!=PestSprayObservation.State.ACTIVE||f.usingItem()||!plan.receipt(f.inventory(),receipts,stamp))return;
   confirmed=f.inventory();state=State.READY_RESTORE;revision++;return;
  }
  if(state==State.READY_RESTORE){if(!freshAfterUse(f,now)||!effectReady(Kind.RESTORE,f)){finish(State.FAILED,now);return;}if(original==plan.tool().slot()){finish(State.COMPLETE,now);return;}send(Kind.RESTORE,f,now);}
 }
 private void send(Kind kind,Frame frame,long now){
  if(!tab(frame,policy,now)||!effectReady(kind,frame)){finish(State.FAILED,now);return;}
  if(kind==Kind.USE){reserved=plan.reservedItems();stamp=receipts.stamp(frame.world());successAck=failureAck=conflicting=false;phaseAt=now;}
  pending=new Action(kind,frame);var action=pending;state=sending(kind);long effect=++revision;
  Effect result;try{result=port.sprayEffect(owner,action,now);}catch(RuntimeException ambiguous){result=Effect.UNCERTAIN;}
  if(revision!=effect)return;
  if(result!=Effect.SENT||!action.claimed){finish(result==Effect.UNCERTAIN?State.UNCERTAIN:State.FAILED,now);return;}
  pending=null;
  if(kind==Kind.USE){state=State.AWAITING;revision++;return;}
  boolean valid=root(now);var after=port.sprayFrame(now);if(revision!=effect)return;
  if(!valid||!scope(after,now)||!frame.inventory().equals(after.inventory())||after.selected()!=action.targetSlot()||after.usingItem()){finish(State.FAILED,now);return;}
  if(kind==Kind.RESTORE){finish(freshAfterUse(after,now)&&after.spray().state()==PestSprayObservation.State.ACTIVE?State.COMPLETE:State.FAILED,now);return;}state=selectedState();revision++;
 }
 /** Called only by the existing GAME/system-message channel. A raw text callback is never completion alone. */
 public void onSystemMessage(long world,String text,long now){
  if(state==State.ACQUIRING_STOCK){if(owner!=null&&world==owner.lease().worldId()&&session.isCurrent(owner)&&coordinator.holdsIdentity(owner.lease()))port.sprayAcquisitionMessage(world,text);return;}
  if(state==State.SELECTING&&selection!=null){selection.onSystemMessage(world,text,now);return;}
  if(owner==null||world!=owner.lease().worldId()||!session.isCurrent(owner)||!coordinator.holdsIdentity(owner.lease())||state!=State.AWAITING&&(state!=State.SENDING_USE||pending==null||!pending.claimed)||now-phaseAt<0||now-phaseAt>=8*S||text==null||text.length()>512||text.codePoints().anyMatch(Character::isISOControl))return;
  String line=text.replaceAll("(?i)§[0-9a-fk-orx]","").strip();var success=SUCCESS.matcher(line);String lower=line.toLowerCase(Locale.ROOT);
  if(success.matches()&&Integer.parseInt(success.group(1))==plot){if(failureAck)conflicting=true;successAck=true;}
  else if(lower.startsWith("you don't have enough ")||lower.startsWith("you cannot spray")||lower.equals("this plot was sprayed with that item recently! try again soon!")){if(successAck)conflicting=true;failureAck=true;}
 }
 private State selectedState(){return plan.tool().selectedMaterial().equals(policy.materialName())?State.READY_USE:State.READY_SELECTION;}
 private boolean effectReady(Kind kind,Frame f){
  if(f==null||f.usingItem()||anchor==null||distance(anchor,f)>.0001||speed(f)>.0001)return false;
  return switch(kind){case EQUIP->f.selected()==original&&plan.stillMatches(f.inventory())&&f.spray().state()==PestSprayObservation.State.NONE;case USE->plan.tool().selectedMaterial().equals(policy.materialName())&&f.selected()==plan.tool().slot()&&plan.stillMatches(f.inventory())&&f.spray().state()==PestSprayObservation.State.NONE;case RESTORE->f.selected()==plan.tool().slot()&&confirmed.equals(f.inventory())&&successAck&&!failureAck&&!conflicting&&f.spray().state()==PestSprayObservation.State.ACTIVE;};
 }
 private boolean tab(Frame f,PestSprayPolicy p,long now){return f!=null&&f.tab()!=null&&f.tab().fresh(new PestSprayTabReceipts.Scope(f.world(),f.level(),f.player(),PestPlot.at(f.x(),f.z()).orElseThrow().id(),f.positionEpoch(),f.positionSequence()),now)&&f.spray().equals(PestSprayObservation.parse(f.tab().entries(),p.materialName()));}
 private boolean freshAfterUse(Frame f,long now){return tab(f,policy,now)&&useTab!=null&&f.tab().epoch()==useTab.epoch()&&f.tab().sequence()>useTab.sequence()&&f.tab().observedAt()-useClaimAt>=0;}
 private boolean scope(Frame f,long now){return scope(f,now,false);}
 private boolean scope(Frame f,long now,boolean ownedMenu){return base(f,now,ownedMenu)&&(f.tab()==null||f.tab().epoch()==initial.tab().epoch())&&owner!=null&&f.world()==owner.lease().worldId()&&f.level()==initial.level()&&f.player()==initial.player()&&f.yaw()==initial.yaw()&&f.pitch()==initial.pitch()&&f.positionEpoch()==initial.positionEpoch()&&f.positionSequence()==initial.positionSequence()&&PestPlot.at(f.x(),f.z()).orElseThrow().id()==plot&&(!(state==State.AWAITING||state==State.READY_SELECTION||state==State.WAIT_SELECTION_TAB||state==State.READY_USE||state==State.READY_RESTORE)||f.selected()==plan.tool().slot())&&(anchor==null||distance(anchor,f)<=.0001&&speed(f)<=.0001);}
 private static boolean base(Frame f,long now){return base(f,now,false);}
 private static boolean base(Frame f,long now,boolean ownedMenu){return f!=null&&f.world()>0&&f.level()!=null&&f.player()!=null&&(f.idleInventory()||ownedMenu)&&PestSprayPlan.air(f.cursor())&&PestSprayPlan.complete(f.inventory())&&f.selected()>=0&&f.selected()<9&&now-f.observedAt()>=0&&now-f.observedAt()<=S&&Double.isFinite(f.x())&&Double.isFinite(f.y())&&Double.isFinite(f.z())&&Math.abs(f.y())<2048&&Double.isFinite(f.vx())&&Double.isFinite(f.vy())&&Double.isFinite(f.vz())&&Float.isFinite(f.yaw())&&Float.isFinite(f.pitch())&&PestPlot.at(f.x(),f.z()).filter(p->p.id()!=0).isPresent();}
 private boolean root(long now){long expected=revision;var h=owner;if(h==null||h.hold()==null||!session.holdsFarming()||h.lease().owner()!=AutomationFeature.PEST_SPRAY||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime())return false;
  try{var c=port.context();boolean manual=port.manualInput();return revision==expected&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&!manual&&c!=null&&c.worldId()==h.lease().worldId()&&c.garden()&&c.farmingActive()&&(state==State.ACQUIRING_STOCK?c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU):c.canIssueCommand());}catch(RuntimeException unavailable){return false;}}
 private static State sending(Kind k){return switch(k){case EQUIP->State.SENDING_EQUIP;case USE->State.SENDING_USE;case RESTORE->State.SENDING_RESTORE;};}
 private static double distance(Frame a,Frame b){return Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2);}static double speed(Frame f){return f.vx()*f.vx()+(f.groundResidual()&&f.vy()<0?0:f.vy()*f.vy())+f.vz()*f.vz();}
 private static boolean same(Frame a,Frame b){return a.world()==b.world()&&a.level()==b.level()&&a.player()==b.player()&&a.inventory().equals(b.inventory())&&PestSprayPlan.raw(a.cursor(),b.cursor())&&a.selected()==b.selected()&&a.idleInventory()==b.idleInventory()&&a.usingItem()==b.usingItem()&&distance(a,b)==0&&a.vx()==b.vx()&&a.vy()==b.vy()&&a.vz()==b.vz()&&a.groundResidual()==b.groundResidual()&&a.yaw()==b.yaw()&&a.pitch()==b.pitch()&&a.spray().equals(b.spray())&&a.positionEpoch()==b.positionEpoch()&&a.positionSequence()==b.positionSequence()&&Objects.equals(a.tab(),b.tab());}
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){var h=owner;state=terminal;pending=null;initial=anchor=last=null;useTab=null;long expected=++revision;boolean[] complete={false};try{AutomationStops.run(port::cancelSprayAcquisition,()->{if(selection!=null)selection.cancel();},()->complete[0]=session.finish(h,terminal==State.COMPLETE,now),()->port.disposeSprayData(h));}catch(RuntimeException failure){if(revision==expected){state=State.FAILED;revision++;}throw failure;}if(revision==expected&&terminal==State.COMPLETE&&!complete[0]){state=State.CANCELLED;revision++;}}
}
