// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Source-style GUI selection under the ORIGINAL held spray root. No root begin/finish, buys, use, movement or retry. */
public final class PestSpraySelectionTask {
 public enum State {IDLE,OPENING,SENDING_OPEN,WAIT_MENU,READY,SENDING_CLICK,AWAITING,CLOSING,SENDING_CLOSE,COMPLETE,CANCELLED,TIMED_OUT,FAILED,UNCERTAIN}
 public enum Kind {OPEN,CLICK,CLOSE}
 public record Frame(PestSprayTask.Frame motion,Object menuIdentity,MenuSnapshot menu,String invariant,boolean usable,boolean airTarget){public Frame{Objects.requireNonNull(motion);Objects.requireNonNull(invariant);}}
 public interface Port {AutomationContext context();boolean manualInput();Frame selectionFrame(long now);PestSprayTask.Effect selectionEffect(AutomationSession.Handle h,Action action,long now);}
 public final class Action {
  private final Kind kind;private final Frame before;private final AutomationSession.Handle bound;private final MenuContract.Ticket ticket;private boolean claimed;
  private Action(Kind k,Frame f){kind=k;before=f;bound=owner;ticket=k==Kind.CLICK?role.ticket():null;}
  public Kind kind(){return kind;}public Frame snapshot(){return before;}public AutomationSession.Handle owner(){return bound;}public MenuContract.Ticket ticket(){return ticket;}public boolean matchesSnapshot(Frame f){return claimed&&pending==this&&state==sending(kind)&&Objects.equals(bound,owner)&&session.isCurrent(bound)&&coordinator.holdsIdentity(bound.lease())&&same(before,f);}
  public boolean claim(AutomationSession.Handle h,Frame f,long now){long expected=revision;if(claimed||pending!=this||!Objects.equals(h,bound)||state!=sending(kind)||!root(now)||revision!=expected||claimed||pending!=this||!scope(f,now)||!same(before,f)||!ready(kind,f,now)||revision!=expected)return false;claimed=true;return true;}
 }
 private static final long S=1_000_000_000L;
 private static final java.util.regex.Pattern SELECTED=java.util.regex.Pattern.compile("^(?i:sprayonator! your selected material is now )(.+?)(?:!)?$");
 private final AutomationSession session;private final AutomationCoordinator coordinator;private final InventoryReceipts items;private final CursorReceipts cursors;private final Port port;
 private State state=State.IDLE;private long revision,started,phaseAt;private AutomationSession.Handle owner;private PestSprayPolicy policy;private PestSprayTool.Tool tool;private PestSprayTask.Frame anchor;private String invariant;private Map<Integer,ItemSnapshot> baseline=Map.of(),confirmed=Map.of();private Object identity;private int container=-1;private PestSpraySelectionView.Role role;private Action pending;private Frame clicked;private InventoryReceipts.Stamp opening,itemStamp;private CursorReceipts.OpeningStamp openingCursor;private CursorReceipts.Stamp cursorStamp;private boolean gameAck,conflict;
 public PestSpraySelectionTask(AutomationSession s,AutomationCoordinator c,InventoryReceipts i,CursorReceipts u,Port p){session=Objects.requireNonNull(s);coordinator=Objects.requireNonNull(c);items=Objects.requireNonNull(i);cursors=Objects.requireNonNull(u);port=Objects.requireNonNull(p);}
 public State state(){return state;}public Map<Integer,ItemSnapshot> selectedInventory(){return confirmed;}public boolean active(){return switch(state){case OPENING,SENDING_OPEN,WAIT_MENU,READY,SENDING_CLICK,AWAITING,CLOSING,SENDING_CLOSE->true;default->false;};}
 public boolean ownsMenu(){try{var f=port.selectionFrame(System.nanoTime());return active()&&session.isCurrent(owner)&&scope(f,f.motion().observedAt())&&(f.menu()!=null||state==State.OPENING||state==State.SENDING_OPEN||state==State.WAIT_MENU||state==State.SENDING_CLOSE&&pending!=null&&pending.claimed);}catch(RuntimeException unavailable){return false;}}
 public boolean begin(AutomationSession.Handle h,PestSprayPolicy policy,PestSprayTool.Tool tool,PestSprayTask.Frame original,long now){
  if(active()||h==null||h.hold()==null||policy==null||!policy.configured()||tool==null||tool.selectedMaterial().equals(policy.materialName())||original==null||!session.isCurrent(h)||h.lease().owner()!=AutomationFeature.PEST_SPRAY||!session.holdsFarming())return false;long expected=revision,assigned=-1;
  try{var c=port.context();var f=port.selectionFrame(now);boolean manual=port.manualInput();if(revision!=expected||active()||manual||c==null||!c.canIssueCommand()||c.worldId()!=h.lease().worldId()||original.world()!=h.lease().worldId()||!c.garden()||!c.farmingActive()||!coordinator.holdsIdentity(h.lease())||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime()||!closed(f)||!f.airTarget()||!pose(original,f.motion())||!original.inventory().equals(f.motion().inventory())||original.selected()!=tool.slot()||!tool.item().equals(original.inventory().get(tool.slot()))||f.invariant().isBlank()||f.invariant().length()>65536)return false;
   owner=h;this.policy=policy;this.tool=tool;anchor=original;invariant=f.invariant();baseline=original.inventory();confirmed=Map.of();identity=null;container=-1;role=null;pending=null;clicked=null;gameAck=conflict=false;started=phaseAt=now;state=State.OPENING;assigned=++revision;
   if(!scope(f,now)||!session.expectScreenSequence(h,this::ownsMenu,now,10*S)||revision!=assigned){if(revision==assigned)finish(State.FAILED);return false;}return true;
  }catch(RuntimeException unavailable){if(assigned>=0&&revision==assigned&&active())finish(State.FAILED);return false;}
 }
 public void tick(long now){if(!active())return;long expected=revision;try{run(now,expected);}catch(RuntimeException unavailable){if(revision==expected&&active())finish(State.FAILED);}}
 private void run(long now,long expected){
  boolean valid=root(now);var f=port.selectionFrame(now);if(revision!=expected)return;if(!valid||!scope(f,now)){finish(State.CANCELLED);return;}
  if(now-started<0||now-started>=10*S){finish(State.TIMED_OUT);return;}if(state==State.SENDING_OPEN||state==State.SENDING_CLICK||state==State.SENDING_CLOSE)return;
  if(state==State.OPENING){send(Kind.OPEN,f,now);return;}
  if(state==State.WAIT_MENU){if(now-phaseAt>=3*S){finish(State.TIMED_OUT);return;}if(f.menu()==null)return;if(identity==null){identity=f.menuIdentity();container=f.menu().containerId();}var candidate=PestSpraySelectionView.read(f.menu(),policy);if(candidate.isEmpty()||!openingProof(f,now))return;role=candidate.get();state=State.READY;revision++;return;}
  if(state==State.READY){send(Kind.CLICK,f,now);return;}
  if(state==State.AWAITING){
   var cursor=cursors.after(cursorStamp,now,8*S).orElse(null);if(!PestSprayPlan.air(f.motion().cursor())&&!PestSprayPlan.raw(f.motion().cursor(),role.marker())||cursor!=null&&!PestSprayPlan.air(cursor.item())){finish(State.UNCERTAIN);return;}
   if(conflict){finish(State.UNCERTAIN);return;}if(now-phaseAt>=8*S){finish(State.TIMED_OUT);return;}if(!proof(f,now))return;confirmed=f.motion().inventory();state=State.CLOSING;revision++;return;
  }
  if(state==State.CLOSING)send(Kind.CLOSE,f,now);
 }
 private void send(Kind k,Frame f,long now){
  if(!ready(k,f,now)){finish(State.FAILED);return;}
  if(k==Kind.OPEN){opening=items.stamp(f.motion().world());openingCursor=cursors.openingStamp(f.motion().world(),now);}
  if(k==Kind.CLICK){clicked=f;itemStamp=items.stamp(f.motion().world());cursorStamp=cursors.stamp(f.motion().world(),container,now);gameAck=conflict=false;}
  pending=new Action(k,f);var action=pending;phaseAt=now;state=sending(k);long effect=++revision;PestSprayTask.Effect result;
  try{result=port.selectionEffect(owner,action,now);}catch(RuntimeException ambiguous){result=PestSprayTask.Effect.UNCERTAIN;}if(revision!=effect)return;
  if(result!=PestSprayTask.Effect.SENT||!action.claimed){finish(result==PestSprayTask.Effect.UNCERTAIN?State.UNCERTAIN:State.FAILED);return;}
  pending=null;
  if(k==Kind.CLOSE){boolean valid=root(now);var after=port.selectionFrame(now);if(revision!=effect)return;if(!valid||!closed(after)||!scope(after,now)||!confirmed.equals(after.motion().inventory())){finish(State.FAILED);return;}finish(State.COMPLETE);return;}
  state=k==Kind.OPEN?State.WAIT_MENU:State.AWAITING;revision++;
 }
 /** Existing GAME/system channel only, after this private selection click was claimed. */
 public void onSystemMessage(long world,String text,long now){
  if(owner==null||world!=owner.lease().worldId()||!session.isCurrent(owner)||!coordinator.holdsIdentity(owner.lease())||state!=State.AWAITING&&(state!=State.SENDING_CLICK||pending==null||!pending.claimed)||now-phaseAt<0||now-phaseAt>=8*S||text==null||text.length()>512||text.codePoints().anyMatch(Character::isISOControl))return;
  String line=text.replaceAll("(?i)§[0-9a-fk-orx]","").strip();var selected=SELECTED.matcher(line);
  if(selected.matches()){if(selected.group(1).equals(policy.materialName()))gameAck=true;else conflict=true;}else if(line.toLowerCase(Locale.ROOT).startsWith("you cannot ")||line.toLowerCase(Locale.ROOT).startsWith("you don't have enough "))conflict=true;
 }
 private boolean ready(Kind k,Frame f,long now){return switch(k){case OPEN->closed(f)&&f.airTarget()&&baseline.equals(f.motion().inventory());case CLICK->{var r=PestSpraySelectionView.read(f.menu(),policy);yield r.isPresent()&&r.get().slot()==role.slot()&&role.menu().equals(f.menu())&&openingProof(f,now);}case CLOSE->proof(f,now)&&confirmed.equals(f.motion().inventory());};}
 private boolean openingProof(Frame f,long now){if(f.menu()==null||!baseline.equals(f.motion().inventory())||!PestSprayPlan.air(f.motion().cursor())||!cursors.emptyAfterOpening(openingCursor,f.menu().containerId(),now,8*S))return false;for(var s:f.menu().slots())if(!PestSprayPlan.raw(s.item(),items.after(opening,f.menu().containerId(),s.index())))return false;return true;}
 private boolean proof(Frame f,long now){
  if(!gameAck||conflict||f.menu()==null||!PestSprayPlan.air(f.motion().cursor())||!cursors.emptyAfter(cursorStamp,now,8*S))return false;
  var selected=PestSprayTool.read(tool.slot(),f.motion().inventory().get(tool.slot()));if(selected.isEmpty()||!selected.get().selectedMaterial().equals(policy.materialName())||!PestSprayPlan.raw(selected.get().item(),items.after(itemStamp,0,36+tool.slot()))||!PestSprayPlan.raw(selected.get().item(),items.after(itemStamp,container,f.menu().containerSlots()+27+tool.slot())))return false;
  for(var slot:f.menu().slots())if(!slot.equals(clicked.menu().slot(slot.index()))&&!PestSprayPlan.raw(slot.item(),items.after(itemStamp,container,slot.index())))return false;return true;
 }
 private boolean scope(Frame f,long now){
  if(f==null||!f.usable()||f.motion().world()!=owner.lease().worldId()||f.motion().level()==null||f.motion().player()==null||anchor==null||!pose(anchor,f.motion())||now-f.motion().observedAt()<0||now-f.motion().observedAt()>S||!invariant.equals(f.invariant())||!PestSprayPlan.complete(f.motion().inventory())||f.motion().selected()!=tool.slot()||f.motion().usingItem())return false;
  var current=PestSprayTool.read(tool.slot(),f.motion().inventory().get(tool.slot()));var old=tool.item();if(current.isEmpty())return false;var x=current.get().item();if(!old.skyblockId().equals(x.skyblockId())||!old.minecraftId().equals(x.minecraftId())||!old.uuid().equals(x.uuid())||!old.name().equals(x.name())||old.count()!=x.count()||old.protectedItem()!=x.protectedItem())return false;
  for(int i=0;i<43;i++)if(i!=tool.slot()&&!baseline.get(i).equals(f.motion().inventory().get(i)))return false;
  if(f.menu()==null)return (identity==null||state==State.SENDING_CLOSE||state==State.COMPLETE)&&closed(f);
  if(f.menuIdentity()==null||!PestSpraySelectionView.complete(f.menu())||f.menu().worldId()!=anchor.world()||identity!=null&&(identity!=f.menuIdentity()||container!=f.menu().containerId()||!roleTitle(f.menu())))return false;
  for(int i=0;i<36;i++)if(!f.menu().item(f.menu().containerSlots()+i).equals(f.motion().inventory().get(i<27?i+9:i-27)))return false;return true;
 }
 private boolean roleTitle(MenuSnapshot m){return role==null||role.menu().title().equals(m.title());}
 private boolean root(long now){long expected=revision;var h=owner;if(h==null||h.hold()==null||!session.holdsFarming()||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime())return false;try{var c=port.context();boolean manual=port.manualInput();return revision==expected&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&!manual&&c!=null&&c.worldId()==h.lease().worldId()&&c.garden()&&c.farmingActive()&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU);}catch(RuntimeException unavailable){return false;}}
 private static boolean closed(Frame f){return f!=null&&f.usable()&&f.menu()==null&&f.menuIdentity()==null&&f.motion().idleInventory()&&PestSprayPlan.air(f.motion().cursor());}
 private static boolean pose(PestSprayTask.Frame a,PestSprayTask.Frame b){return a!=null&&b!=null&&a.world()==b.world()&&a.level()==b.level()&&a.player()==b.player()&&a.x()==b.x()&&a.y()==b.y()&&a.z()==b.z()&&a.yaw()==b.yaw()&&a.pitch()==b.pitch()&&Double.isFinite(b.x())&&Double.isFinite(b.y())&&Math.abs(b.y())<2048&&Double.isFinite(b.z())&&Float.isFinite(b.yaw())&&Float.isFinite(b.pitch())&&Double.isFinite(b.vx())&&Double.isFinite(b.vy())&&Double.isFinite(b.vz())&&PestSprayTask.speed(b)<=.0001&&a.positionEpoch()==b.positionEpoch()&&a.positionSequence()==b.positionSequence();}
 private static boolean same(Frame a,Frame b){return a!=null&&b!=null&&a.menuIdentity()==b.menuIdentity()&&Objects.equals(a.menu(),b.menu())&&a.invariant().equals(b.invariant())&&a.usable()==b.usable()&&a.airTarget()==b.airTarget()&&a.motion().equals(b.motion());}
 private static State sending(Kind k){return switch(k){case OPEN->State.SENDING_OPEN;case CLICK->State.SENDING_CLICK;case CLOSE->State.SENDING_CLOSE;};}
 public void cancel(){if(active())finish(State.CANCELLED);}
 private void finish(State terminal){var h=owner;state=terminal;pending=null;anchor=null;clicked=null;identity=null;container=-1;revision++;if(session.isCurrent(h))session.clearExpectedMenu(h);}
}
