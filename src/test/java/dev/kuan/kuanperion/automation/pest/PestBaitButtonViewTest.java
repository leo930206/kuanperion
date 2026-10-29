package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestBaitButtonViewTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
 static final PestTrapTarget TARGET=new PestTrapTarget(4,new UUID(0,1),1,"Mouse Trap",new PestTargets.Point(0,72,0));
 static ItemSnapshot role(String...lore){return new ItemSnapshot("","minecraft:paper","","Trap Bait","role"+Arrays.toString(lore),1,List.of(lore),false);}
 static MenuSnapshot menu(Map<Integer,ItemSnapshot> changes){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<63;i++)slots.add(new MenuSnapshot.Slot(i,changes.getOrDefault(i,AIR),i>=27));return new MenuSnapshot(1,7,1,"Mouse Trap",27,slots);}
 public static void main(String[]args){
  var empty=new PestTrapObservation(Set.of(),true,Set.of(1),true);var m=menu(Map.of(13,role("Click to insert bait!")));var r=PestBaitButtonView.initial(m,TARGET,empty).orElseThrow();check(r.slot()==13&&r.marker().count()==1,"original nonempty Trap Bait role, not a fabricated empty storage slot or capacity");check(PestBaitButtonView.initial(menu(Map.of()),TARGET,empty).isEmpty(),"an empty guessed slot is not the source role");
  for(String bad:List.of("Cost: 10 Coins","Use from Sacks","Click to withdraw bait!"))check(PestBaitButtonView.initial(menu(Map.of(13,role("Click to insert bait!",bad))),TARGET,empty).isEmpty(),"no extra spending/Sacks/withdraw behavior: "+bad);
  check(PestBaitButtonView.initial(menu(Map.of(13,m.item(13),14,m.item(13))),TARGET,empty).isEmpty(),"duplicate roles refused");check(PestBaitButtonView.initial(m,TARGET,new PestTrapObservation(Set.of(1),true,Set.of(1),true)).isEmpty(),"full trap is not a refill candidate");check(PestBaitButtonView.initial(m,TARGET,PestTrapObservation.unknown()).isEmpty(),"unknown is not empty");
  System.out.println("PestBaitButtonViewTest: PASS source-role candidate only, no click/capacity/runtime claim");
 }
}
