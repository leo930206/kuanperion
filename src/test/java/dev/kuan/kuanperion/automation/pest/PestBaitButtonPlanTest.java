package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestBaitButtonPlanTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}static ItemSnapshot bait(int n){return new ItemSnapshot("TASTY_CHEESE","minecraft:paper","","Tasty Cheese","native-bait",n,List.of(),false);}
 static Map<Integer,ItemSnapshot> inventory(int n){var m=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)m.put(i,PestBaitButtonViewTest.AIR);m.put(9,bait(n));return m;}
 public static void main(String[]args){
  var rules=new PestBaitButtonPlan.Rules("TASTY_CHEESE",16,64,Set.of());var inv=inventory(16);var p=PestBaitButtonPlan.select(rules,inv,64).orElseThrow();check(p.source()==9&&p.maximumConsumption()==16,"whole actual source stack bounds irreversible consumption; not guessed server capacity");check(PestBaitButtonPlan.select(rules,inventory(17),64).isEmpty(),"left click cannot request only16 from a17-stack");check(PestBaitButtonPlan.select(rules,inv,15).isEmpty(),"remaining run ceiling covers full possible source consumption");
  inv.put(9,new ItemSnapshot("TASTY_CHEESE","minecraft:paper","uuid","Cheese","protected",1,List.of(),true));check(PestBaitButtonPlan.select(rules,inv,64).isEmpty(),"protection/individual identity overrides permitted material");inv=inventory(16);inv.put(0,inv.remove(9));inv.put(9,PestBaitButtonViewTest.AIR);check(PestBaitButtonPlan.select(rules,inv,64).isEmpty(),"hotbar bait untouched");
  inv=inventory(16);p=PestBaitButtonPlan.select(rules,inv,64).orElseThrow();inv.put(42,bait(1));check(!p.stillMatches(inv)&&p.baseline().get(42).count()==0,"full43 immutable baseline includes equipment");check(PestBaitButtonPlan.select(PestBaitButtonPlan.Rules.defaults(),inventory(16),0).isEmpty(),"default no authority");
  System.out.println("PestBaitButtonPlanTest: PASS explicit whole-source and run ceilings, no native capacity/action/runtime claim");
 }
}
