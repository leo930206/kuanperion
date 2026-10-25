// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
/** One typed travel command. Local coordinates are never a substitute for a server receipt. */
public final class PestPlotTravel {
 public enum State {IDLE,WAITING,ARRIVED,TIMED_OUT,CANCELLED,FAILED}
 public interface Port {
  AutomationContext context(); PestTargets.Point position();
  boolean travel(AutomationSession.Handle handle,PestPlot plot,long now);
  void release(AutomationSession.Handle handle);
 }
 private static final long TIMEOUT=15_000_000_000L;
 private final AutomationSession session;private final PlayerPositionReceipts receipts;private final Port port;
 private State state=State.IDLE;private AutomationSession.Handle handle;private PestPlot plot;private PlayerPositionReceipts.Stamp stamp;private long started;
 public PestPlotTravel(AutomationSession session,PlayerPositionReceipts receipts,Port port){this.session=Objects.requireNonNull(session);this.receipts=Objects.requireNonNull(receipts);this.port=Objects.requireNonNull(port);}
 public State state(){return state;}
 public boolean begin(AutomationSession.Handle handle,PestPlot plot,long now){
  if(state!=State.IDLE)return false;
  this.handle=handle;this.plot=Objects.requireNonNull(plot);started=now;
  if(!allowed(now)){state=State.CANCELLED;return false;}
  stamp=receipts.stamp(handle.lease().worldId(),now);
  try{port.release(handle);if(!allowed(now)||!port.travel(handle,plot,now)){state=State.FAILED;return false;}}
  catch(RuntimeException failure){state=State.FAILED;return false;}
  state=State.WAITING;return true;
 }
 private boolean allowed(long now){
  var context=port.context();return session.isCurrent(handle)&&handle.lease().owner()==AutomationFeature.PEST&&handle.lease().worldId()==context.worldId()&&now>=started&&now-handle.lease().started()<handle.lease().lifetime()&&context.garden()&&context.canIssueCommand();
 }
 public void tick(long now){
  if(state!=State.WAITING&&state!=State.ARRIVED)return;
  if(!allowed(now)){state=State.CANCELLED;return;}
  if(now-started>=TIMEOUT){state=State.TIMED_OUT;return;}
  state=evidence(now).isPresent()?State.ARRIVED:State.WAITING;
 }
 private Optional<PlayerPositionReceipts.Receipt> evidence(long now){
  if(!allowed(now)||now-started>=TIMEOUT)return Optional.empty();
  var current=port.position();if(current==null)return Optional.empty();
  return receipts.since(stamp,now,TIMEOUT).filter(r->{var p=r.position();double dx=p.x()-current.x(),dy=p.y()-current.y(),dz=p.z()-current.z();return plot.contains(p.x(),p.z())&&dx*dx+dy*dy+dz*dz<=.25;});
 }
 public Optional<PlayerPositionReceipts.Receipt> arrival(AutomationSession.Handle handle,long now){return state==State.ARRIVED&&Objects.equals(this.handle,handle)?evidence(now):Optional.empty();}
 public void reset(){state=State.IDLE;handle=null;plot=null;stamp=null;}
}
