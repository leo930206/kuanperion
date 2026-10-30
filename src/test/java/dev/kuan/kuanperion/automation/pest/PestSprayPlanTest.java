package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestSprayPlanTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","air",0,List.of(),false);
 static ItemSnapshot material(int n){return new ItemSnapshot("TEST_MATERIAL","minecraft:paper","","Test Material","commodity",n,List.of("Commodity"),false);}
 static Map<Integer,ItemSnapshot> inventory(){var m=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)m.put(i,AIR);m.put(4,PestSprayToolTest.tool("SPRAYONATOR","Selected Material: Test Material"));m.put(9,material(12));return m;}
 static PestSprayPolicy policy(){return new PestSprayPolicy("TEST_MATERIAL","minecraft:paper","Test Material",5,true,Set.of());}
 public static void main(String[]args){
  var inv=inventory();var rules=policy();var p=PestSprayPlan.select(rules,inv).orElseThrow();check(p.tool().slot()==4&&p.reservedItems()==5&&p.baseline().equals(inv),"fixed worst-case nozzle ceiling; not a guessed exact cost");
  check(!PestSprayPolicy.defaults().configured()&&PestSprayPlan.select(PestSprayPolicy.defaults(),inv).isEmpty(),"no implicit material or Sacks grant");
  check(PestSprayPlan.select(new PestSprayPolicy(rules.materialId(),rules.minecraftId(),rules.materialName(),4,true,Set.of()),inv).isEmpty(),"below five cannot authorize unknown 1/3/5 nozzle");
  check(PestSprayPlan.select(new PestSprayPolicy(rules.materialId(),rules.minecraftId(),rules.materialName(),5,false,Set.of()),inv).isEmpty(),"must separately acknowledge server Sacks fallback");
  check(PestSprayPlan.select(new PestSprayPolicy(rules.materialId(),rules.minecraftId(),"Wrong Material",5,true,Set.of()),inv).isEmpty(),"selected label must exactly match explicit native material");
  for(int slot:List.of(0,8,36,40,42)){var m=new HashMap<>(inv);m.put(slot,material(1));check(PestSprayPlan.select(rules,m).isEmpty(),"same-ID protected hotbar/equipment blocks whole action "+slot);}
  for(var bad:List.of(new ItemSnapshot("TEST_MATERIAL","minecraft:paper","uuid","Test Material","commodity",1,List.of(),false),new ItemSnapshot("TEST_MATERIAL","minecraft:stone","","Test Material","commodity",1,List.of(),false),new ItemSnapshot("TEST_MATERIAL","minecraft:paper","","Test Material","different",1,List.of(),false),new ItemSnapshot("TEST_MATERIAL","minecraft:paper","","Test Material","commodity",1,List.of(),true))){var m=new HashMap<>(inv);m.put(10,bad);check(PestSprayPlan.select(rules,m).isEmpty(),"UUID/protection/native/component variants cannot be consumed");}
  var lower=new HashMap<>(inv);lower.put(4,PestSprayToolTest.tool("SPRAYONATOR","Selected Material: test material"));check(PestSprayPlan.select(rules,lower).isEmpty(),"case-folded display label cannot authorize a potentially different exact catalog identity");
  var m=new HashMap<>(inv);m.put(5,m.get(4));check(PestSprayPlan.select(rules,m).isEmpty(),"multiple matched tools ambiguous");m=new HashMap<>(inv);m.put(9,material(4));check(PestSprayPlan.select(rules,m).isEmpty(),"enough visible authorized inventory for worst case required; no Sacks-only blind use");m.remove(42);check(PestSprayPlan.select(rules,m).isEmpty(),"full43 required");
  var after=new HashMap<>(inv);after.put(9,material(9));var receipts=new InventoryReceipts();var stamp=receipts.stamp(1);check(!p.receipt(after,receipts,stamp),"client predicted count is not applied receipt");receipts.observe(1,0,9,material(9));check(p.receipt(after,receipts,stamp),"fresh applied authorized decrease bounded by five");
  after.put(9,material(6));receipts.observe(1,0,9,material(6));check(!p.receipt(after,receipts,stamp),"observed over-ceiling consumption cannot complete");after.put(9,material(12));check(!p.receipt(after,receipts,stamp),"chat/Sacks-only cannot claim inventory confirmation");after.put(9,material(9));after.put(42,material(1));check(!p.receipt(after,receipts,stamp),"unrelated protected slot change cannot complete");
  check(PestSprayObservation.parse(List.of("Pests:","Spray: None"),"Test Material").state()==PestSprayObservation.State.NONE,"source no spray within scoped field");
  check(PestSprayObservation.parse(List.of("Pests:","Spray: Test Material (29m 1s)"),"Test Material").state()==PestSprayObservation.State.ACTIVE,"exact material and bounded local timer grammar");
  for(var lines:List.of(List.of("Spray: None"),List.of("Pests:","Spray: Something"),List.of("Pests:","Spray: None","Spray: None"),List.of("Pests:","Spray: None","Spray: Test Material"),List.of("Pests:","Visitors:","Spray: None"),List.of("Pests:","Spray: not none"),List.of("Pests:","Spray: Test Material (999m)"),List.of("Pests:","Spray: Test Material (0m)"),List.of("Pests:","Spray: Test Material (0m 0s)")))check(PestSprayObservation.parse(lines,"Test Material").state()==PestSprayObservation.State.UNKNOWN,"missing/duplicate/conflicting/unknown field never zero or rearm: "+lines);
  System.out.println("PestSprayPlanTest: PASS bounded explicit inventory plan and scoped provisional observations; not native functionality");
 }
}
