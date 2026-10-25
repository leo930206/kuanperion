package dev.kuan.kuanperion.automation.supercraft;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class SupercraftPlanTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","",0,List.of(),false);
 static ItemSnapshot item(String id,int count,boolean locked,String uuid){return new ItemSnapshot(id,"minecraft:wheat",uuid,id,id,count,List.of(),locked);}
 static Map<Integer,ItemSnapshot> inventory(){var result=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)result.put(i,AIR);for(int i=9;i<14;i++)result.put(i,item("WHEAT",64,false,""));return result;}
 static final SupercraftPlan.Recipe RECIPE=new SupercraftPlan.Recipe("ENCHANTED_WHEAT",1,64,Map.of("WHEAT",160));
 static final SupercraftPlan.Rules RULES=new SupercraftPlan.Rules(Set.of("ENCHANTED_WHEAT"),Set.of("WHEAT"),64,512,64,Set.of());
 static void rejected(Runnable r){try{r.run();throw new AssertionError("expected strict validation");}catch(IllegalArgumentException expected){}}
 public static void main(String[]args){
  var inv=inventory();check(SupercraftPlan.select(RECIPE,2,SupercraftPlan.Rules.defaults(),inv).isEmpty(),"no configured output/ingredient/quantity authority");
  var plan=SupercraftPlan.select(RECIPE,2,RULES,inv).orElseThrow();check(plan.crafts()==2&&plan.outputCount()==2&&plan.inputs().equals(Map.of("WHEAT",320)),"explicit quantity, exact per-craft input multiplication; never maximize blindly");check(SupercraftPlan.stillMatches(plan,inv),"fresh exact baseline");
  inv.put(9,AIR);check(!SupercraftPlan.stillMatches(plan,inv)&&plan.baseline().get(9).count()==64,"immutable plan and any inventory change revokes preflight");
  for(int slot:List.of(0,8,36,40,41,42)){var bad=inventory();bad.put(slot,item("WHEAT",1,false,""));check(SupercraftPlan.select(RECIPE,1,RULES,bad).isEmpty(),"server consumption order cannot hide same-ID protected position: "+slot);}
  for(String uuid:List.of("","valuable")){var bad=inventory();bad.put(9,item("WHEAT",64,uuid.isEmpty(),uuid));check(SupercraftPlan.select(RECIPE,1,RULES,bad).isEmpty(),"locked or unique ingredients veto whole craft");}
  check(SupercraftPlan.select(RECIPE,3,RULES,inventory()).isEmpty(),"missing ingredients not invented or bought");
  check(SupercraftPlan.select(RECIPE,0,RULES,inventory()).isEmpty()&&SupercraftPlan.select(RECIPE,65,RULES,inventory()).isEmpty(),"zero/over-max output denied");
  var multiple=new SupercraftPlan.Recipe("ENCHANTED_WHEAT",16,64,Map.of("WHEAT",2));check(SupercraftPlan.select(multiple,17,RULES,inventory()).isEmpty(),"never round up desired amount and consume unexpected extra recipe");check(SupercraftPlan.select(multiple,32,RULES,inventory()).orElseThrow().crafts()==2,"exact output batches");
  var noInputs=new SupercraftPlan.Rules(Set.of("ENCHANTED_WHEAT"),Set.of(),64,512,64,Set.of());check(SupercraftPlan.select(RECIPE,1,noInputs,inventory()).isEmpty(),"output permission does not grant ingredient disposal authority");
  var consumeCap=new SupercraftPlan.Rules(RULES.outputs(),RULES.ingredients(),64,159,64,Set.of());check(SupercraftPlan.select(RECIPE,1,consumeCap,inventory()).isEmpty(),"total consumed input cap");
  var craftCap=new SupercraftPlan.Rules(RULES.outputs(),RULES.ingredients(),64,512,1,Set.of());check(SupercraftPlan.select(RECIPE,2,craftCap,inventory()).isEmpty(),"craft cap independent of output cap");
  var full=inventory();for(int i=14;i<36;i++)full.put(i,item("STONE",64,false,""));check(SupercraftPlan.select(RECIPE,1,RULES,full).isEmpty(),"no output capacity, never assume future slot movement or cursor repair");full.put(14,item("ENCHANTED_WHEAT",63,false,""));check(SupercraftPlan.select(RECIPE,1,RULES,full).isPresent()&&SupercraftPlan.select(RECIPE,2,RULES,full).isEmpty(),"exact existing output capacity, not generic free slot count");
  var incomplete=inventory();incomplete.remove(42);check(SupercraftPlan.select(RECIPE,1,RULES,incomplete).isEmpty(),"all43 inventory positions required");
  var malformed=inventory();malformed.put(20,item("STONE",65,false,""));check(SupercraftPlan.select(RECIPE,1,RULES,malformed).isEmpty(),"invalid native stack projection not silently ignored");
  rejected(()->new SupercraftPlan.Recipe("WHEAT",1,64,Map.of("WHEAT",160)));rejected(()->new SupercraftPlan.Recipe("ENCHANTED_WHEAT",0,64,Map.of("WHEAT",160)));rejected(()->new SupercraftPlan.Recipe("ENCHANTED_WHEAT",1,65,Map.of("WHEAT",160)));rejected(()->new SupercraftPlan.Recipe("ENCHANTED_WHEAT",1,64,Map.of("wheat",160)));rejected(()->new SupercraftPlan.Recipe("ENCHANTED_WHEAT",1,64,Map.of("WHEAT",0)));
  var original=inventory();var frozen=Map.copyOf(original);check(SupercraftPlan.select(RECIPE,2,RULES,original).isPresent()&&original.equals(frozen),"read-only planner never mutates inventory");
  System.out.println("SupercraftPlanTest: PASS (explicit output/ingredient/quantity caps, exact recipe batches, protected43-slot inventory, immutable preflight, no buy/maximize/action)");
 }
}
