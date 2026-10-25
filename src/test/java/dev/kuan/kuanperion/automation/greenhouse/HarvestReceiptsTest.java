package dev.kuan.kuanperion.automation.greenhouse;
import java.util.*;
public final class HarvestReceiptsTest {
 static final long S=1_000_000_000L;static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var r=new HarvestReceipts();var entity=new GreenhouseTarget.EntityRef(10,new UUID(0,10));var block=new GreenhouseTarget.BlockRef(5,75,-96);var stamp=r.watch(1,entity,0);
  check(!r.after(stamp,S),"local predicted target disappearance never sufficient");r.entityChanged(1,11,S);check(!r.after(stamp,S),"unrelated entity packet not proof");r.entityChanged(1,10,2*S);check(r.after(stamp,2*S),"targeted fresh server update pairs with later native target-change check");
  check(!r.after(stamp,5*S),"stale target event not reusable");var previous=stamp;stamp=r.watch(1,entity,6*S);check(!r.after(previous,6*S)&&!r.after(stamp,6*S),"watch generation invalidates previous operation and preexisting evidence");
  r.blockChanged(1,5,75,-96,7*S);check(!r.after(stamp,7*S),"block event cannot acknowledge entity harvest");
  stamp=r.watch(1,block,8*S);r.blockChanged(1,5,75,-95,9*S);check(!r.after(stamp,9*S),"exact position, no nearby-block inference");r.blockChanged(1,5,75,-96,10*S);check(r.after(stamp,10*S),"targeted server block update");
  previous=stamp;r.clear();r.blockChanged(1,5,75,-96,11*S);check(!r.after(previous,11*S)&&r.watched().isEmpty(),"cancel clears watch/evidence, no replay across task");
  stamp=r.watch(1,block,12*S);r.blockChanged(2,5,75,-96,13*S);check(!r.after(stamp,13*S),"world mismatch invalidates proof");
  stamp=r.watch(2,block,14*S);r.blockChanged(2,5,75,-96,15*S);check(!r.after(stamp,14*S),"future event cannot authorize earlier tick");
  System.out.println("HarvestReceiptsTest: PASS (one target watch, exact world/entity/block, new generation and bounded freshness, no prediction/unrelated/future/stale/replay proof)");
 }
}
