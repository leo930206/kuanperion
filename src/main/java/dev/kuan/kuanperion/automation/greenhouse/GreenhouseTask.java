// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
/** One configured Greenhouse run under a revocable hold; no shared global macro state. */
public final class GreenhouseTask {
 public enum State {IDLE,PREPARING,TRAVEL_OUT,SCANNING,SEEKING,APPROACHING,HARVESTING,WAITING_HARVEST,TRAVEL_BACK,RETURNING,RESTORING,COMPLETE,PARTIAL,CANCELLED,TIMED_OUT,FAILED}
 public enum TargetState {MATURE,CHANGED,UNKNOWN}
 public enum Attack {AIMING,ISSUED,REJECTED}
 public record TargetStatus(TargetState state,GreenhouseTarget current){public TargetStatus{Objects.requireNonNull(state);if(state==TargetState.MATURE)Objects.requireNonNull(current);}}
 public record Tool(int slot,ItemSnapshot item){public Tool{if(slot<0||slot>8)throw new IllegalArgumentException("Invalid hotbar slot");Objects.requireNonNull(item);}}
 public record Frame(long world,long observedAt,PestTargets.Point position,PestTargets.Point eye,float yaw,float pitch,int slot,ItemSnapshot item,boolean mayFly,boolean flying,boolean manualInput,List<Tool> tools,boolean complete){
  public Frame{Objects.requireNonNull(position);Objects.requireNonNull(eye);Objects.requireNonNull(item);tools=List.copyOf(tools);}
  boolean fresh(long world,long now){return complete&&this.world==world&&world>0&&now-observedAt>=0&&now-observedAt<=1_000_000_000L&&slot>=0&&slot<9&&tools.size()<=9&&tools.stream().map(Tool::slot).distinct().count()==tools.size()&&Float.isFinite(yaw)&&Float.isFinite(pitch)&&Math.abs(pitch)<=90;}
 }
 public interface Port extends PestPlotTravel.Port {
  Frame frame(long now);boolean prepare(AutomationSession.Handle handle,Tool tool,FlightAnchor region,int radius,long now);
  Optional<GreenhouseScan> scan(GreenhousePolicy policy,int plot,PestTargets.Point center,long now);
  TargetStatus target(GreenhouseTarget expected,long now);
  FlightMotion move(AutomationSession.Handle handle,PestTargets.Point goal,double tolerance,long now);
  Attack harvest(AutomationSession.Handle handle,GreenhouseTarget target,Tool tool,long now);
  FlightMotion restore(AutomationSession.Handle handle,FlightAnchor original,long now);
  default void dispose(AutomationSession.Handle handle){release(handle);}
  /** Failure-only physical input reconciliation; never sends gameplay or re-arms Farming. */
  default void afterStop(AutomationSession.Handle handle){}
 }
 private static final long SECOND=1_000_000_000L;
 private final AutomationSession session;private final HarvestReceipts harvests;private final Port port;private final PestPlotTravel travel;
 private State state=State.IDLE;private AutomationSession.Handle handle;private GreenhousePolicy policy;private FlightAnchor original,region;private PestPlot home;private Tool tool;private List<Integer> plots=List.of();private int plotIndex,currentPlot,confirmed,returnLegs;private long world,started,phaseStart,lastScanTry;private boolean partial,triedScan;private GreenhouseScan scanner;private GreenhouseTarget target;private final Deque<GreenhouseTarget> queue=new ArrayDeque<>();private HarvestReceipts.Stamp stamp;private PestTargets.Point returnGoal;
 public GreenhouseTask(AutomationSession session,PlayerPositionReceipts positions,HarvestReceipts harvests,Port port){this.session=session;this.harvests=harvests;this.port=port;travel=new PestPlotTravel(session,positions,port,AutomationFeature.GREENHOUSE);}
 public State state(){return state;}public int confirmed(){return confirmed;}public boolean active(){return switch(state){case PREPARING,TRAVEL_OUT,SCANNING,SEEKING,APPROACHING,HARVESTING,WAITING_HARVEST,TRAVEL_BACK,RETURNING,RESTORING->true;default->false;};}
 public boolean request(GreenhousePolicy policy,boolean manual,long now){
  if(active()||policy==null||!policy.configured())return false;
  try{
   var context=port.context();var f=port.frame(now);
   if(!context.canIssueCommand()||!context.garden()||!f.fresh(context.worldId(),now)||f.manualInput()||!f.mayFly()||!manual&&policy.onlyWhileFarming()&&!context.farmingActive())return false;
   var choices=f.tools().stream().filter(t->policy.matchesTool(t.item())).toList();var home=PestPlot.at(f.position().x(),f.position().z());if(choices.size()!=1||home.isEmpty())return false;
   var anchor=new FlightAnchor(f.position(),f.yaw(),f.pitch(),f.slot(),f.item(),f.flying());var next=session.begin(AutomationFeature.GREENHOUSE,now,policy.timeoutSeconds()*SECOND);if(next==null)return false;
   this.handle=next;this.policy=policy;this.original=anchor;this.region=anchor;this.home=home.get();this.tool=choices.getFirst();this.plots=policy.plots().stream().sorted().toList();plotIndex=0;confirmed=returnLegs=0;partial=triedScan=false;scanner=null;target=null;returnGoal=null;queue.clear();world=context.worldId();started=phaseStart=now;travel.reset();harvests.clear();state=State.PREPARING;return true;
  }catch(RuntimeException invalid){if(handle!=null)finish(State.FAILED,now);return false;}
 }
 public void tick(long now){if(!active())return;try{run(now);}catch(RuntimeException failed){finish(State.FAILED,now);}}
 private void run(long now){
  var context=port.context();var f=port.frame(now);
  if(!session.isCurrent(handle)||!context.canIssueCommand()||!context.garden()||context.worldId()!=world||handle.hold()!=null&&!context.farmingActive()||!f.fresh(world,now)||f.manualInput()||!f.mayFly()){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=policy.timeoutSeconds()*SECOND){finish(State.TIMED_OUT,now);return;}
  if(state!=State.PREPARING&&(f.slot()!=tool.slot()||!PestIdentity.sameTool(tool.item(),f.item()))){finish(State.CANCELLED,now);return;}
  if(state==State.PREPARING){
   if(!port.prepare(handle,tool,region,64,now)){finish(State.FAILED,now);return;}
   nextPlot(now);return;
  }
  if(state==State.TRAVEL_OUT||state==State.TRAVEL_BACK){
   travel.tick(now);if(travel.arrival(handle,now).isPresent()){
    region=new FlightAnchor(f.position(),f.yaw(),f.pitch(),f.slot(),f.item(),f.flying());if(!port.prepare(handle,tool,region,64,now)){finish(State.FAILED,now);return;}boolean returning=state==State.TRAVEL_BACK;travel.reset();state=returning?State.RETURNING:State.SCANNING;scanner=null;triedScan=false;phaseStart=now;return;
   }
   switch(travel.state()){case TIMED_OUT->finish(State.TIMED_OUT,now);case CANCELLED->finish(State.CANCELLED,now);case FAILED->finish(State.FAILED,now);default->{}}return;
  }
  if(f.position().distanceSquared(region.position())>66*66){finish(State.FAILED,now);return;}
  if(state!=State.RETURNING&&state!=State.RESTORING&&state!=State.WAITING_HARVEST&&now-started>=(policy.timeoutSeconds()-45)*SECOND){partial=true;beginReturn(now);return;}
  if(state==State.RETURNING){
   port.release(handle);if(returnGoal==null){var a=original.position();var p=f.position();double distance=Math.sqrt(p.distanceSquared(a)),fraction=Math.min(1,41.6/Math.max(.001,distance));returnGoal=fraction==1?a:new PestTargets.Point(p.x()+(a.x()-p.x())*fraction,p.y()+(a.y()-p.y())*fraction,p.z()+(a.z()-p.z())*fraction);}
   var motion=port.move(handle,returnGoal,.35,now);if(motion==FlightMotion.BLOCKED){finish(State.FAILED,now);return;}
   if(motion==FlightMotion.ARRIVED){if(returnGoal.equals(original.position())){state=State.RESTORING;phaseStart=now;}else if(++returnLegs>16)finish(State.FAILED,now);else{region=new FlightAnchor(returnGoal,f.yaw(),f.pitch(),f.slot(),f.item(),f.flying());returnGoal=null;if(!port.prepare(handle,tool,region,64,now))finish(State.FAILED,now);}}return;
  }
  if(state==State.RESTORING){var motion=port.restore(handle,original,now);if(motion==FlightMotion.BLOCKED)finish(State.FAILED,now);else if(motion==FlightMotion.ARRIVED)finish(partial?State.PARTIAL:State.COMPLETE,now);return;}
  if(state==State.SCANNING){
   port.release(handle);
   if(scanner==null){if(!triedScan||now-lastScanTry>=250_000_000L){triedScan=true;lastScanTry=now;scanner=port.scan(policy,currentPlot,region.position(),now).orElse(null);}if(scanner==null){if(now-phaseStart>=10*SECOND)finish(State.TIMED_OUT,now);return;}}
   scanner.step(now);if(scanner.state()==GreenhouseScan.State.SCANNING)return;
   if(scanner.state()!=GreenhouseScan.State.COMPLETE){finish(State.FAILED,now);return;}
   scanner.targets().stream().sorted(Comparator.comparingDouble(t->f.position().distanceSquared(t.position()))).forEach(queue::addLast);scanner=null;state=State.SEEKING;phaseStart=now;return;
  }
  if(state==State.SEEKING){
   port.release(handle);if(confirmed>=policy.maxTargets()){partial=!queue.isEmpty()||plotIndex<plots.size();beginReturn(now);return;}
   if(queue.isEmpty()){if(plotIndex<plots.size())nextPlot(now);else beginReturn(now);return;}
   target=queue.removeFirst();var status=port.target(target,now);
   if(status.state()==TargetState.CHANGED){skip(now);return;}if(status.state()!=TargetState.MATURE||!inScope(status.current())){finish(State.FAILED,now);return;}
   target=status.current();state=State.APPROACHING;phaseStart=now;return;
  }
  if(state==State.WAITING_HARVEST){
   port.release(handle);var status=port.target(target,now);
   if(status.state()==TargetState.CHANGED&&harvests.after(stamp,now)){confirmed++;skip(now);return;}
   if(now-phaseStart>=5*SECOND)finish(State.TIMED_OUT,now);return;
  }
  var status=port.target(target,now);if(status.state()==TargetState.CHANGED){skip(now);return;}if(status.state()!=TargetState.MATURE||!inScope(status.current())){finish(State.FAILED,now);return;}target=status.current();
  double distance=Math.sqrt(f.eye().distanceSquared(target.position()));
  if(state==State.APPROACHING){
   if(distance<=2.5){port.release(handle);state=State.HARVESTING;phaseStart=now;return;}
   double ratio=1-2.25/distance;var p=f.position();var eye=f.eye();var aim=target.position();var goal=new PestTargets.Point(p.x()+(aim.x()-eye.x())*ratio,p.y()+(aim.y()-eye.y())*ratio,p.z()+(aim.z()-eye.z())*ratio);
   if(port.move(handle,goal,.4,now)==FlightMotion.BLOCKED){finish(State.FAILED,now);return;}if(now-phaseStart>=15*SECOND)finish(State.TIMED_OUT,now);return;
  }
  if(state==State.HARVESTING){
   port.release(handle);if(distance>2.75){state=State.APPROACHING;phaseStart=now;return;}
   stamp=harvests.watch(world,target.ref(),now);var result=port.harvest(handle,target,tool,now);
   if(result==Attack.REJECTED){finish(State.FAILED,now);return;}if(result==Attack.ISSUED){state=State.WAITING_HARVEST;phaseStart=now;}else if(now-phaseStart>=5*SECOND)finish(State.TIMED_OUT,now);
  }
 }
 private boolean inScope(GreenhouseTarget current){
  if(current==null||!target.ref().equals(current.ref())||target.kind()!=current.kind()||!policy.kinds().contains(current.kind())||!PestPlot.of(currentPlot).contains(current.position().x(),current.position().z()))return false;
  double dx=current.position().x()-region.position().x(),dz=current.position().z()-region.position().z();return dx*dx+dz*dz<=policy.radius()*policy.radius()&&Math.abs(current.position().y()-region.position().y())<=10;
 }
 private void nextPlot(long now){
  harvests.clear();queue.clear();target=null;scanner=null;currentPlot=plots.get(plotIndex++);travel.reset();
  if(!travel.begin(handle,PestPlot.of(currentPlot),now)){finish(State.FAILED,now);return;}state=State.TRAVEL_OUT;phaseStart=now;
 }
 private void beginReturn(long now){
  harvests.clear();port.release(handle);target=null;scanner=null;queue.clear();returnGoal=null;travel.reset();
  if(!travel.begin(handle,home,now)){finish(State.FAILED,now);return;}state=State.TRAVEL_BACK;phaseStart=now;
 }
 private void skip(long now){harvests.clear();target=null;stamp=null;state=State.SEEKING;phaseStart=now;}
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){
  var done=handle;handle=null;state=terminal;harvests.clear();travel.reset();
  var result=AutomationCompletion.finish(session,done,(terminal==State.COMPLETE||terminal==State.PARTIAL),now,()->{port.release(done);},()->port.dispose(done),()->port.afterStop(done));
  if(result.failed())state=State.FAILED;
 }
}
