package dev.kuan.kuanperion.automation.pest;
import java.util.*;
public final class PestBarnTargetTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var desk=new PestTargets.Point(12,72,-10);var t=new PestBarnTarget(5,UUID.randomUUID(),new PestTargets.Point(13,73.6,-10));check(PestBarnTarget.select(List.of(t),desk).orElseThrow().equals(t),"one typed loaded NPC within explicit Barn desk region");
  check(PestBarnTarget.select(List.of(),desk).isEmpty()&&PestBarnTarget.select(List.of(t),null).isEmpty(),"no target/desk never inferred");
  check(PestBarnTarget.select(List.of(t,new PestBarnTarget(6,UUID.randomUUID(),t.aim())),desk).isEmpty(),"two eligible NPCs cannot be arbitrarily selected");
  check(PestBarnTarget.select(List.of(t,new PestBarnTarget(5,UUID.randomUUID(),new PestTargets.Point(-40,73,0))),desk).isEmpty(),"duplicate entity ID rejected even if other position outside desk region");
  check(PestBarnTarget.select(List.of(t,new PestBarnTarget(6,t.uuid(),new PestTargets.Point(-40,73,0))),desk).isEmpty(),"duplicate UUID is not independent identity");
  check(PestBarnTarget.select(List.of(new PestBarnTarget(7,UUID.randomUUID(),new PestTargets.Point(-40,73,0))),desk).isEmpty(),"never search/fly to guessed broad Garden NPC");
  System.out.println("PestBarnTargetTest: PASS typed local selection only, no native/current-server identity claim");
 }
}
