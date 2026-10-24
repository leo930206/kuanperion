// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
/** Bounded local cleanup and optional explicit search. A disappearance is not a kill: require a coupled server Tab decrease. */
public final class PestTask {
 public enum State {IDLE,PREPARING,SEEKING,SWEEPING,CAPTURING,RETURNING,RESTORING,COMPLETE,INCOMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public enum Motion {MOVING,ARRIVED,BLOCKED}
 public enum Capture {AIMING,USING,REJECTED}
 public record Anchor(PestTargets.Point position,float yaw,float pitch,int slot,ItemSnapshot item,boolean flying){}
 public record Frame(long world,long observedAt,PestTargets.Point position,PestTargets.Point eye,float yaw,float pitch,int slot,ItemSnapshot item,boolean mayFly,boolean flying,boolean manualInput,int alive,List<PestTargets.Target> targets,List<PestIdentity.Vacuum> tools,boolean complete){
  public Frame {Objects.requireNonNull(position);Objects.requireNonNull(eye);Objects.requireNonNull(item);targets=List.copyOf(targets);tools=List.copyOf(tools);}
  public boolean fresh(long world,long now){long age=now-observedAt;return complete&&this.world==world&&world>0&&age>=0&&age<=1_000_000_000L&&alive>=0&&alive<=1000&&slot>=0&&slot<9&&Float.isFinite(yaw)&&Float.isFinite(pitch)&&Math.abs(pitch)<=90&&targets.size()<=256&&tools.size()<=9;}
 }
 public interface Port {
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
 private PestPolicy policy;private Anchor anchor;private PestIdentity.Vacuum tool;private PestTargets.Target target;
 private long world,started,phaseStart;private int baseline,confirmed;private boolean used,incomplete,sweepArrived;private int sweepIndex;private long sweepArrival;
 private static final int[][] SWEEP={{1,1},{-1,1},{-1,-1},{1,-1}};
 public PestTask(AutomationSession session,Port port){this.session=session;this.port=port;}
 public State state(){return state;}public int confirmedDecreases(){return confirmed;}
 public boolean active(){return switch(state){case PREPARING,SEEKING,SWEEPING,CAPTURING,RETURNING,RESTORING->true;default->false;};}
 public boolean request(PestPolicy policy,boolean manual,long now){
  if(active())return false;
  try{
   var context=port.context();var f=port.frame(now);
   if(!context.canIssueCommand()||!context.garden()||!f.fresh(context.worldId(),now)||f.manualInput()||!f.mayFly()||f.alive()<policy.threshold()||f.targets().isEmpty()&&!policy.sweep()||!manual&&policy.onlyWhileFarming()&&!context.farmingActive())return false;
   var best=f.tools().stream().filter(t->PestIdentity.vacuum(t.slot(),t.item()).filter(v->v.equals(t)).isPresent()).max(Comparator.comparingDouble(PestIdentity.Vacuum::range).thenComparingInt(t->-t.slot())).orElse(null);
   if(best==null||!policy.sweep()&&f.targets().stream().noneMatch(t->f.position().distanceSquared(t.position())<=policy.radius()*policy.radius()))return false;
   var next=session.begin(AutomationFeature.PEST,now,policy.timeoutSeconds()*SECOND);if(next==null)return false;
   handle=next;this.policy=policy;tool=best;anchor=new Anchor(f.position(),f.yaw(),f.pitch(),f.slot(),f.item(),f.flying());world=context.worldId();started=phaseStart=now;confirmed=0;sweepIndex=0;incomplete=false;sweepArrived=false;used=false;target=null;state=State.PREPARING;return true;
  }catch(RuntimeException invalid){return false;}
 }
 public void tick(long now){
  if(!active())return;
  try{tickChecked(now);}catch(RuntimeException invalid){finish(State.FAILED,now);}
 }
 private void tickChecked(long now){
  var context=port.context();var f=port.frame(now);
  if(!session.isCurrent(handle)||!context.canIssueCommand()||!context.garden()||handle.hold()!=null&&!context.farmingActive()||context.worldId()!=world||!f.fresh(world,now)||f.manualInput()||!f.mayFly()){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=policy.timeoutSeconds()*SECOND){finish(State.TIMED_OUT,now);return;}
  if(f.position().distanceSquared(anchor.position())>Math.pow(policy.radius()+2,2)){finish(State.FAILED,now);return;}
  if(state==State.PREPARING){if(!port.prepare(handle,tool,anchor,policy.radius(),now)){finish(State.FAILED,now);return;}state=State.SEEKING;phaseStart=now;return;}
  if(f.slot()!=tool.slot()||!PestIdentity.sameVacuum(tool,f.item())){finish(State.CANCELLED,now);return;}
  if(state==State.RETURNING){
   port.release(handle);var motion=port.move(handle,anchor.position(),.35,now);
   if(motion==Motion.BLOCKED)finish(State.FAILED,now);else if(motion==Motion.ARRIVED){state=State.RESTORING;phaseStart=now;}return;
  }
  if(state==State.RESTORING){var motion=port.restore(handle,anchor,now);if(motion==Motion.BLOCKED)finish(State.FAILED,now);else if(motion==Motion.ARRIVED)finish(incomplete?State.INCOMPLETE:State.COMPLETE,now);return;}
  if(state==State.SWEEPING){
   if(f.targets().stream().anyMatch(t->anchor.position().distanceSquared(t.position())<=policy.radius()*policy.radius())){port.release(handle);state=State.SEEKING;phaseStart=now;return;}
   if(sweepArrived){if(now-sweepArrival>=SECOND){sweepIndex++;state=State.SEEKING;phaseStart=now;}return;}
   double offset=policy.radius()*.5;var direction=SWEEP[sweepIndex];var start=anchor.position();
   var goal=new PestTargets.Point(start.x()+direction[0]*offset,start.y()+Math.min(6,policy.radius()*.25),start.z()+direction[1]*offset);
   var motion=port.move(handle,goal,.6,now);if(motion==Motion.BLOCKED)finish(State.FAILED,now);else if(motion==Motion.ARRIVED){sweepArrived=true;sweepArrival=now;}return;
  }
  if(state==State.SEEKING){
   port.release(handle);
   if(confirmed>=policy.maxTargets()||f.alive()==0&&f.targets().isEmpty()){state=State.RETURNING;return;}
   if(f.alive()==0){if(now-phaseStart>=5*SECOND)finish(State.FAILED,now);return;}
   target=f.targets().stream().filter(t->anchor.position().distanceSquared(t.position())<=policy.radius()*policy.radius()).min(Comparator.comparingDouble(t->f.position().distanceSquared(t.position()))).orElse(null);
   if(target==null){
    if(policy.sweep()){if(sweepIndex<SWEEP.length){sweepArrived=false;state=State.SWEEPING;}else{incomplete=true;state=State.RETURNING;}}
    else if(now-phaseStart>=5*SECOND)finish(State.FAILED,now);return;
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
     if(anchor.position().distanceSquared(goal)>policy.radius()*policy.radius()||port.move(handle,goal,.6,now)==Motion.BLOCKED){finish(State.FAILED,now);return;}
    }else{
     var capture=port.capture(handle,target,tool,now);if(capture==Capture.REJECTED){finish(State.FAILED,now);return;}if(capture==Capture.USING){if(!used)baseline=f.alive();used=true;}
    }
   }
   if(now-phaseStart>=policy.captureSeconds()*SECOND)finish(State.TIMED_OUT,now);
  }
 }
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){
  state=terminal;
  try{port.release(handle);}catch(RuntimeException cleanup){state=State.FAILED;}
  finally{try{boolean completed=session.finish(handle,state==State.COMPLETE,now);if(state==State.COMPLETE&&!completed)state=State.FAILED;}finally{try{port.dispose(handle);}catch(RuntimeException cleanup){state=State.FAILED;}handle=null;target=null;}}
 }
}
