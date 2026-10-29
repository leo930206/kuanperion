// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
/** Explicit inventory/Sacks/budgeted-buy refill: a material stack is not considered consumed without source, resource and cursor receipts. */
public final class ComposterTask {
 public enum State{IDLE,PREPARING,TRAVEL_OUT,APPROACHING,OPENING,WAIT_MENU,FILLING,SACKS,ACQUIRING,WAIT_DEPOSIT,TRAVEL_BACK,RETURNING,RESTORING,COMPLETE,PARTIAL,CANCELLED,TIMED_OUT,FAILED}
 public enum Acquisition{RUNNING,COMPLETE,FAILED,UNCERTAIN}
 public enum Open{AIMING,SENT,REJECTED}
 public record Frame(long world,long observedAt,FlightAnchor pose,PestTargets.Point eye,boolean mayFly,boolean manualInput,boolean cursorEmpty,boolean complete){public Frame{Objects.requireNonNull(pose);Objects.requireNonNull(eye);}boolean fresh(long world,long now){return complete&&this.world==world&&world>0&&now-observedAt>=0&&now-observedAt<=1_000_000_000L;}}
 public interface Port extends PestPlotTravel.Port {
  Frame frame(long now);List<ComposterPlan.Stack> inventory();MenuSnapshot menu();
  boolean prepare(AutomationSession.Handle handle,FlightAnchor original,FlightAnchor region,long now);
  FlightMotion move(AutomationSession.Handle handle,PestTargets.Point goal,double tolerance,long now);
  Open open(AutomationSession.Handle handle,ComposterPolicy.Anchor machine,long now);
  boolean deposit(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection protection,long now);
  default boolean beginSacks(AutomationSession.Handle h,ComposterObservation.Kind kind,long ceiling,long now){return false;}
  default Acquisition pollSacks(AutomationSession.Handle h,long now){return Acquisition.FAILED;}
  default boolean ownsSacksScreen(){return false;}
  default long sacksIncrease(){return 0;}
  default void cancelSacks(){}
  default boolean beginAcquisition(AutomationSession.Handle h,ComposterPolicy policy,Set<ComposterObservation.Kind> kinds,SpendingBudget budget,long now){return false;}
  default Acquisition pollAcquisition(AutomationSession.Handle h,long now){return Acquisition.FAILED;}
  default boolean ownsAcquisitionScreen(){return false;}
  default void acquisitionMessage(long world,String text){}
  default void cancelAcquisition(){}
  void closeOwnedMenu(int id);FlightMotion restore(AutomationSession.Handle handle,FlightAnchor original,long now);
  default void dispose(AutomationSession.Handle handle){release(handle);}
  /** Failure-only physical input reconciliation; never sends gameplay or re-arms Farming. */
  default void afterStop(AutomationSession.Handle handle){}
 }
 private static final long SECOND=1_000_000_000L;private static final MenuContract MENU=new MenuContract(Set.of("Composter"),Set.of(45,54));
 private final AutomationSession session;private final InventoryReceipts inventory;private final CursorReceipts cursor;private final Port port;private final PestPlotTravel travel;
 private State state=State.IDLE;private AutomationSession.Handle handle;private ComposterPolicy policy;private FlightAnchor original,region;private PestPlot home;private long world,started,phaseStart,lastAction;private int container=-1,stacks,inserted,returnLegs,sourceSlot;private boolean partial,inputPending,expectedMenu;private ComposterPlan.Deposit pending;private ComposterObservation.Snapshot before;private InventoryReceipts.Stamp inventoryStamp;private CursorReceipts.Stamp cursorStamp;private PestTargets.Point returnGoal;private long sacksCharged,sacksInserted;private final Set<ComposterObservation.Kind> sacksAttempted=EnumSet.noneOf(ComposterObservation.Kind.class);private SpendingBudget buyingBudget;private final Set<ComposterObservation.Kind> acquired=EnumSet.noneOf(ComposterObservation.Kind.class);
 public ComposterTask(AutomationSession session,PlayerPositionReceipts positions,InventoryReceipts inventory,CursorReceipts cursor,Port port){this.session=session;this.inventory=inventory;this.cursor=cursor;this.port=port;travel=new PestPlotTravel(session,positions,port,AutomationFeature.COMPOSTER);}
 public State state(){return state;}public int inserted(){return inserted;}public long sacksInserted(){return sacksInserted;}public boolean active(){return switch(state){case PREPARING,TRAVEL_OUT,APPROACHING,OPENING,WAIT_MENU,FILLING,SACKS,ACQUIRING,WAIT_DEPOSIT,TRAVEL_BACK,RETURNING,RESTORING->true;default->false;};}
 public boolean ownsMenu(){return active()&&session.isCurrent(handle)&&(state==State.SACKS?port.ownsSacksScreen():state==State.ACQUIRING?port.ownsAcquisitionScreen():expectedMenu&&owned(port.menu()));}
 public boolean request(ComposterPolicy policy,boolean manual,long now){
  if(active()||policy==null||!policy.configured())return false;
  try{var context=port.context();var f=port.frame(now);var home=PestPlot.at(f.pose().position().x(),f.pose().position().z());
   if(!context.canIssueCommand()||!context.garden()||!f.fresh(context.worldId(),now)||!f.mayFly()||f.manualInput()||!f.cursorEmpty()||home.isEmpty()||!manual&&policy.onlyWhileFarming()&&!context.farmingActive())return false;
   var next=session.begin(AutomationFeature.COMPOSTER,now,policy.timeoutSeconds()*SECOND);if(next==null)return false;
   handle=next;this.policy=policy;buyingBudget=new SpendingBudget(policy.buying().maximumCoins());acquired.clear();sacksAttempted.clear();sacksCharged=sacksInserted=0;original=region=f.pose();this.home=home.get();world=context.worldId();started=phaseStart=lastAction=now;container=-1;stacks=inserted=returnLegs=0;partial=inputPending=expectedMenu=false;pending=null;before=null;returnGoal=null;travel.reset();state=State.PREPARING;return true;
  }catch(RuntimeException invalid){if(handle!=null)finish(State.FAILED,now);return false;}
 }
 public void tick(long now){if(active())try{run(now);}catch(RuntimeException invalid){finish(State.FAILED,now);}}
 private boolean owned(MenuSnapshot menu){return MENU.matches(menu)&&menu.worldId()==world&&(container<0||container==menu.containerId());}
 private AutomationContext currentContext(){
  var context=port.context();if(!context.blockers().contains(AutomationContext.Blocker.MENU))return context;
  if(!ownsMenu())return context;var blockers=EnumSet.noneOf(AutomationContext.Blocker.class);blockers.addAll(context.blockers());blockers.remove(AutomationContext.Blocker.MENU);return new AutomationContext(context.worldId(),context.farmingActive(),context.garden(),blockers);
 }
 private void run(long now){
  var context=currentContext();var f=port.frame(now);
  if(!session.isCurrent(handle)||!context.canIssueCommand()||!context.garden()||context.worldId()!=world||handle.hold()!=null&&!context.farmingActive()||!f.fresh(world,now)||!f.mayFly()||f.manualInput()||f.pose().slot()!=original.slot()||!PestIdentity.sameTool(original.item(),f.pose().item())){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=policy.timeoutSeconds()*SECOND){finish(State.TIMED_OUT,now);return;}
  if(state!=State.WAIT_DEPOSIT&&!f.cursorEmpty()){finish(State.CANCELLED,now);return;}
  if(state==State.SACKS){
   port.release(handle);var result=port.pollSacks(handle,now);if(result==Acquisition.RUNNING)return;
   if(result!=Acquisition.COMPLETE||!MENU.matches(port.menu())||!f.cursorEmpty()||port.sacksIncrease()<=0){finish(State.FAILED,now);return;}
   sacksInserted+=port.sacksIncrease();container=port.menu().containerId();session.expectMenu(handle,MENU,port::menu);expectedMenu=true;state=State.FILLING;lastAction=now;return;
  }
  if(state==State.ACQUIRING){
   port.release(handle);var result=port.pollAcquisition(handle,now);if(result==Acquisition.RUNNING)return;
   if(result!=Acquisition.COMPLETE||port.menu()!=null||!f.cursorEmpty()){finish(State.FAILED,now);return;}
   state=State.OPENING;phaseStart=now;return;
  }
  if(state==State.PREPARING){if(!port.prepare(handle,original,region,now)){finish(State.FAILED,now);return;}if(!travel.begin(handle,PestPlot.of(0),now)){finish(State.FAILED,now);return;}state=State.TRAVEL_OUT;phaseStart=now;return;}
  if(state==State.TRAVEL_OUT||state==State.TRAVEL_BACK){
   travel.tick(now);if(travel.arrival(handle,now).isPresent()){
    boolean returning=state==State.TRAVEL_BACK;region=f.pose();if(!port.prepare(handle,original,region,now)){finish(State.FAILED,now);return;}travel.reset();state=returning?State.RETURNING:State.APPROACHING;phaseStart=now;return;
   }
   switch(travel.state()){case TIMED_OUT->finish(State.TIMED_OUT,now);case CANCELLED->finish(State.CANCELLED,now);case FAILED->finish(State.FAILED,now);default->{}}return;
  }
  if(f.pose().position().distanceSquared(region.position())>66*66){finish(State.FAILED,now);return;}
  if(state!=State.WAIT_DEPOSIT&&state!=State.WAIT_MENU&&state!=State.RETURNING&&state!=State.RESTORING&&now-started>=(policy.timeoutSeconds()-45)*SECOND){partial=true;beginReturn(now);return;}
  if(state==State.RETURNING){
   if(returnGoal==null){var a=original.position();var p=f.pose().position();double distance=Math.sqrt(p.distanceSquared(a)),fraction=Math.min(1,41.6/Math.max(.001,distance));returnGoal=fraction==1?a:new PestTargets.Point(p.x()+(a.x()-p.x())*fraction,p.y()+(a.y()-p.y())*fraction,p.z()+(a.z()-p.z())*fraction);}
   var motion=port.move(handle,returnGoal,.35,now);if(motion==FlightMotion.BLOCKED){finish(State.FAILED,now);return;}
   if(motion==FlightMotion.ARRIVED){if(returnGoal.equals(original.position()))state=State.RESTORING;else if(++returnLegs>16)finish(State.FAILED,now);else{region=f.pose();returnGoal=null;if(!port.prepare(handle,original,region,now))finish(State.FAILED,now);}}return;
  }
  if(state==State.RESTORING){var motion=port.restore(handle,original,now);if(motion==FlightMotion.BLOCKED)finish(State.FAILED,now);else if(motion==FlightMotion.ARRIVED)finish(partial?State.PARTIAL:State.COMPLETE,now);return;}
  var machine=policy.anchor();var aim=new PestTargets.Point(machine.x()+.5,machine.y()+.25,machine.z()+.5);
  if(state==State.APPROACHING){
   double distance=Math.sqrt(f.eye().distanceSquared(aim));if(distance<=2.5){port.release(handle);state=State.OPENING;phaseStart=now;return;}
   double ratio=1-2.25/distance;var p=f.pose().position();var eye=f.eye();var goal=new PestTargets.Point(p.x()+(aim.x()-eye.x())*ratio,p.y()+(aim.y()-eye.y())*ratio,p.z()+(aim.z()-eye.z())*ratio);
   if(port.move(handle,goal,.4,now)==FlightMotion.BLOCKED)finish(State.FAILED,now);else if(now-phaseStart>=15*SECOND)finish(State.TIMED_OUT,now);return;
  }
  if(f.eye().distanceSquared(aim)>4*4){finish(State.FAILED,now);return;}
  port.release(handle);
  if(state==State.OPENING){
   if(!expectedMenu){session.expectMenu(handle,MENU,port::menu);expectedMenu=true;}
   var result=port.open(handle,machine,now);if(result==Open.REJECTED)finish(State.FAILED,now);else if(result==Open.SENT){state=State.WAIT_MENU;phaseStart=now;}else if(now-phaseStart>=5*SECOND)finish(State.TIMED_OUT,now);return;
  }
  var menu=port.menu();if(menu==null){if(state!=State.WAIT_MENU||now-phaseStart>=8*SECOND)finish(State.TIMED_OUT,now);return;}
  if(!owned(menu)){finish(State.FAILED,now);return;}if(container<0)container=menu.containerId();var observed=ComposterObservation.inspect(menu).orElse(null);if(observed==null){if(state==State.WAIT_MENU&&now-phaseStart<8*SECOND)return;finish(state==State.WAIT_MENU?State.TIMED_OUT:State.FAILED,now);return;}
  if(state==State.WAIT_MENU){state=State.FILLING;lastAction=now;return;}
  if(state==State.WAIT_DEPOSIT){
   var resourceSlot=pending.kind()==ComposterObservation.Kind.ORGANIC?observed.organicSlot():observed.fuelSlot();var resource=inventory.after(inventoryStamp,container,resourceSlot);var source=inventory.after(inventoryStamp,container,sourceSlot);
   if(resource!=null&&resource.equals(menu.item(resourceSlot))&&source!=null&&source.equals(menu.item(sourceSlot))&&inventory.confirmsDecrease(inventoryStamp,container,sourceSlot,pending.item(),pending.amount())&&ComposterObservation.increased(before,observed,pending.kind(),pending.increase())&&f.cursorEmpty()&&cursor.emptyAfter(cursorStamp,now,8*SECOND)){
    inserted+=pending.amount();stacks++;inputPending=false;pending=null;state=State.FILLING;lastAction=now;return;
   }
   if(now-lastAction>=8*SECOND)finish(State.TIMED_OUT,now);return;
  }
  if(state==State.FILLING){
   boolean needsOrganic=(policy.maximumSacksUnits()>0||policy.materials().values().stream().anyMatch(m->m.kind()==ComposterObservation.Kind.ORGANIC))&&!ComposterPlan.supplied(policy,observed.organic());boolean needsFuel=(policy.maximumSacksUnits()>0||policy.materials().values().stream().anyMatch(m->m.kind()==ComposterObservation.Kind.FUEL))&&!ComposterPlan.supplied(policy,observed.fuel());
   if(!needsOrganic&&!needsFuel){beginReturn(now);return;}if(stacks>=policy.maxStacks()||inserted>=policy.maxItems()){partial=true;beginReturn(now);return;}
   if(now-lastAction<policy.delayMillis()*1_000_000L)return;
   var items=port.inventory();var selected=needsOrganic?ComposterPlan.select(policy,ComposterObservation.Kind.ORGANIC,observed.organic(),items,policy.maxItems()-inserted):Optional.<ComposterPlan.Deposit>empty();if(selected.isEmpty()&&needsFuel)selected=ComposterPlan.select(policy,ComposterObservation.Kind.FUEL,observed.fuel(),items,policy.maxItems()-inserted);
   if(selected.isEmpty()){
    for(var kind:ComposterObservation.Kind.values())if(!sacksAttempted.contains(kind)&&(kind==ComposterObservation.Kind.ORGANIC?needsOrganic:needsFuel)){
     var resource=kind==ComposterObservation.Kind.ORGANIC?observed.organic():observed.fuel();
     if(policy.maximumSacksUnits()-sacksCharged>=resource.capacity()){
      sacksAttempted.add(kind);sacksCharged+=resource.capacity();state=State.SACKS;phaseStart=now;
      if(!port.beginSacks(handle,kind,resource.capacity(),now))finish(State.FAILED,now);return;
     }
    }
    var kinds=EnumSet.noneOf(ComposterObservation.Kind.class);
    for(var id:policy.buying().stock().keySet()){var kind=policy.materials().get(id).kind();if(!acquired.contains(kind)&&(kind==ComposterObservation.Kind.ORGANIC?needsOrganic:needsFuel))kinds.add(kind);}
    if(!kinds.isEmpty()&&buyingBudget.available()>0){
     acquired.addAll(kinds);if(!closeMenu(now))return;state=State.ACQUIRING;phaseStart=now;
     if(!port.beginAcquisition(handle,policy,Set.copyOf(kinds),buyingBudget,now))finish(State.FAILED,now);return;
    }
    partial=true;beginReturn(now);return;
   }pending=selected.get();sourceSlot=menu.containerSlots()+pending.inventorySlot()-9;var slot=menu.slot(sourceSlot);
   if(slot==null||!slot.playerInventory()||!slot.item().equals(pending.item())){finish(State.FAILED,now);return;}
   var ticket=MENU.plan(menu,sourceSlot,MenuContract.Click.PICKUP);if(ticket==null){finish(State.FAILED,now);return;}
   before=observed;inventoryStamp=inventory.stamp(world);cursorStamp=cursor.stamp(world,container,now);inputPending=true;
   if(!port.deposit(handle,MENU,ticket,new InventoryProtection(policy.materials().keySet(),policy.protectedUuids()),now)){finish(State.FAILED,now);return;}lastAction=now;state=State.WAIT_DEPOSIT;
  }
 }
 private boolean closeMenu(long now){
  port.release(handle);if(inputPending){finish(State.FAILED,now);return false;}
  if(container>=0){if(!owned(port.menu())||!port.frame(now).cursorEmpty()){finish(State.FAILED,now);return false;}port.closeOwnedMenu(container);if(port.menu()!=null){finish(State.FAILED,now);return false;}container=-1;}
  if(expectedMenu){session.clearExpectedMenu(handle);expectedMenu=false;}
  return true;
 }
 private void beginReturn(long now){
  if(!closeMenu(now))return;
  travel.reset();if(!travel.begin(handle,home,now)){finish(State.FAILED,now);return;}state=State.TRAVEL_BACK;phaseStart=now;returnGoal=null;
 }
 public void onSystemMessage(long world,String text){if(state==State.ACQUIRING)port.acquisitionMessage(world,text);}
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){
  var done=handle;handle=null;state=terminal;travel.reset();
  var result=AutomationCompletion.finish(session,done,(terminal==State.COMPLETE||terminal==State.PARTIAL),now,()->AutomationStops.run(port::cancelAcquisition,port::cancelSacks,()->port.release(done)),()->port.dispose(done),()->port.afterStop(done));
  if(result.failed())state=State.FAILED;
 }
}
