package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import java.util.*;
public final class GreenhouseScanTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}static final Point CENTER=new Point(0,75,-96);
 static GreenhouseTarget target(int id,double x,double y,double z,GreenhouseIdentity.Kind kind){return new GreenhouseTarget(new GreenhouseTarget.EntityRef(id,new UUID(0,id)),kind,new Point(x,y,z));}
 static final class Fake implements GreenhouseScan.Source {
  long world=1;boolean loaded=true;int index,calls;List<GreenhouseTarget> entries;
  Fake(List<GreenhouseTarget> entries){this.entries=entries;}public long world(){return world;}public boolean loaded(){return loaded;}public boolean hasNext(){return index<entries.size();}public Optional<GreenhouseTarget> next(){calls++;return Optional.ofNullable(entries.get(index++));}
 }
 static GreenhouseScan scan(Fake source){return new GreenhouseScan(1,CENTER,25,Set.of(1),Set.of(GreenhouseIdentity.Kind.ASHWREATH),source,0);}
 public static void main(String[]args){
  var entries=new ArrayList<GreenhouseTarget>();for(int i=0;i<200;i++)entries.add(target(i,1,75,-96,GreenhouseIdentity.Kind.ASHWREATH));var f=new Fake(entries);var scan=scan(f);scan.step(0);check(scan.state()==GreenhouseScan.State.SCANNING&&f.calls==64,"64 native candidates per tick, no full cube scan");for(int i=1;i<5;i++)scan.step(i*50_000_000L);check(scan.state()==GreenhouseScan.State.COMPLETE&&scan.targets().size()==200,"all bounded exact configured targets collected");
  f=new Fake(Arrays.asList(target(1,0,75,-96,GreenhouseIdentity.Kind.ASHWREATH),target(2,0,75,0,GreenhouseIdentity.Kind.ASHWREATH),target(3,26,75,-96,GreenhouseIdentity.Kind.ASHWREATH),target(4,0,86,-96,GreenhouseIdentity.Kind.ASHWREATH),target(5,0,75,-96,GreenhouseIdentity.Kind.GLASSCORN),null));scan=scan(f);scan.step(0);check(scan.state()==GreenhouseScan.State.COMPLETE&&scan.targets().size()==1,"explicit plot/type/horizontal/vertical scope, unknown candidates ignored not invented");
  f=new Fake(List.of());f.loaded=false;scan=scan(f);scan.step(0);check(scan.state()==GreenhouseScan.State.UNKNOWN&&f.calls==0,"unloaded chunk source is not a successfully empty greenhouse");
  f=new Fake(entries);scan=scan(f);scan.step(0);f.world=2;scan.step(50_000_000);check(scan.state()==GreenhouseScan.State.CANCELLED&&scan.targets().isEmpty(),"world change clears discovery evidence");
  f=new Fake(List.of(entries.get(0),entries.get(0)));scan=scan(f);scan.step(0);check(scan.state()==GreenhouseScan.State.UNKNOWN,"duplicate target identity ambiguous rather than first wins");
  f=new Fake(entries);scan=scan(f);scan.step(10_000_000_000L);check(scan.state()==GreenhouseScan.State.TIMED_OUT&&f.calls==0,"bounded scan lifetime, no work after deadline");
  var many=new ArrayList<GreenhouseTarget>();for(int i=0;i<1100;i++)many.add(target(i,0,75,-96,GreenhouseIdentity.Kind.ASHWREATH));f=new Fake(many);scan=scan(f);for(int i=0;i<20&&scan.state()==GreenhouseScan.State.SCANNING;i++)scan.step(i*50_000_000L);check(scan.state()==GreenhouseScan.State.LIMIT&&scan.targets().isEmpty(),"target bound is unknown/limit, never empty complete or unbounded memory");
  f=new Fake(Collections.nCopies(9000,null));scan=scan(f);for(int i=0;i<150&&scan.state()==GreenhouseScan.State.SCANNING;i++)scan.step(i*50_000_000L);check(scan.state()==GreenhouseScan.State.LIMIT&&f.calls==8192,"total native candidate budget prevents infinite/oversized source scan");
  f=new Fake(entries);scan=scan(f);scan.step(0);try{scan.targets().clear();throw new AssertionError("mutable discovery");}catch(UnsupportedOperationException expected){}
  System.out.println("GreenhouseScanTest: PASS (64-query ticks, exact configured plot/type/range, immutable discovery, unloaded/duplicate/world/deadline rejection)");
 }
}
