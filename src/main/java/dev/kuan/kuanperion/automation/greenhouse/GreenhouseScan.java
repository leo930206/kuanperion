// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.pest.PestPlot;import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import java.util.*;
/** Bounded discovery, never attack authority. Every queued target must be revalidated natively at use. */
public final class GreenhouseScan {
 public enum State {SCANNING,COMPLETE,UNKNOWN,LIMIT,CANCELLED,TIMED_OUT}
 public interface Source{long world();boolean loaded();boolean hasNext();Optional<GreenhouseTarget> next();}
 private final long world,started;private final Point center;private final int radius;private final Set<Integer> plots;private final Set<GreenhouseIdentity.Kind> kinds;private final Source source;
 private final List<GreenhouseTarget> targets=new ArrayList<>();private final Set<GreenhouseTarget.Ref> seen=new HashSet<>();private State state=State.SCANNING;private int queries;
 public GreenhouseScan(long world,Point center,int radius,Set<Integer> plots,Set<GreenhouseIdentity.Kind> kinds,Source source,long now){
  if(world<=0||radius<4||radius>25||plots.isEmpty()||plots.size()>24||plots.stream().anyMatch(id->id<1||id>24)||kinds.isEmpty())throw new IllegalArgumentException("Invalid greenhouse scan");
  this.world=world;this.center=Objects.requireNonNull(center);this.radius=radius;this.plots=Set.copyOf(plots);this.kinds=Set.copyOf(kinds);this.source=Objects.requireNonNull(source);started=now;
 }
 public State state(){return state;}public List<GreenhouseTarget> targets(){return List.copyOf(targets);}
 public void step(long now){
  if(state!=State.SCANNING)return;
  try{
   if(now-started<0||now-started>=10_000_000_000L){stop(State.TIMED_OUT);return;}
   if(source.world()!=world){stop(State.CANCELLED);return;}
   if(!source.loaded()){stop(State.UNKNOWN);return;}
   for(int budget=0;budget<64;budget++){
    if(!source.hasNext()){state=State.COMPLETE;return;}
    if(++queries>8192){stop(State.LIMIT);return;}var candidate=source.next();if(candidate.isEmpty())continue;var target=candidate.get();
    if(!seen.add(target.ref())){stop(State.UNKNOWN);return;}
    double dx=target.position().x()-center.x(),dz=target.position().z()-center.z(),dy=target.position().y()-center.y();
    if(!kinds.contains(target.kind())||dx*dx+dz*dz>radius*radius||Math.abs(dy)>10||PestPlot.at(target.position().x(),target.position().z()).filter(p->plots.contains(p.id())).isEmpty())continue;
    if(targets.size()>=1024){stop(State.LIMIT);return;}targets.add(target);
   }
   if(!source.hasNext())state=State.COMPLETE;
  }catch(RuntimeException malformed){stop(State.UNKNOWN);}
 }
 private void stop(State terminal){state=terminal;targets.clear();seen.clear();}
}
