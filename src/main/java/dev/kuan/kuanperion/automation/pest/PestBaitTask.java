// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** One declared direct-item refill under the existing Pest root. No runtime/native caller yet.
 * Port.layout MUST independently prove actual role, current capacity and free transfer BEFORE the first pickup.
 * This subtask never releases/re-arms the original Farming hold, repairs a user's cursor, buys or retries. */
public final class PestBaitTask {
 public enum State{IDLE,OPENING,WAIT_MENU,TRANSFER,CLOSING,COMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public interface Port {
  AutomationContext context();boolean manualInput();PestTrapRelease.Facts facts(long now);Map<Integer,ItemSnapshot> inventory();ItemSnapshot cursor();MenuSnapshot menu();
  Optional<PestBaitTransfer.Layout> layout(PestTrapTarget target,MenuSnapshot menu);
  PestTrapRelease.Open open(AutomationSession.Handle handle,PestTrapTarget target,long now);
  /** Native caller must independently revalidate current root, layout/material/capacity, full43 and exact owned cursor before a guarded single PICKUP. */
  GuardedMenuAction.Result transfer(AutomationSession.Handle handle,PestBaitTransfer.Plan plan,PestBaitTransfer.Step step,MenuContract.Ticket ticket,long now);
  void closeOwnedMenu(int id);
 }
 private static final long SECOND=1_000_000_000L;private final AutomationSession session;private final InventoryReceipts items;private final CursorReceipts cursors;private final Port port;
 private State state=State.IDLE;private AutomationSession.Handle handle;private PestTrapTarget target;private PestBaitTransfer.Rules rules;private Map<Integer,ItemSnapshot> baseline;private MenuContract contract;private int container=-1;private long started,phaseAt;private PestBaitTransfer proof;private PestBaitTransfer.Plan plan;
 public PestBaitTask(AutomationSession session,InventoryReceipts items,CursorReceipts cursors,Port port){this.session=Objects.requireNonNull(session);this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.port=Objects.requireNonNull(port);}
 public State state(){return state;}public boolean active(){return state==State.OPENING||state==State.WAIT_MENU||state==State.TRANSFER||state==State.CLOSING;}
 private static boolean empty(ItemSnapshot i){return i!=null&&i.count()==0&&i.minecraftId().equals("minecraft:air")&&i.skyblockId().isEmpty()&&i.uuid().isEmpty();}
 private boolean shape(MenuSnapshot menu){return menu!=null&&contract!=null&&contract.matches(menu)&&handle!=null&&menu.worldId()==handle.lease().worldId()&&(container<0||menu.containerId()==container);}
 public boolean ownsMenu(){try{return active()&&session.isCurrent(handle)&&shape(port.menu());}catch(RuntimeException unavailable){return false;}}
 public boolean begin(AutomationSession.Handle handle,PestTrapTarget target,PestBaitTransfer.Rules rules,long now){
  if(active()||target==null||handle==null||handle.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(handle))return false;
  try{var c=port.context();var facts=port.facts(now);var inv=port.inventory();if(!c.canIssueCommand()||!c.garden()||c.worldId()!=handle.lease().worldId()||port.manualInput()||port.menu()!=null||!empty(port.cursor())||!facts.fresh(c.worldId(),now)||!facts.traps().emptyKnown()||!facts.traps().empty().contains(target.trapId())||!PestTrapTarget.unique(facts.targets()).contains(target)||!PestBaitTransfer.eligible(rules,inv))return false;
   this.handle=handle;this.target=target;this.rules=rules;baseline=Map.copyOf(inv);contract=new MenuContract(Set.of(target.family(),target.label()),Set.of(9,18,27,36,45,54));container=-1;plan=null;proof=null;started=phaseAt=now;session.expectMenu(handle,contract,port::menu);state=State.OPENING;return true;
  }catch(RuntimeException invalid){return false;}
 }
 private PestBaitTransfer.TrapFrame trapFrame(PestTrapRelease.Facts f){return new PestBaitTransfer.TrapFrame(f.world(),f.observedAt(),f.traps());}
 public void tick(long now){if(active())try{run(now);}catch(RuntimeException invalid){finish(State.FAILED);}}
 private void run(long now){
  var c=port.context();var menu=port.menu();var facts=port.facts(now);
  if(!session.isCurrent(handle)||c.worldId()!=handle.lease().worldId()||!c.garden()||c.blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU)||port.manualInput()||!facts.fresh(c.worldId(),now)){finish(State.CANCELLED);return;}
  if(now-started<0||now-started>=40*SECOND){finish(State.TIMED_OUT);return;}
  if(state==State.OPENING){if(menu!=null||!empty(port.cursor())||!baseline.equals(port.inventory())||!facts.traps().emptyKnown()||!facts.traps().empty().contains(target.trapId())||!PestTrapTarget.unique(facts.targets()).contains(target)){finish(State.CANCELLED);return;}
   var result=port.open(handle,target,now);if(result==PestTrapRelease.Open.REJECTED){finish(State.FAILED);return;}if(result==PestTrapRelease.Open.SENT){state=State.WAIT_MENU;phaseAt=now;}else if(now-phaseAt<0||now-phaseAt>=5*SECOND)finish(State.TIMED_OUT);return;
  }
  if(menu!=null&&!shape(menu)){finish(State.CANCELLED);return;}
  if(state==State.WAIT_MENU){
   if(now-phaseAt<0||now-phaseAt>=5*SECOND){finish(State.TIMED_OUT);return;}if(menu==null){if(c.blockers().contains(AutomationContext.Blocker.MENU))finish(State.CANCELLED);return;}
   container=menu.containerId();if(!empty(port.cursor())||!baseline.equals(port.inventory())){finish(State.CANCELLED);return;}var layout=port.layout(target,menu);if(layout.isEmpty())return;
   plan=PestBaitTransfer.select(rules,target.trapId(),layout.get(),menu,port.inventory(),facts.traps()).orElse(null);if(plan==null){finish(State.FAILED);return;}
   proof=new PestBaitTransfer(items,cursors,()->session.isCurrent(this.handle));if(!proof.begin(plan,menu,port.inventory(),port.cursor(),now)){finish(State.FAILED);return;}state=State.TRANSFER;return;
  }
  if(menu==null){finish(State.CANCELLED);return;}
  if(state==State.TRANSFER){
   proof.poll(c.worldId(),menu,port.inventory(),port.cursor(),trapFrame(facts),false,now);
   if(proof.state()==PestBaitTransfer.State.CONFIRMED){state=State.CLOSING;return;}
   if(proof.state()==PestBaitTransfer.State.UNCERTAIN||proof.state()==PestBaitTransfer.State.REJECTED){finish(State.FAILED);return;}
   if(proof.state()==PestBaitTransfer.State.READY){
    // A declared quantity does not cap a native left-click. Capacity/empty-trap authority must still hold BEFORE both pickup and deposit.
    if(proof.step()!=PestBaitTransfer.Step.RETURN&&(!facts.traps().emptyKnown()||!facts.traps().empty().contains(target.trapId())||!PestTrapTarget.unique(facts.targets()).contains(target)||port.layout(target,menu).filter(plan.layout()::equals).isEmpty())){finish(State.FAILED);return;}
    if(!proof.armNext(menu,port.inventory(),port.cursor(),now)){finish(State.FAILED);return;}var step=proof.step();var ticket=proof.ticket();GuardedMenuAction.Result result;try{result=port.transfer(handle,plan,step,ticket,now);}catch(RuntimeException ambiguous){result=GuardedMenuAction.Result.UNCERTAIN;}if(!proof.sent(result,now))finish(State.FAILED);}return;
  }
  if(state==State.CLOSING){
   if(!proof.confirmedStillMatches(c.worldId(),menu,port.inventory(),port.cursor(),trapFrame(facts),now)){finish(State.FAILED);return;}var settled=Map.copyOf(port.inventory());port.closeOwnedMenu(container);
   var after=port.context();if(!session.isCurrent(handle)||!after.canIssueCommand()||after.worldId()!=c.worldId()||!after.garden()||port.manualInput()||port.menu()!=null||!empty(port.cursor())||!settled.equals(port.inventory())){finish(State.FAILED);return;}finish(State.COMPLETE);
  }
 }
 public void cancel(){if(active())finish(State.CANCELLED);}
 private void finish(State terminal){state=terminal;if(proof!=null)proof.cancel();if(session.isCurrent(handle))session.clearExpectedMenu(handle);}
}
