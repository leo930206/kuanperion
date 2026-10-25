package dev.kuan.kuanperion.automation.pest;
import java.util.*;
public final class PestTrapTargetTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var id=UUID.fromString("00000000-0000-0000-0000-000000000010");var p=new PestTargets.Point(1,72,3);
  var a=PestTrapTarget.read(10,id,"Pest Trap #1",p).orElseThrow();check(a.trapId()==1&&a.label().equals("Pest Trap #1"),"typed exact trap identity");
  check(PestTrapTarget.read(10,id,"Vermin Trap #2",p).isPresent()&&PestTrapTarget.read(10,id,"Mouse Trap #3",p).isPresent(),"three official trap families, not arbitrary trap substring");
  for(String bad:new String[]{"#1","Pest Trap #0","Pest Trap #25","Pest Trap #01","Quest Pest Trap #1","Pest Trap #1 extra","MouseTrap #2","trap 1"})check(PestTrapTarget.read(10,id,bad,p).isEmpty(),"unknown label rejected: "+bad);
  check(PestTrapTarget.unique(List.of(a,a)).isEmpty(),"duplicate trap ID ambiguous, not nearest wins");check(PestTrapTarget.unique(List.of(a)).equals(List.of(a)),"immutable unique snapshot");
  System.out.println("PestTrapTargetTest: PASS (bounded exact native marker families/IDs, entity identity, duplicate ambiguity)");
 }
}
