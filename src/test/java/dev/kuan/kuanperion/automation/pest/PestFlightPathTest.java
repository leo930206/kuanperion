package dev.kuan.kuanperion.automation.pest;
import java.util.*;
public final class PestFlightPathTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final class Grid implements PestFlightPath.Space {
  int checks;boolean unknown,throwsError;Set<String> blocked=new HashSet<>();
  public boolean clear(PestTargets.Point from,PestTargets.Point to){checks++;if(throwsError)throw new IllegalStateException();return !unknown&&!blocked.contains(Math.round(to.x())+","+Math.round(to.y())+","+Math.round(to.z()));}
 }
 static PestTargets.Point p(double x,double y,double z){return new PestTargets.Point(x,y,z);}
 static void finish(PestFlightPath path,long world){for(int i=0;i<200&&path.state()==PestFlightPath.State.SEARCHING;i++)path.step(world,i*1000000L,24);}
 public static void main(String[] args){
  var g=new Grid();var path=new PestFlightPath(1,p(0,70,0),p(12,70,0),.4,32,2048,g,0);path.step(1,0,24);check(g.checks<=24,"strict per-step collision query budget");finish(path,1);check(path.state()==PestFlightPath.State.FOUND,"bounded clear route found");var points=path.route(1,200000000L).orElseThrow();check(points.get(0).equals(p(0,70,0))&&points.get(points.size()-1).distanceSquared(p(12,70,0))<=.16,"route includes exact start and reaches goal radius");
  check(path.route(2,200000000L).isEmpty()&&path.route(1,6000000000L).isEmpty(),"old-world/stale route unavailable");
  g=new Grid();g.blocked.add("1,70,0");path=new PestFlightPath(1,p(0,70,0),p(3,70,0),.1,8,2048,g,0);finish(path,1);check(path.state()==PestFlightPath.State.FOUND&&path.route(1,200000000L).orElseThrow().stream().noneMatch(v->v.equals(p(1,70,0))),"obstacle detour, no diagonal corner cut");
  g=new Grid();path=new PestFlightPath(1,p(.2,70.1,.4),p(3.75,70.4,.95),.1,8,2048,g,0);finish(path,1);check(path.state()==PestFlightPath.State.FOUND&&path.route(1,200000000L).orElseThrow().getLast().equals(p(3.75,70.4,.95)),"exact fractional return anchor, not forced grid rounding");
  g=new Grid();g.unknown=true;path=new PestFlightPath(1,p(0,70,0),p(5,70,0),.1,8,2048,g,0);finish(path,1);check(path.state()==PestFlightPath.State.NO_ROUTE,"unknown/unloaded cells are blocked, not guessed air");
  g=new Grid();path=new PestFlightPath(1,p(0,70,0),p(5,70,0),.1,8,2048,g,0);path.step(2,0,24);check(path.state()==PestFlightPath.State.CANCELLED&&g.checks==0,"world change cancels before collision reads");
  g=new Grid();path=new PestFlightPath(1,p(0,70,0),p(5,70,0),.1,8,2048,g,0);path.step(1,5000000000L,24);check(path.state()==PestFlightPath.State.TIMED_OUT&&g.checks==0,"bounded planning deadline");
  g=new Grid();g.throwsError=true;path=new PestFlightPath(1,p(0,70,0),p(5,70,0),.1,8,2048,g,0);path.step(1,0,24);check(path.state()==PestFlightPath.State.FAILED,"collision observation errors fail closed");
  g=new Grid();path=new PestFlightPath(1,p(0,70,0),p(20,70,0),.1,32,16,g,0);finish(path,1);check(path.state()==PestFlightPath.State.LIMIT,"node/memory cap cannot expand forever");
  var fence=new PestFlightBounds(new PestTargets.Point(.5,70,.5),16);
  check(fence.permits(new PestTargets.Point(.5,70,.5),new PestTargets.Point(16.5,70,.5)),"inclusive explicit flight radius");
  check(!fence.permits(new PestTargets.Point(.5,70,.5),new PestTargets.Point(16.51,70,.5))&&!fence.permits(new PestTargets.Point(.5,86.01,.5),new PestTargets.Point(.5,70,.5)),"planning/execution fence must include full start and end in 3D");
  System.out.println("PestFlightPathTest: PASS (bounded queries/nodes/deadline, obstacle routing, fractional anchors, no unknown-air/world/stale/error bypass)");
 }
}
