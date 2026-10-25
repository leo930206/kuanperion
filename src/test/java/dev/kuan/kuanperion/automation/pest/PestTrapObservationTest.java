package dev.kuan.kuanperion.automation.pest;
import java.util.*;
public final class PestTrapObservationTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var p=PestTrapObservation.parse(List.of("§aFull Traps: §e#1, #3","No Bait: #2"));check(p.fullKnown()&&p.full().equals(Set.of(1,3))&&p.emptyKnown()&&p.empty().equals(Set.of(2)),"exact native Tab headings and immutable typed IDs");
  p=PestTrapObservation.parse(List.of("Full Traps: None","No Bait: None"));check(p.fullKnown()&&p.full().isEmpty()&&p.emptyKnown()&&p.empty().isEmpty(),"explicit None is known empty");
  p=PestTrapObservation.parse(List.of("Full Traps: #1","No Bait: #1"));check(p.full().contains(1)&&p.empty().contains(1),"full and no bait may legitimately coexist");
  for(String bad:new String[]{"#0","#25","1","#01","#1, #1","#1, garbage","","#1 and #2","#1 #2","#1,","-1"}){p=PestTrapObservation.parse(List.of("Full Traps: "+bad,"No Bait: #2"));check(!p.fullKnown()&&p.full().isEmpty()&&p.emptyKnown(),"malformed field unknown without polluting independent field: "+bad);}
  p=PestTrapObservation.parse(List.of("Full Traps: #1","Full Traps: #2","No Bait: #3"));check(!p.fullKnown()&&p.emptyKnown(),"conflicting repeated headings unknown, not union");
  p=PestTrapObservation.parse(List.of("[player] Full Traps: #1","Quest: No Bait: #2"));check(!p.fullKnown()&&!p.emptyKnown(),"substring/player/quest mention never authorizes work");
  p=PestTrapObservation.parse(Collections.nCopies(129,"Full Traps: #1"));check(!p.fullKnown(),"bounded input count");
  var raw=new ArrayList<String>();raw.add("No Bait: #1");p=PestTrapObservation.parse(raw);raw.clear();check(p.empty().equals(Set.of(1)),"immutable copy");
  try{p.empty().add(2);throw new AssertionError("mutable fact");}catch(UnsupportedOperationException expected){}
  System.out.println("PestTrapObservationTest: PASS (strict bounded native headings/IDs, independent full/no-bait, unknown/None/conflict/spoof separation, immutable facts)");
 }
}
