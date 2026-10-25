// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
/** Inventory-backed visitor run. No buying, blind refusal, repeated accept, or success based on NPC disappearance. */
public final class VisitorTask {
 public enum State{IDLE,DATA,PREPARING,TRAVEL_OUT,DISCOVERING,APPROACHING,OPENING,WAIT_MENU,EVALUATING,WAIT_SUBMISSION,TRAVEL_BACK,RETURNING,RESTORING,COMPLETE,PARTIAL,CANCELLED,TIMED_OUT,FAILED}
 public enum Open{AIMING,SENT,REJECTED}
 public record Frame(long world,long observedAt,FlightAnchor pose,PestTargets.Point eye,boolean mayFly,boolean manualInput,boolean cursorEmpty,boolean complete){
  public Frame{Objects.requireNonNull(pose);Objects.requireNonNull(eye);}boolean fresh(long world,long now){return complete&&this.world==world&&world>0&&now-observedAt>=0&&now-observedAt<=1_000_000_000L;}
 }
 public record Data(Map<String,String> materialIds,BazaarMarket market){public Data{materialIds=Map.copyOf(materialIds);Objects.requireNonNull(market);}}
 public interface Port extends PestPlotTravel.Port {
  Frame frame(long now);Map<Integer,ItemSnapshot> inventory();VisitorQueue queue();List<VisitorTargets.Target> targets(VisitorQueue queue);MenuSnapshot menu();
  void requestData(AutomationSession.Handle handle);Optional<Data> data();long wallMillis();
  boolean prepare(AutomationSession.Handle handle,FlightAnchor original,FlightAnchor region,long now);
  FlightMotion move(AutomationSession.Handle handle,PestTargets.Point goal,double tolerance,long now);Open open(AutomationSession.Handle handle,VisitorTargets.Target target,long now);
  GuardedMenuAction.Result submit(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,long now);
  void closeOwnedMenu(int id);FlightMotion restore(AutomationSession.Handle handle,FlightAnchor original,long now);
  default void dispose(AutomationSession.Handle handle){release(handle);}
  /** Failure-only physical input reconciliation; never sends gameplay or re-arms Farming. */
  default void afterStop(AutomationSession.Handle handle){}
 }
 /** Issued while vanilla still displays the precise pending submission menu; consumed by one TAIL only. */
 public static final class CloseTicket{private final AutomationSession.Handle handle;private final int container;private final VisitorSubmission submission;private CloseTicket(AutomationSession.Handle h,int id,VisitorSubmission s){handle=h;container=id;submission=s;}}
 private static final long SECOND=1_000_000_000L;
 private final AutomationSession session;private final InventoryReceipts receipts;private final CursorReceipts cursors;private final Port port;private final PestPlotTravel travel;
 private State state=State.IDLE;private AutomationSession.Handle handle;private VisitorPolicy policy;private FlightAnchor original,region;private PestPlot home;private long world,started,phaseStart;private int confirmed,skipped,container=-1,returnLegs;private boolean expectedMenu,serverClosed,partial;private MenuContract contract;private VisitorTargets.Target target;private VisitorSubmission submission;private CloseTicket closeTicket;private PestTargets.Point returnGoal;private final Set<String> attempted=new HashSet<>();
 public VisitorTask(AutomationSession session,PlayerPositionReceipts positions,InventoryReceipts receipts,CursorReceipts cursors,Port port){this.session=session;this.receipts=receipts;this.cursors=cursors;this.port=port;travel=new PestPlotTravel(session,positions,port,AutomationFeature.VISITOR);}
 public State state(){return state;}public int confirmed(){return confirmed;}public int skipped(){return skipped;}
 public boolean active(){return switch(state){case DATA,PREPARING,TRAVEL_OUT,DISCOVERING,APPROACHING,OPENING,WAIT_MENU,EVALUATING,WAIT_SUBMISSION,TRAVEL_BACK,RETURNING,RESTORING->true;default->false;};}
 private boolean owned(MenuSnapshot m){return expectedMenu&&contract!=null&&contract.matches(m)&&m.worldId()==world&&(container<0||m.containerId()==container);}
 public boolean ownsMenu(){return active()&&session.isCurrent(handle)&&owned(port.menu());}
 private AutomationContext context(){var c=port.context();if(!c.blockers().contains(AutomationContext.Blocker.MENU)||!owned(port.menu()))return c;var b=EnumSet.noneOf(AutomationContext.Blocker.class);b.addAll(c.blockers());b.remove(AutomationContext.Blocker.MENU);return new AutomationContext(c.worldId(),c.farmingActive(),c.garden(),b);}
 public boolean request(VisitorPolicy policy,boolean manual,long now){
  if(active()||policy==null||!policy.configured())return false;
  try{var c=port.context();var f=port.frame(now);var q=port.queue();var home=PestPlot.at(f.pose().position().x(),f.pose().position().z());
   if(!c.canIssueCommand()||!c.garden()||!f.fresh(c.worldId(),now)||!f.mayFly()||f.manualInput()||!f.cursorEmpty()||home.isEmpty()||!q.known()||q.count()==0||!manual&&(q.count()<policy.threshold()||policy.onlyWhileFarming()&&!c.farmingActive())||q.entries().stream().noneMatch(e->policy.rules().visitors().contains(e.name())))return false;
   var next=session.begin(AutomationFeature.VISITOR,now,policy.timeoutSeconds()*SECOND);if(next==null)return false;
   handle=next;this.policy=policy;original=region=f.pose();this.home=home.get();world=c.worldId();started=phaseStart=now;container=-1;confirmed=skipped=returnLegs=0;expectedMenu=serverClosed=partial=false;target=null;submission=null;closeTicket=null;contract=null;returnGoal=null;attempted.clear();travel.reset();state=State.DATA;port.requestData(handle);return true;
  }catch(RuntimeException invalid){if(handle!=null)finish(State.FAILED,now);return false;}
 }
 public void tick(long now){if(active())try{run(now);}catch(RuntimeException invalid){finish(State.FAILED,now);}}
 private void run(long now){
  var c=context();var f=port.frame(now);
  if(!session.isCurrent(handle)||!c.canIssueCommand()||!c.garden()||c.worldId()!=world||handle.hold()!=null&&!c.farmingActive()||!f.fresh(world,now)||!f.mayFly()||f.manualInput()||f.pose().slot()!=original.slot()||!PestIdentity.sameTool(original.item(),f.pose().item())){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=policy.timeoutSeconds()*SECOND){finish(State.TIMED_OUT,now);return;}
  if(state!=State.WAIT_SUBMISSION&&!f.cursorEmpty()){finish(State.CANCELLED,now);return;}
  if(state==State.DATA){port.release(handle);if(port.data().isPresent()){state=State.PREPARING;return;}if(now-phaseStart>=12*SECOND)finish(State.TIMED_OUT,now);return;}
  if(state==State.PREPARING){if(!port.prepare(handle,original,region,now)||!travel.begin(handle,PestPlot.of(0),now)){finish(State.FAILED,now);return;}state=State.TRAVEL_OUT;phaseStart=now;return;}
  if(state==State.TRAVEL_OUT||state==State.TRAVEL_BACK){
   travel.tick(now);if(travel.arrival(handle,now).isPresent()){boolean returning=state==State.TRAVEL_BACK;region=f.pose();if(!port.prepare(handle,original,region,now)){finish(State.FAILED,now);return;}travel.reset();state=returning?State.RETURNING:State.DISCOVERING;phaseStart=now;return;}
   switch(travel.state()){case TIMED_OUT->finish(State.TIMED_OUT,now);case CANCELLED->finish(State.CANCELLED,now);case FAILED->finish(State.FAILED,now);default->{}}return;
  }
  if(f.pose().position().distanceSquared(region.position())>66*66){finish(State.FAILED,now);return;}
  if(state==State.RETURNING){
   if(returnGoal==null){var a=original.position();var p=f.pose().position();double distance=Math.sqrt(p.distanceSquared(a)),fraction=Math.min(1,41.6/Math.max(.001,distance));returnGoal=fraction==1?a:new PestTargets.Point(p.x()+(a.x()-p.x())*fraction,p.y()+(a.y()-p.y())*fraction,p.z()+(a.z()-p.z())*fraction);}
   var motion=port.move(handle,returnGoal,.35,now);if(motion==FlightMotion.BLOCKED){finish(State.FAILED,now);return;}
   if(motion==FlightMotion.ARRIVED){if(returnGoal.equals(original.position()))state=State.RESTORING;else if(++returnLegs>16)finish(State.FAILED,now);else{region=f.pose();returnGoal=null;if(!port.prepare(handle,original,region,now))finish(State.FAILED,now);}}return;
  }
  if(state==State.RESTORING){var result=port.restore(handle,original,now);if(result==FlightMotion.BLOCKED)finish(State.FAILED,now);else if(result==FlightMotion.ARRIVED)finish(partial||skipped>0?State.PARTIAL:State.COMPLETE,now);return;}
  if(state==State.WAIT_SUBMISSION){
   port.release(handle);var m=port.menu();if(m==null?!serverClosed:serverClosed||!owned(m)){finish(State.CANCELLED,now);return;}
   submission.poll(world,port.inventory(),f.cursorEmpty(),now);
   if(submission.state()==VisitorSubmission.State.CONFIRMED){confirmed++;if(!closeMenu(now))return;target=null;submission=null;state=State.DISCOVERING;phaseStart=now;return;}
   if(submission.state()!=VisitorSubmission.State.AWAITING){finish(State.TIMED_OUT,now);return;}return;
  }
  if(now-started>=(policy.timeoutSeconds()-45)*SECOND&&state!=State.WAIT_MENU&&state!=State.EVALUATING){partial=true;beginReturn(now);return;}
  if(state==State.DISCOVERING){
   port.release(handle);var q=port.queue();if(!q.known()){if(now-phaseStart>=5*SECOND)finish(State.TIMED_OUT,now);return;}
   var eligible=q.entries().stream().filter(e->policy.rules().visitors().contains(e.name())&&!attempted.contains(e.name())).toList();
   if(confirmed>=policy.maxVisitors()||eligible.isEmpty()){partial|=confirmed>=policy.maxVisitors()&&!eligible.isEmpty();beginReturn(now);return;}
   var candidates=port.targets(q);for(var entry:eligible){var matches=candidates.stream().filter(t->t.visitor().equals(entry.name())).toList();if(matches.size()!=1)continue;target=matches.getFirst();state=State.APPROACHING;phaseStart=now;return;}
   if(now-phaseStart>=5*SECOND){partial=true;beginReturn(now);}return;
  }
  if(state==State.APPROACHING||state==State.OPENING){
   var q=port.queue();var matches=port.targets(q).stream().filter(this::sameTarget).toList();if(!q.known()||matches.size()!=1){finish(State.CANCELLED,now);return;}var current=matches.getFirst();var aim=new PestTargets.Point(current.position().x(),current.position().y()+1,current.position().z());double distance=Math.sqrt(f.eye().distanceSquared(aim));
   if(state==State.APPROACHING){
    if(distance<=2.5){port.release(handle);contract=new MenuContract(Set.of(target.visitor()),Set.of(9,18,27,36,45,54));session.expectMenu(handle,contract,port::menu);expectedMenu=true;serverClosed=false;container=-1;state=State.OPENING;phaseStart=now;return;}
    double ratio=1-2.25/distance;var p=f.pose().position();var eye=f.eye();var goal=new PestTargets.Point(p.x()+(aim.x()-eye.x())*ratio,p.y()+(aim.y()-eye.y())*ratio,p.z()+(aim.z()-eye.z())*ratio);
    if(port.move(handle,goal,.4,now)==FlightMotion.BLOCKED)finish(State.FAILED,now);else if(now-phaseStart>=15*SECOND)finish(State.TIMED_OUT,now);return;
   }
   port.release(handle);if(distance>2.75){finish(State.FAILED,now);return;}var result=port.open(handle,current,now);if(result==Open.REJECTED)finish(State.FAILED,now);else if(result==Open.SENT){state=State.WAIT_MENU;phaseStart=now;}else if(now-phaseStart>=5*SECOND)finish(State.TIMED_OUT,now);return;
  }
  port.release(handle);var m=port.menu();if(m==null){if(now-phaseStart>=8*SECOND)finish(State.TIMED_OUT,now);return;}if(!owned(m)){finish(State.FAILED,now);return;}container=m.containerId();var data=port.data();var offer=data.flatMap(d->VisitorOffer.inspect(m,target.visitor(),d.materialIds()));
  if(offer.isEmpty()){if(now-phaseStart>=8*SECOND)finish(State.TIMED_OUT,now);return;}
  if(state==State.WAIT_MENU){state=State.EVALUATING;return;}
  var inv=port.inventory();var main=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<36;i++)if(inv.get(i)!=null)main.put(i,inv.get(i));
  var estimate=VisitorValuation.estimate(offer.get(),policy.rules(),main,data.get().market(),port.wallMillis());attempted.add(target.visitor());
  if(estimate.isEmpty()||!estimate.get().missing().isEmpty()){skipped++;if(!closeMenu(now))return;target=null;state=State.DISCOVERING;phaseStart=now;return;}
  var ticket=contract.plan(m,offer.get().acceptSlot(),MenuContract.Click.PICKUP);if(ticket==null){finish(State.FAILED,now);return;}
  submission=new VisitorSubmission(receipts,cursors,()->session.isCurrent(handle));state=State.WAIT_SUBMISSION;phaseStart=now;
  if(!submission.begin(offer.get(),policy.rules(),inv,data.get().market(),port.wallMillis(),now,f.cursorEmpty(),()->port.submit(handle,contract,ticket,now)))finish(State.FAILED,now);
 }
 private boolean sameTarget(VisitorTargets.Target t){return target!=null&&t.visitor().equals(target.visitor())&&t.id()==target.id()&&t.uuid().equals(target.uuid())&&t.markerId()==target.markerId()&&t.markerUuid().equals(target.markerUuid())&&t.position().distanceSquared(target.position())<=1&&t.markerPosition().distanceSquared(target.markerPosition())<=1;}
 public void onSystemMessage(long world,String message){if(state==State.WAIT_SUBMISSION&&submission!=null)submission.onSystemMessage(world,message);}
 public CloseTicket beforeServerClose(int id,long now){
  closeTicket=null;if(state!=State.WAIT_SUBMISSION||submission==null||submission.state()!=VisitorSubmission.State.AWAITING||serverClosed||!session.isCurrent(handle)||!owned(port.menu())||container!=id||now-phaseStart<0||now-phaseStart>=15*SECOND)return null;
  var c=context();var f=port.frame(now);if(!c.canIssueCommand()||!c.garden()||c.worldId()!=world||!f.fresh(world,now)||f.manualInput())return null;
  return closeTicket=new CloseTicket(handle,id,submission);
 }
 public boolean afterServerClose(CloseTicket ticket,long now){
  var issued=closeTicket;closeTicket=null;if(ticket==null||issued!=ticket||state!=State.WAIT_SUBMISSION||ticket.handle!=handle||ticket.submission!=submission||ticket.container!=container||port.menu()!=null||!session.isCurrent(handle))return false;
  var c=port.context();var f=port.frame(now);if(!c.canIssueCommand()||!c.garden()||c.worldId()!=world||!f.fresh(world,now)||f.manualInput()||!submission.appliedServerClose(world,container,now))return false;
  serverClosed=true;session.clearExpectedMenu(handle);expectedMenu=false;return true;
 }
 private boolean closeMenu(long now){
  if(!port.frame(now).cursorEmpty()){finish(State.FAILED,now);return false;}
  if(port.menu()!=null){if(!owned(port.menu())){finish(State.FAILED,now);return false;}port.closeOwnedMenu(container);if(port.menu()!=null){finish(State.FAILED,now);return false;}}
  else if(expectedMenu&&!serverClosed){finish(State.FAILED,now);return false;}
  session.clearExpectedMenu(handle);expectedMenu=false;serverClosed=false;container=-1;closeTicket=null;return true;
 }
 private void beginReturn(long now){if(!closeMenu(now))return;port.release(handle);travel.reset();if(!travel.begin(handle,home,now)){finish(State.FAILED,now);return;}state=State.TRAVEL_BACK;phaseStart=now;returnGoal=null;}
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){
  var done=handle;handle=null;state=terminal;closeTicket=null;travel.reset();if(submission!=null)submission.cancel();
  var result=AutomationCompletion.finish(session,done,(terminal==State.COMPLETE||terminal==State.PARTIAL),now,()->port.release(done),()->port.dispose(done),()->port.afterStop(done));
  if(result.failed())state=State.FAILED;
 }
}
