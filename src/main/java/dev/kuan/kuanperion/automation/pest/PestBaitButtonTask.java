// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;

import dev.kuan.kuanperion.automation.*;
import java.util.*;

/** One source-style button refill under the parent's existing Pest root. Never acquires/releases Farming. */
public final class PestBaitButtonTask {
 public enum State {IDLE,OPENING,WAIT_MENU,TRANSFER,CLOSING,COMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public interface Port {
  AutomationContext context();boolean manualInput();PestTrapRelease.Facts facts(long now);
  PestBaitButtonTransfer.Frame baitFrame(long now);
  PestTrapRelease.Open open(AutomationSession.Handle owner,PestTrapTarget target,long now);
  GuardedMenuAction.Result baitClick(AutomationSession.Handle owner,PestBaitButtonTransfer.Action action,long now);
  boolean closeBaitMenu(Object identity,int id);
 }
 private static final long SECOND=1_000_000_000L;
 private final AutomationSession session;private final AutomationCoordinator coordinator;private final InventoryReceipts items;private final CursorReceipts cursors;private final Port port;
 private State state=State.IDLE;private long revision,started,phaseAt;private int container=-1,charged;
 private AutomationSession.Handle owner;private PestTrapTarget target;private PestBaitButtonPlan plan;private PestBaitButtonTransfer proof;
 private MenuContract contract;private Object level,identity;private InventoryReceipts.Stamp opening;private CursorReceipts.OpeningStamp openingCursor;
 public PestBaitButtonTask(AutomationSession s,AutomationCoordinator c,InventoryReceipts i,CursorReceipts cursors,Port p){session=Objects.requireNonNull(s);coordinator=Objects.requireNonNull(c);items=Objects.requireNonNull(i);this.cursors=Objects.requireNonNull(cursors);port=Objects.requireNonNull(p);}
 public State state(){return state;}public int charged(){return charged;}
 public boolean active(){return state==State.OPENING||state==State.WAIT_MENU||state==State.TRANSFER||state==State.CLOSING;}
 public boolean ownsMenu(){try{return active()&&session.isCurrent(owner)&&shape(port.baitFrame(System.nanoTime()));}catch(RuntimeException unavailable){return false;}}
 private static boolean empty(ItemSnapshot x){return x!=null&&x.count()==0&&x.minecraftId().equals("minecraft:air")&&x.skyblockId().isEmpty()&&x.uuid().isEmpty();}
 public boolean begin(AutomationSession.Handle h,PestTrapTarget t,PestBaitButtonPlan.Rules rules,int remaining,long now){
  if(active()||h==null||h.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(h)||t==null)return false;
  long before=revision;
  try{
   var c=port.context();var f=port.baitFrame(now);var facts=port.facts(now);var chosen=f==null?Optional.<PestBaitButtonPlan>empty():PestBaitButtonPlan.select(rules,f.inventory(),remaining);
   if(revision!=before||active()||!session.isCurrent(h)||c==null||!c.canIssueCommand()||!c.garden()||c.worldId()!=h.lease().worldId()||port.manualInput()||f==null||f.world()!=c.worldId()||f.level()==null||now-f.observedAt()<0||now-f.observedAt()>SECOND||f.menu()!=null||!empty(f.cursor())||!facts.fresh(c.worldId(),now)||!facts.traps().fullKnown()||!facts.traps().emptyKnown()||facts.traps().full().contains(t.trapId())||!facts.traps().empty().contains(t.trapId())||!PestTrapTarget.unique(facts.targets()).contains(t)||chosen.isEmpty()||revision!=before||!session.isCurrent(h))return false;
   owner=h;target=t;plan=chosen.get();level=f.level();identity=null;container=-1;charged=0;proof=null;started=phaseAt=now;
   contract=new MenuContract(Set.of(t.family(),t.label()),Set.of(9,18,27,36,45,54));state=State.OPENING;revision++;
   session.expectMenu(owner,contract,()->{var current=port.baitFrame(System.nanoTime());return current==null?null:current.menu();});return true;
  }catch(RuntimeException unavailable){return false;}
 }
 public void tick(long now){if(!active())return;long expected=revision;try{run(now,expected);}catch(RuntimeException unavailable){if(revision==expected)finish(State.FAILED);}}
 private boolean shape(PestBaitButtonTransfer.Frame f){return f!=null&&owner!=null&&f.world()==owner.lease().worldId()&&f.level()==level&&f.menuIdentity()!=null&&f.menu()!=null&&contract.matches(f.menu())&&f.menu().worldId()==f.world()&&(container<0||container==f.menu().containerId()&&identity==f.menuIdentity());}
 private void run(long now,long expected){
  var c=port.context();var f=port.baitFrame(now);var facts=port.facts(now);boolean manual=port.manualInput();
  if(revision!=expected)return;
  if(!session.isCurrent(owner)||c==null||!c.garden()||c.worldId()!=owner.lease().worldId()||owner.hold()!=null&&!c.farmingActive()||c.blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU)||manual||f==null||f.world()!=c.worldId()||f.level()!=level||now-f.observedAt()<0||now-f.observedAt()>SECOND||!facts.fresh(c.worldId(),now)){finish(State.CANCELLED);return;}
  if(now-started<0||now-started>=40*SECOND){finish(State.TIMED_OUT);return;}
  if(state==State.OPENING){
   if(f.menu()!=null||c.blockers().contains(AutomationContext.Blocker.MENU)||!empty(f.cursor())||!plan.stillMatches(f.inventory())||!facts.traps().emptyKnown()||!facts.traps().empty().contains(target.trapId())||!PestTrapTarget.unique(facts.targets()).contains(target)){finish(State.CANCELLED);return;}
   if(now-phaseAt>=5*SECOND){finish(State.TIMED_OUT);return;}
   var itemOpening=items.stamp(owner.lease().worldId());var cursorOpening=cursors.openingStamp(owner.lease().worldId(),now);
   var result=port.open(owner,target,now);if(revision!=expected||!session.isCurrent(owner))return;
   if(result==PestTrapRelease.Open.REJECTED){finish(State.FAILED);return;}
   if(result==PestTrapRelease.Open.SENT){opening=itemOpening;openingCursor=cursorOpening;state=State.WAIT_MENU;phaseAt=now;revision++;}return;
  }
  if(f.menu()!=null&&!shape(f)){finish(State.CANCELLED);return;}
  if(state==State.WAIT_MENU){
   if(now-phaseAt<0||now-phaseAt>=5*SECOND){finish(State.TIMED_OUT);return;}
   if(f.menu()==null){if(c.blockers().contains(AutomationContext.Blocker.MENU))finish(State.CANCELLED);return;}
   if(!empty(f.cursor())||!plan.stillMatches(f.inventory())){finish(State.CANCELLED);return;}
   var role=PestBaitButtonView.initial(f.menu(),target,facts.traps());if(role.isEmpty())return;
   var next=new PestBaitButtonTransfer(session,coordinator,items,cursors,port::context);
   boolean ready=next.begin(owner,plan,target,role.get(),f,opening,openingCursor,now);if(revision!=expected)return;
   if(!ready)return;proof=next;container=f.menu().containerId();identity=f.menuIdentity();state=State.TRANSFER;revision++;return;
  }
  if(f.menu()==null){finish(State.CANCELLED);return;}
  if(state==State.TRANSFER){
   proof.poll(f,now);if(revision!=expected)return;
   if(proof.state()==PestBaitButtonTransfer.State.CONFIRMED){state=State.CLOSING;revision++;return;}
   if(proof.state()==PestBaitButtonTransfer.State.REJECTED||proof.state()==PestBaitButtonTransfer.State.UNCERTAIN){finish(State.FAILED);return;}
   if(proof.state()!=PestBaitButtonTransfer.State.READY)return;
   var action=proof.arm(f,now);if(revision!=expected)return;if(action.isEmpty()){finish(State.FAILED);return;}
   if(action.get().step()==PestBaitButtonTransfer.Step.PICKUP)charged=plan.maximumConsumption();
   GuardedMenuAction.Result result;try{result=port.baitClick(owner,action.get(),now);}catch(RuntimeException ambiguous){result=GuardedMenuAction.Result.UNCERTAIN;}
   if(revision!=expected)return;if(!proof.sent(action.get(),result,now))finish(State.FAILED);return;
  }
  if(state==State.CLOSING){
   boolean settled=proof.confirmedStillMatches(f,now);if(revision!=expected)return;if(!settled){finish(State.FAILED);return;}
   var inventory=f.inventory();boolean didClose=port.closeBaitMenu(identity,container);if(revision!=expected)return;
   var after=port.context();var closed=port.baitFrame(now);boolean afterManual=port.manualInput();
   if(revision!=expected)return;
   if(!didClose||!session.isCurrent(owner)||after==null||!after.canIssueCommand()||!after.garden()||after.worldId()!=owner.lease().worldId()||afterManual||closed==null||closed.level()!=level||closed.world()!=owner.lease().worldId()||closed.menu()!=null||!empty(closed.cursor())||!inventory.equals(closed.inventory())){finish(State.FAILED);return;}
   finish(State.COMPLETE);
  }
 }
 public void cancel(){if(active())finish(State.CANCELLED);}
 private void finish(State result){state=result;revision++;if(proof!=null)proof.cancel();if(session.isCurrent(owner))session.clearExpectedMenu(owner);}
}
