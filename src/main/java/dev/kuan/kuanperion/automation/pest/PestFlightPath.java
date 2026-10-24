// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.*;

/** Bounded client-tick A*. A route proposal is NOT flight permission: recheck every executed edge. */
public final class PestFlightPath {
 public interface Space {boolean clear(PestTargets.Point from,PestTargets.Point to);}
 public enum State {SEARCHING,FOUND,NO_ROUTE,LIMIT,TIMED_OUT,CANCELLED,FAILED}
 private record Cell(int x,int y,int z){}
 private record Node(Cell cell,int cost,Node parent,double remaining){}
 private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
 private final long world,started;private final PestTargets.Point origin,goal;private final double tolerance;private final int radius,maxNodes;private final Space space;
 private final PriorityQueue<Node> open=new PriorityQueue<>(Comparator.<Node>comparingDouble(n->n.cost()+n.remaining()).thenComparingDouble(Node::remaining).thenComparingInt(n->n.cell().x()).thenComparingInt(n->n.cell().y()).thenComparingInt(n->n.cell().z()));
 private final Map<Cell,Integer> best=new HashMap<>();private final Set<Cell> closed=new HashSet<>();
 private State state=State.SEARCHING;private Node expanding;private int direction=-1;private List<PestTargets.Point> route=List.of();private long foundAt;
 public PestFlightPath(long world,PestTargets.Point start,PestTargets.Point goal,double tolerance,int radius,int maxNodes,Space space,long now){
  this.world=world;this.origin=Objects.requireNonNull(start);this.goal=Objects.requireNonNull(goal);this.tolerance=tolerance;this.radius=radius;this.maxNodes=maxNodes;this.space=Objects.requireNonNull(space);started=now;
  if(world<=0||!Double.isFinite(tolerance)||tolerance<.05||tolerance>16||radius<2||radius>96||maxNodes<16||maxNodes>4096||start.distanceSquared(goal)>Math.pow(radius+tolerance,2))throw new IllegalArgumentException("Invalid bounded flight goal");
  var cell=new Cell(0,0,0);best.put(cell,0);open.add(new Node(cell,0,null,remaining(start)));
 }
 public State state(){return state;}
 public Optional<List<PestTargets.Point>> route(long currentWorld,long now){long age=now-foundAt;return state==State.FOUND&&world==currentWorld&&age>=0&&age<=2_000_000_000L?Optional.of(route):Optional.empty();}
 public void step(long currentWorld,long now,int maxQueries){
  if(maxQueries<1||maxQueries>256)throw new IllegalArgumentException("Invalid query budget");if(state!=State.SEARCHING)return;
  if(currentWorld!=world||now-started<0){state=State.CANCELLED;return;}if(now-started>=5_000_000_000L){state=State.TIMED_OUT;return;}
  try{
   int queries=0,work=maxQueries*8;
   while(state==State.SEARCHING&&queries<maxQueries&&work-->0){
    if(expanding==null){
     Node next=open.poll();if(next==null){state=State.NO_ROUTE;return;}
     if(closed.contains(next.cell())||next.cost()!=best.getOrDefault(next.cell(),Integer.MAX_VALUE))continue;
     closed.add(next.cell());expanding=next;direction=-1;
    }
    var from=point(expanding.cell());
    if(from.distanceSquared(goal)<=tolerance*tolerance){found(expanding,false,now);return;}
    if(direction<0){direction=0;if(from.distanceSquared(goal)<=4){queries++;if(space.clear(from,goal)){found(expanding,true,now);return;}if(queries>=maxQueries)return;}}
    if(direction>=DIRECTIONS.length){expanding=null;continue;}
    var delta=DIRECTIONS[direction++];var cell=new Cell(expanding.cell().x()+delta[0],expanding.cell().y()+delta[1],expanding.cell().z()+delta[2]);
    if(cell.x()*cell.x()+cell.y()*cell.y()+cell.z()*cell.z()>radius*radius||closed.contains(cell)||best.getOrDefault(cell,Integer.MAX_VALUE)<=expanding.cost()+1)continue;
    if(best.size()>=maxNodes||open.size()>=maxNodes){state=State.LIMIT;return;}
    var to=point(cell);queries++;if(!space.clear(from,to))continue;
    var next=new Node(cell,expanding.cost()+1,expanding,remaining(to));best.put(cell,next.cost());open.add(next);
   }
  }catch(RuntimeException invalid){state=State.FAILED;}
 }
 private double remaining(PestTargets.Point point){return Math.max(0,Math.sqrt(point.distanceSquared(goal))-tolerance);}
 private PestTargets.Point point(Cell cell){return new PestTargets.Point(origin.x()+cell.x(),origin.y()+cell.y(),origin.z()+cell.z());}
 private void found(Node node,boolean exactGoal,long now){var points=new ArrayList<PestTargets.Point>();for(var current=node;current!=null;current=current.parent())points.add(point(current.cell()));Collections.reverse(points);if(exactGoal)points.add(goal);route=List.copyOf(points);foundAt=now;state=State.FOUND;}
}
