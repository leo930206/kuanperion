// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
/** Bounded local cleanup and optional explicit search. A disappearance is not a kill: require a coupled server Tab decrease. */
public final class PestTask {
 public enum State {IDLE,PREPARING,SEEKING,SWEEPING,TRAP_APPROACH,TRAP_MENU,TRAVEL_OUT,TRAVEL_BACK,CAPTURING,RETURNING,RESTORING,COMPLETE,INCOMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public enum Motion {MOVING,ARRIVED,BLOCKED}
 public enum Capture {AIMING,USING,REJECTED}
 public record Anchor(PestTargets.Point position,float yaw,float pitch,int slot,ItemSnapshot item,boolean flying){}
 public record Frame(long world,long observedAt,PestTargets.Point position,PestTargets.Point eye,float yaw,float pitch,int slot,ItemSnapshot item,boolean mayFly,boolean flying,boolean manualInput,int alive,List<PestTargets.Target> targets,List<PestIdentity.Vacuum> tools,boolean complete,Set<Integer> plots,boolean plotsKnown){
  public Frame {Objects.requireNonNull(position);Objects.requireNonNull(eye);Objects.requireNonNull(item);targets=List.copyOf(targets);tools=List.copyOf(tools);plots=Set.copyOf(plots);if(plots.size()>24||plots.stream().anyMatch(id->id<1||id>24)||!plotsKnown&&!plots.isEmpty())throw new IllegalArgumentException("Invalid plot observation");}
  public Frame(long world,long observedAt,PestTargets.Point position,PestTargets.Point eye,float yaw,float pitch,int slot,ItemSnapshot item,boolean mayFly,boolean flying,boolean manualInput,int alive,List<PestTargets.Target> targets,List<PestIdentity.Vacuum> tools,boolean complete){this(world,observedAt,position,eye,yaw,pitch,slot,item,mayFly,flying,manualInput,alive,targets,tools,complete,Set.of(),false);}
  public Frame withPlots(Set<Integer> plots,boolean known){return new Frame(world,observedAt,position,eye,yaw,pitch,slot,item,mayFly,flying,manualInput,alive,targets,tools,complete,plots,known);}
  public boolean fresh(long world,long now){long age=now-observedAt;return complete&&this.world==world&&world>0&&age>=0&&age<=1_000_000_000L&&alive>=0&&alive<=1000&&slot>=0&&slot<9&&Float.isFinite(yaw)&&Float.isFinite(pitch)&&Math.abs(pitch)<=90&&targets.size()<=256&&tools.size()<=9;}
 }
 public interface Port extends PestPlotTravel.Port,PestTrapRelease.Port {
  default PestTrapRelease.Facts facts(long now){var f=frame(now);return new PestTrapRelease.Facts(f.world(),f.observedAt(),f.alive(),PestTrapObservation.unknown(),List.of());}
  default MenuSnapshot menu(){return null;}
  default PestTrapRelease.Open open(AutomationSession.Handle h,PestTrapTarget target,long now){return PestTrapRelease.Open.REJECTED;}
  default boolean release(AutomationSession.Handle h,MenuContract contract,MenuContract.Ticket ticket,long now){return false;}
  default void closeOwnedMenu(int id){}
  default PestTargets.Point position(){return null;}
  default boolean travel(AutomationSession.Handle handle,PestPlot plot,long now){return false;}
  AutomationContext context();Frame frame(long now);
  boolean prepare(AutomationSession.Handle handle,PestIdentity.Vacuum tool,Anchor anchor,int radius,long now);
  Motion move(AutomationSession.Handle handle,PestTargets.Point goal,double tolerance,long now);
  Capture capture(AutomationSession.Handle handle,PestTargets.Target target,PestIdentity.Vacuum tool,long now);
  Motion restore(AutomationSession.Handle handle,Anchor anchor,long now);
  /** Release only this handle's overlay, including after revocation; never change user's look/tool. */
  void release(AutomationSession.Handle handle);
  default void dispose(AutomationSession.Handle handle){release(handle);}
 }
 private static final long SECOND=1_000_000_000L;
 private final AutomationSession session;private final Port port;private State state=State.IDLE;private AutomationSession.Handle handle;
 private final PestTrapRelease trap;private PestTrapTarget trapTarget;private final Set<Integer> handledTraps=new HashSet<>();
 private final PestPlotTravel travel;private PestPolicy policy;private Anchor anchor,region;private PestPlot homePlot;private boolean traveled;private int returnLegs;private PestTargets.Point returnGoal;private final Set<Integer> visited=new HashSet<>();private PestIdentity.Vacuum tool;private PestTargets.Target target;
 private long world,started,phaseStart;private int baseline,confirmed;private boolean used,incomplete,sweepArrived;private int sweepIndex;private long sweepArrival;
 private static final int[][] SWEEP={{1,1},{-1,1},{-1,-1},{1,-1}};
 public PestTrapRelease.CloseTicket beforeServerClose(int id,long now){return state==State.TRAP_MENU?trap.beforeServerClose(id,now):null;}
 public boolean afterServerClose(PestTrapRelease.CloseTicket ticket,long now){return state==State.TRAP_MENU&&trap.afterServerClose(ticket,now);}
 public PestTask(AutomationSession session,Port port){this(session,port,new PlayerPositionReceipts());}
 public PestTask(AutomationSession session,Port port,PlayerPositionReceipts receipts){this.session=session;this.port=port;travel=new PestPlotTravel(session,receipts,port);trap=new PestTrapRelease(session,port);}
 public boolean ownsMenu(){try{return state==State.TRAP_MENU&&trap.ownsMenu();}catch(RuntimeException invalid){return false;}}
 public State state(){return state;}public int confirmedDecreases(){return confirmed;}
 public boolean active(){return switch(state){case PREPARING,SEEKING,SWEEPING,TRAP_APPROACH,TRAP_MENU,TRAVEL_OUT,TRAVEL_BACK,CAPTURING,RETURNING,RESTORING->true;default->false;};}
 public boolean request(PestPolicy policy,boolean manual,long now){
  if(active())return false;
  try{
   var context=port.context();var f=port.frame(now);
   boolean trapReady=policy.clearTraps()&&trapCandidates(port.facts(now),f.position(),policy.radius(),now,context.worldId()).size()>0;
   var trapFacts=port.facts(now);boolean trapTravel=policy.clearTraps()&&policy.trapPlot()>=0&&policy.travelPlots()>0&&trapFacts.fresh(context.worldId(),now)&&trapFacts.traps().fullKnown()&&!trapFacts.traps().full().isEmpty()&&PestPlot.at(f.position().x(),f.position().z()).isPresent();
   boolean plotTravel=(policy.travelPlots()>0&&f.plotsKnown()&&!f.plots().isEmpty()&&PestPlot.at(f.position().x(),f.position().z()).isPresent())||trapTravel;
   if(!context.canIssueCommand()||!context.garden()||!f.fresh(context.worldId(),now)||f.manualInput()||!f.mayFly()||f.alive()<policy.threshold()&&!trapReady&&!trapTravel||f.targets().isEmpty()&&!policy.sweep()&&!plotTravel&&!trapReady||!manual&&policy.onlyWhileFarming()&&!context.farmingActive())return false;
   var best=f.tools().stream().filter(t->PestIdentity.vacuum(t.slot(),t.item()).filter(v->v.equals(t)).isPresent()).max(Comparator.comparingDouble(PestIdentity.Vacuum::range).thenComparingInt(t->-t.slot())).orElse(null);
   if(best==null||!policy.sweep()&&!plotTravel&&!trapReady&&f.targets().stream().noneMatch(t->f.position().distanceSquared(t.position())<=policy.radius()*policy.radius()))return false;
   var next=session.begin(AutomationFeature.PEST,now,policy.timeoutSeconds()*SECOND);if(next==null)return false;
   handle=next;this.policy=policy;tool=best;anchor=new Anchor(f.position(),f.yaw(),f.pitch(),f.slot(),f.item(),f.flying());region=anchor;homePlot=PestPlot.at(f.position().x(),f.position().z()).orElse(null);traveled=false;visited.clear();handledTraps.clear();trapTarget=null;travel.reset();returnLegs=0;returnGoal=null;world=context.worldId();started=phaseStart=now;confirmed=0;sweepIndex=0;incomplete=false;sweepArrived=false;used=false;target=null;state=State.PREPARING;return true;
  }catch(RuntimeException invalid){return false;}
 }
 public void tick(long now){
  if(!active())return;
  try{tickChecked(now);}catch(RuntimeException invalid){finish(State.FAILED,now);}
 }
 private void tickChecked(long now){
  var context=port.context();var f=port.frame(now);
  if(!session.isCurrent(handle)||!context.canIssueCommand()&&!(context.blockers().equals(Set.of(AutomationContext.Blocker.MENU))&&ownsMenu())||!context.garden()||handle.hold()!=null&&!context.farmingActive()||context.worldId()!=world||!f.fresh(world,now)||f.manualInput()||!f.mayFly()){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=policy.timeoutSeconds()*SECOND){finish(State.TIMED_OUT,now);return;}
  if(state!=State.PREPARING&&(f.slot()!=tool.slot()||!PestIdentity.sameVacuum(tool,f.item()))){finish(State.CANCELLED,now);return;}
  if(state==State.TRAVEL_OUT||state==State.TRAVEL_BACK){
   travel.tick(now);var arrived=travel.arrival(handle,now);
   if(arrived.isPresent()){
    region=new Anchor(f.position(),f.yaw(),f.pitch(),f.slot(),f.item(),f.flying());
    if(!port.prepare(handle,tool,region,policy.radius(),now)){finish(State.FAILED,now);return;}
    boolean returning=state==State.TRAVEL_BACK;travel.reset();state=returning?State.RETURNING:State.SEEKING;sweepIndex=0;phaseStart=now;return;
   }
   switch(travel.state()){case TIMED_OUT->finish(State.TIMED_OUT,now);case CANCELLED->finish(State.CANCELLED,now);case FAILED->finish(State.FAILED,now);default->{}}return;
  }
  if(f.position().distanceSquared(region.position())>Math.pow(policy.radius()+2,2)){finish(State.FAILED,now);return;}
  if(state==State.PREPARING){if(!port.prepare(handle,tool,anchor,policy.radius(),now)){finish(State.FAILED,now);return;}state=State.SEEKING;phaseStart=now;return;}
  if(f.slot()!=tool.slot()||!PestIdentity.sameVacuum(tool,f.item())){finish(State.CANCELLED,now);return;}
  if(state==State.TRAP_MENU){
   port.release(handle);trap.tick(now);
   if(!trap.active())switch(trap.state()){
    case COMPLETE->{state=State.SEEKING;sweepIndex=0;phaseStart=now;}
    case CANCELLED->finish(State.CANCELLED,now);case TIMED_OUT->finish(State.TIMED_OUT,now);default->finish(State.FAILED,now);
   }return;
  }
  if(state==State.TRAP_APPROACH){
   port.release(handle);var facts=port.facts(now);var current=trapCandidates(facts,region.position(),policy.radius(),now,world).stream().filter(t->t.trapId()==trapTarget.trapId()&&t.entityId()==trapTarget.entityId()&&t.uuid().equals(trapTarget.uuid())).findFirst();
   if(current.isEmpty()){finish(State.FAILED,now);return;}trapTarget=current.get();
   double distance=Math.sqrt(f.eye().distanceSquared(trapTarget.position()));
   if(distance<=2.75){if(!trap.begin(handle,trapTarget,now)){finish(State.FAILED,now);return;}state=State.TRAP_MENU;return;}
   double ratio=1-2.25/distance;var p=f.position();var eye=f.eye();var aim=trapTarget.position();var goal=new PestTargets.Point(p.x()+(aim.x()-eye.x())*ratio,p.y()+(aim.y()-eye.y())*ratio,p.z()+(aim.z()-eye.z())*ratio);
   if(region.position().distanceSquared(goal)>policy.radius()*policy.radius()||port.move(handle,goal,.4,now)==Motion.BLOCKED){finish(State.FAILED,now);return;}
   if(now-phaseStart>=15*SECOND)finish(State.TIMED_OUT,now);return;
  }
  if(state==State.RETURNING){
   port.release(handle);
   if(returnGoal==null){
    double distance=Math.sqrt(f.position().distanceSquared(anchor.position()));double fraction=!traveled?1:Math.min(1,policy.radius()*.65/Math.max(.001,distance));var p=f.position();var a=anchor.position();
    returnGoal=fraction==1?a:new PestTargets.Point(p.x()+(a.x()-p.x())*fraction,p.y()+(a.y()-p.y())*fraction,p.z()+(a.z()-p.z())*fraction);
   }
   var motion=port.move(handle,returnGoal,.35,now);
   if(motion==Motion.BLOCKED)finish(State.FAILED,now);else if(motion==Motion.ARRIVED){
    if(returnGoal.equals(anchor.position())){state=State.RESTORING;phaseStart=now;}
    else if(++returnLegs>16)finish(State.FAILED,now);
    else{region=new Anchor(returnGoal,f.yaw(),f.pitch(),f.slot(),f.item(),true);returnGoal=null;if(!port.prepare(handle,tool,region,policy.radius(),now))finish(State.FAILED,now);}
   }return;
  }
  if(state==State.RESTORING){var motion=port.restore(handle,anchor,now);if(motion==Motion.BLOCKED)finish(State.FAILED,now);else if(motion==Motion.ARRIVED)finish(incomplete?State.INCOMPLETE:State.COMPLETE,now);return;}
  if(state==State.SWEEPING){
   if(f.targets().stream().anyMatch(t->region.position().distanceSquared(t.position())<=policy.radius()*policy.radius())){port.release(handle);state=State.SEEKING;phaseStart=now;return;}
   if(sweepArrived){if(now-sweepArrival>=SECOND){sweepIndex++;state=State.SEEKING;phaseStart=now;}return;}
   double offset=policy.radius()*.5;var direction=SWEEP[sweepIndex];var start=region.position();
   var goal=new PestTargets.Point(start.x()+direction[0]*offset,start.y()+Math.min(6,policy.radius()*.25),start.z()+direction[1]*offset);
   var motion=port.move(handle,goal,.6,now);if(motion==Motion.BLOCKED)finish(State.FAILED,now);else if(motion==Motion.ARRIVED){sweepArrived=true;sweepArrival=now;}return;
  }
  if(state==State.SEEKING){
   port.release(handle);
   if(confirmed>=policy.maxTargets()||f.alive()==0&&f.targets().isEmpty()){
    if(confirmed<policy.maxTargets()&&policy.clearTraps()&&beginTrap(f,now))return;
    incomplete=f.alive()>0;if(policy.clearTraps()){var facts=port.facts(now);incomplete|=!facts.fresh(world,now)||!facts.traps().fullKnown()||!facts.traps().full().isEmpty();}beginReturn(now);return;
   }
   if(f.alive()==0){if(now-phaseStart>=5*SECOND)finish(State.FAILED,now);return;}
   target=f.targets().stream().filter(t->region.position().distanceSquared(t.position())<=policy.radius()*policy.radius()).min(Comparator.comparingDouble(t->f.position().distanceSquared(t.position()))).orElse(null);
   if(target==null){
    if(policy.clearTraps()){
     var facts=port.facts(now);boolean knownFull=facts.fresh(world,now)&&facts.traps().fullKnown()&&!facts.traps().full().isEmpty();
     if(knownFull&&(!trapCandidates(facts,region.position(),policy.radius(),now,world).isEmpty()||policy.trapPlot()>=0&&policy.travelPlots()>0)&&beginTrap(f,now))return;
    }
    if(policy.sweep()){if(sweepIndex<SWEEP.length){sweepArrived=false;state=State.SWEEPING;}else if(!nextPlot(f,now)){incomplete=true;beginReturn(now);}}
    else if(!nextPlot(f,now)&&now-phaseStart>=5*SECOND){if(policy.travelPlots()>0){incomplete=true;beginReturn(now);}else finish(State.FAILED,now);}return;
   }
   baseline=f.alive();used=false;phaseStart=now;state=State.CAPTURING;
  }
  if(state==State.CAPTURING){
   var next=f.targets().stream().filter(t->t.uuid().equals(target.uuid())&&t.id()==target.id()&&t.markerId()==target.markerId()).findFirst().orElse(null);
   if(next==null){
    port.release(handle);
    boolean markerGone=f.targets().stream().noneMatch(t->t.markerId()==target.markerId());
    if(used&&markerGone&&f.alive()<baseline){confirmed++;sweepIndex=0;state=State.SEEKING;phaseStart=now;return;}
   }else{
    target=next;
    double distance=Math.sqrt(f.eye().distanceSquared(target.position()));
    if(distance>tool.range()*.75){
     port.release(handle);double ratio=1-tool.range()*.65/distance;var p=f.position();var eye=f.eye();var aim=target.position();
     var goal=new PestTargets.Point(p.x()+(aim.x()-eye.x())*ratio,p.y()+(aim.y()-eye.y())*ratio,p.z()+(aim.z()-eye.z())*ratio);
     if(region.position().distanceSquared(goal)>policy.radius()*policy.radius()||port.move(handle,goal,.6,now)==Motion.BLOCKED){finish(State.FAILED,now);return;}
    }else{
     var capture=port.capture(handle,target,tool,now);if(capture==Capture.REJECTED){finish(State.FAILED,now);return;}if(capture==Capture.USING){if(!used)baseline=f.alive();used=true;}
    }
   }
   if(now-phaseStart>=policy.captureSeconds()*SECOND)finish(State.TIMED_OUT,now);
  }
 }
 private static List<PestTrapTarget> trapCandidates(PestTrapRelease.Facts facts,PestTargets.Point center,int radius,long now,long world){
  if(!facts.fresh(world,now)||!facts.traps().fullKnown())return List.of();
  return PestTrapTarget.unique(facts.targets()).stream().filter(t->facts.traps().full().contains(t.trapId())&&center.distanceSquared(t.position())<=radius*radius).toList();
 }
 private boolean beginTrap(Frame frame,long now){
  var facts=port.facts(now);
  if(!facts.fresh(world,now)||!facts.traps().fullKnown()){incomplete=true;beginReturn(now);return true;}
  if(facts.traps().full().isEmpty())return false;
  var next=trapCandidates(facts,region.position(),policy.radius(),now,world).stream().filter(t->!handledTraps.contains(t.trapId())).min(Comparator.comparingDouble(t->frame.position().distanceSquared(t.position())));
  if(next.isEmpty()){
   if(policy.trapPlot()>=0&&policy.travelPlots()>0&&homePlot!=null){
    int destination=policy.trapPlot();
    if(!visited.contains(destination)&&visited.size()<policy.travelPlots()){
     var plot=PestPlot.of(destination);visited.add(destination);travel.reset();if(!travel.begin(handle,plot,now)){finish(State.FAILED,now);return true;}traveled=true;state=State.TRAVEL_OUT;return true;
    }
    if(visited.contains(destination)&&PestPlot.of(destination).contains(frame.position().x(),frame.position().z())&&now-phaseStart<5*SECOND)return true;
   }
   incomplete=true;beginReturn(now);return true;
  }
  if(handledTraps.size()>=24){incomplete=true;beginReturn(now);return true;}
  trapTarget=next.get();handledTraps.add(trapTarget.trapId());state=State.TRAP_APPROACH;phaseStart=now;port.release(handle);return true;
 }
 private boolean nextPlot(Frame frame,long now){
  if(policy.travelPlots()==0||homePlot==null||!frame.plotsKnown()||visited.size()>=policy.travelPlots())return false;
  var next=frame.plots().stream().filter(id->!visited.contains(id)).map(PestPlot::of).min(Comparator.comparingDouble(p->{double dx=p.centerX()-frame.position().x(),dz=p.centerZ()-frame.position().z();return dx*dx+dz*dz;}));
  if(next.isEmpty())return false;
  var plot=next.get();visited.add(plot.id());travel.reset();
  if(!travel.begin(handle,plot,now)){finish(State.FAILED,now);return true;}
  traveled=true;state=State.TRAVEL_OUT;return true;
 }
 private void beginReturn(long now){
  port.release(handle);returnGoal=null;
  if(!traveled){state=State.RETURNING;return;}
  travel.reset();if(homePlot==null||!travel.begin(handle,homePlot,now)){finish(State.FAILED,now);return;}state=State.TRAVEL_BACK;
 }
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){
  state=terminal;
  try{trap.cancel();port.release(handle);}catch(RuntimeException cleanup){state=State.FAILED;}
  finally{try{boolean completed=session.finish(handle,state==State.COMPLETE,now);if(state==State.COMPLETE&&!completed)state=State.FAILED;}finally{try{port.dispose(handle);}catch(RuntimeException cleanup){state=State.FAILED;}handle=null;target=null;travel.reset();}}
 }
}
