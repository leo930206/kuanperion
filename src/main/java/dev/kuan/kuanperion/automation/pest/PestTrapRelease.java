// SPDX-License-Identifier: GPL-3.0-only
// Release-role concept examined in supplied Aether PestTrapManager, not its broad menu/slot guessing.
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
/** Subtask under the original Pest lease. No purchase/bait/cursor/inventory mutation authority. */
public final class PestTrapRelease {
 public enum State {IDLE,OPENING,WAITING_MENU,WAITING_RELEASE,COMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public enum Open {AIMING,SENT,REJECTED}
 public record Facts(long world,long observedAt,int alive,PestTrapObservation traps,List<PestTrapTarget> targets,Set<UUID> pestIds){
  public Facts {Objects.requireNonNull(traps);targets=List.copyOf(targets);pestIds=Set.copyOf(pestIds);}
  public Facts(long world,long observedAt,int alive,PestTrapObservation traps,List<PestTrapTarget> targets){this(world,observedAt,alive,traps,targets,Set.of());}
  boolean fresh(long world,long now){return this.world==world&&world>0&&now-observedAt>=0&&now-observedAt<=1_000_000_000L&&alive>=0&&alive<=999&&targets.size()<=24&&pestIds.size()<=256;}
 }
 public interface Port {
  AutomationContext context();Facts facts(long now);MenuSnapshot menu();
  default boolean cursorEmpty(){return true;}
  Open open(AutomationSession.Handle handle,PestTrapTarget target,long now);
  boolean release(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,long now);
  void closeOwnedMenu(int id);
 }
 private static final long SECOND=1_000_000_000L;
 private static final Set<String> BUTTONS=Set.of("minecraft:lime_dye","minecraft:green_dye","minecraft:emerald","minecraft:lime_terracotta","minecraft:green_terracotta");
 /** Opaque, one-use proof binding the actual native close HEAD/TAIL to this pending release. */
 public static final class CloseTicket {private final AutomationSession.Handle handle;private final int container;private final long clicked;private CloseTicket(AutomationSession.Handle h,int id,long clicked){handle=h;container=id;this.clicked=clicked;}}
 private CloseTicket issuedClose;private boolean serverClosed;
 private final AutomationSession session;private final Port port;private AutomationSession.Handle handle;private PestTrapTarget target;private MenuContract contract;
 private State state=State.IDLE;private long started,phaseStart,clickedAt;private int owned=-1,baseline;private Set<UUID> beforeIds=Set.of();
 public PestTrapRelease(AutomationSession session,Port port){this.session=session;this.port=port;}
 public State state(){return state;}public boolean active(){return state==State.OPENING||state==State.WAITING_MENU||state==State.WAITING_RELEASE;}
 public boolean begin(AutomationSession.Handle handle,PestTrapTarget target,long now){
  if(active()||target==null||!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.PEST)return false;
  var context=port.context();var facts=port.facts(now);
  if(!context.canIssueCommand()||!port.cursorEmpty()||!context.garden()||context.worldId()!=handle.lease().worldId()||!facts.fresh(context.worldId(),now)||!facts.traps().fullKnown()||!facts.traps().full().contains(target.trapId())||!PestTrapTarget.unique(facts.targets()).contains(target))return false;
  this.handle=handle;this.target=target;baseline=facts.alive();started=phaseStart=now;owned=-1;issuedClose=null;serverClosed=false;
  contract=new MenuContract(Set.of(target.family(),target.label()),Set.of(9,18,27,36,45,54));session.expectMenu(handle,contract,port::menu);state=State.OPENING;return true;
 }
 private boolean shape(MenuSnapshot menu){return contract!=null&&contract.matches(menu)&&menu.worldId()==handle.lease().worldId()&&menu.slots().size()==menu.containerSlots()+36;}
 public boolean ownsMenu(){var menu=port.menu();return active()&&session.isCurrent(handle)&&shape(menu)&&(owned<0||menu.containerId()==owned);}
 /** Called only from the native matching packet HEAD while the exact owned menu still exists. */
 public CloseTicket beforeServerClose(int id,long now){
  issuedClose=null;var context=port.context();var menu=port.menu();
  if(state!=State.WAITING_RELEASE||id!=owned||!session.isCurrent(handle)||!shape(menu)||menu.containerId()!=id||context.worldId()!=handle.lease().worldId()||!context.garden()||context.hardBlocked()||context.blockers().contains(AutomationContext.Blocker.FOCUS)||now-clickedAt<0||now-clickedAt>=8*SECOND)return null;
  return issuedClose=new CloseTicket(handle,id,clickedAt);
 }
 /** Native adapter must independently prove the matching packet applied and inventory menu/no GUI is current. */
 public boolean afterServerClose(CloseTicket ticket,long now){
  if(ticket==null||ticket!=issuedClose)return false;issuedClose=null;var context=port.context();
  if(state!=State.WAITING_RELEASE||!session.isCurrent(handle)||!ticket.handle.equals(handle)||ticket.container!=owned||ticket.clicked!=clickedAt||port.menu()!=null||context.blockers().contains(AutomationContext.Blocker.MENU)||context.hardBlocked()||context.blockers().contains(AutomationContext.Blocker.FOCUS)||context.worldId()!=handle.lease().worldId()||!context.garden()||now-clickedAt<0||now-clickedAt>=8*SECOND)return false;
  issuedClose=null;serverClosed=true;session.clearExpectedMenu(handle);return true;
 }
 public void tick(long now){if(!active())return;try{run(now);}catch(RuntimeException invalid){finish(State.FAILED);}}
 private void run(long now){
  var context=port.context();
  if(!session.isCurrent(handle)||context.worldId()!=handle.lease().worldId()||!context.garden()||context.hardBlocked()||context.blockers().contains(AutomationContext.Blocker.FOCUS)){finish(State.CANCELLED);return;}
  if(now-started<0||now-started>=15*SECOND){finish(State.TIMED_OUT);return;}
  var menu=port.menu();
  if(menu!=null&&(!shape(menu)||owned>=0&&menu.containerId()!=owned)){finish(State.FAILED);return;}
  if(context.blockers().contains(AutomationContext.Blocker.MENU)&&menu==null){finish(State.CANCELLED);return;}
  if(state==State.OPENING){
   if(!context.canIssueCommand()||menu!=null){finish(State.CANCELLED);return;}
   var facts=port.facts(now);if(!facts.fresh(context.worldId(),now)||!facts.traps().fullKnown()||!facts.traps().full().contains(target.trapId())||!PestTrapTarget.unique(facts.targets()).contains(target)){finish(State.FAILED);return;}
   var result=port.open(handle,target,now);if(result==Open.REJECTED){finish(State.FAILED);return;}if(result==Open.SENT){state=State.WAITING_MENU;phaseStart=now;}
   else if(now-phaseStart>=5*SECOND)finish(State.TIMED_OUT);return;
  }
  if(state==State.WAITING_RELEASE){
   if(menu==null&&!serverClosed){finish(State.CANCELLED);return;}
   var facts=port.facts(now);
   if(port.cursorEmpty()&&facts.fresh(context.worldId(),now)&&facts.observedAt()>clickedAt&&facts.traps().fullKnown()&&!facts.traps().full().contains(target.trapId())&&(facts.alive()>baseline||facts.pestIds().stream().anyMatch(id->!beforeIds.contains(id)))){finish(State.COMPLETE);return;}
   if(now-phaseStart>=8*SECOND)finish(State.TIMED_OUT);return;
  }
  if(menu==null){if(now-phaseStart>=5*SECOND)finish(State.TIMED_OUT);return;}
  owned=menu.containerId();var buttons=menu.slots().stream().filter(slot->slot.index()<menu.containerSlots()&&!slot.playerInventory()&&releaseButton(slot.item())).toList();
  if(buttons.size()!=1){finish(State.FAILED);return;}
  var before=port.facts(now);if(!before.fresh(context.worldId(),now)||!before.traps().fullKnown()||!before.traps().full().contains(target.trapId())){finish(State.FAILED);return;}baseline=before.alive();beforeIds=before.pestIds();
  var ticket=contract.plan(menu,buttons.getFirst().index(),MenuContract.Click.PICKUP);
  if(ticket==null||!port.release(handle,contract,ticket,now)){finish(State.FAILED);return;}
  // Permit only this declared contract if still visible; a server-owned close is expected after click.
  session.expectMenu(handle,contract,port::menu);state=State.WAITING_RELEASE;phaseStart=clickedAt=now;
 }
 private static boolean releaseButton(ItemSnapshot item){
  return item.count()==1&&BUTTONS.contains(item.minecraftId())&&item.name().equals("Release All Pests")&&item.lore().stream().noneMatch(line->line.toLowerCase(Locale.ROOT).matches(".*\\b(cost|price|buy|purchase|pay)\\b.*")&&!line.equals("Cost: 0 Coins"));
 }
 public void cancel(){if(active())finish(State.CANCELLED);}
 private void finish(State terminal){
  state=terminal;issuedClose=null;
  try{
   if(session.isCurrent(handle)){
    var menu=port.menu();if(terminal!=State.CANCELLED&&owned>=0&&shape(menu)&&menu.containerId()==owned){port.closeOwnedMenu(owned);if(port.menu()!=null)state=State.FAILED;}
    session.clearExpectedMenu(handle);
   }
  }catch(RuntimeException cleanup){state=State.FAILED;}
 }
}
