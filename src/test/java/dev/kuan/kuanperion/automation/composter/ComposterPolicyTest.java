package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class ComposterPolicyTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static ItemSnapshot item(String id,int count,boolean protectedItem){return new ItemSnapshot(id,"minecraft:paper","",id,id,count,List.of(),protectedItem);}
 public static void main(String[]args){
  var p=ComposterPolicy.defaults();check(!p.configured()&&p.materials().isEmpty()&&p.anchor()==null,"unconfigured no consumption authority");
  var material=new ComposterPolicy.Material(ComposterObservation.Kind.ORGANIC,1000);
  var anchor=new ComposterPolicy.Anchor(-11,72,-27,"minecraft:player_head");p=new ComposterPolicy(anchor,Map.of("BOX_OF_SEEDS",material),Set.of(),90,3,192,1000,240,7200,true);check(p.configured(),"explicit machine + resources");
  var items=List.of(new ComposterPlan.Stack(9,item("BOX_OF_SEEDS",5,false)),new ComposterPlan.Stack(0,item("BOX_OF_SEEDS",5,false)),new ComposterPlan.Stack(10,item("BOX_OF_SEEDS",1,true)));
  check(!ComposterPlan.supplied(p,new ComposterObservation.Resource(0,1)),"fractional percentage target rounds up, never false empty success");
  var resource=new ComposterObservation.Resource(1000,10000);var plan=ComposterPlan.select(p,ComposterObservation.Kind.ORGANIC,resource,items,192).orElseThrow();check(plan.inventorySlot()==9&&plan.amount()==5&&plan.increase()==5000,"exact full stack + explicit unit value, no hotbar/protected");
  check(ComposterPlan.select(p,ComposterObservation.Kind.ORGANIC,new ComposterObservation.Resource(8500,10000),items,192).isEmpty(),"never overflow configured target or split/guess quantity");
  check(ComposterPlan.select(p,ComposterObservation.Kind.ORGANIC,resource,items,4).isEmpty(),"configured item cap before click, no partial stack guess");
  check(ComposterPlan.select(p,ComposterObservation.Kind.FUEL,resource,items,192).isEmpty(),"material cannot cross resource kind");
  try{new ComposterPolicy.Anchor(96,70,0,"minecraft:stone");throw new AssertionError("machine outside Barn");}catch(IllegalArgumentException expected){}
  try{new ComposterPolicy.Material(ComposterObservation.Kind.ORGANIC,0);throw new AssertionError("zero units");}catch(IllegalArgumentException expected){}
  try{new ComposterPolicy(anchor,Map.of("lower case",material),Set.of(),90,3,192,1000,240,7200,true);throw new AssertionError("display name permission");}catch(IllegalArgumentException expected){}
  var duplicate=new ArrayList<>(items);duplicate.add(items.getFirst());check(ComposterPlan.select(p,ComposterObservation.Kind.ORGANIC,resource,duplicate,192).isEmpty(),"ambiguous inventory not picked first");
  System.out.println("ComposterPolicyTest: PASS (unconfigured default, exact Barn machine/IDs/units, full-stack non-hotbar/protected/cap/target guards, no inferred values)");
 }
}
