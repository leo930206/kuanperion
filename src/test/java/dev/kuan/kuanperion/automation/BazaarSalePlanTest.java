// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
import java.math.BigDecimal;

/** Declared full inventory and public-depth fixtures, no native sell button, fee or live-price claim. */
public final class BazaarSalePlanTest {
 static final long NOW=BazaarMarketTest.NOW;
 static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
 static ItemSnapshot wheat(int count){return new ItemSnapshot("WHEAT","minecraft:wheat","","Wheat","canonical-wheat",count,List.of("COMMON"),false);}
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static Map<Integer,ItemSnapshot> inventory(){var map=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)map.put(i,AIR);map.put(9,wheat(2));map.put(10,wheat(1));return map;}
 static BazaarSalePlan.Rules rules(int products,int stacks,int items){return new BazaarSalePlan.Rules(Map.of("WHEAT",new BazaarSalePlan.Material(wheat(1),64,new BigDecimal("3"))),products,stacks,items);}
 static BazaarMarket market(){return BazaarMarket.parse(BazaarMarketTest.data(BazaarMarketTest.fixture()),NOW);}
 static void success(){var inv=inventory();var plan=BazaarSalePlan.plan(inv,rules(3,5,320),market(),NOW).orElseThrow();var sale=plan.sales().get(0);
  check(plan.sales().size()==1&&sale.productId().equals("WHEAT")&&sale.quantity()==3&&sale.slots().equals(List.of(9,10)),"all selected commodity stacks, stable main slots and exact total");
  check(sale.estimatedGrossCoins()==11&&sale.minimumNetCoins().compareTo(new BigDecimal("9"))==0,"fresh depth estimate is gross, configured minimum is independent exact net");
  check(sale.withinConfiguredBounds("WHEAT",3,new BigDecimal("9"))&&sale.withinConfiguredBounds("WHEAT",3,new BigDecimal("10.01")),"actual net can differ from API gross; no invented tax formula");
  for(Object[] bad:new Object[][]{{"WHEAT",3,new BigDecimal("8.99")},{"WHEAT",2,new BigDecimal("20")},{"WHEAT",4,new BigDecimal("20")},{"CARROT_ITEM",3,new BigDecimal("20")},{"WHEAT",3,null},{"WHEAT",3,new BigDecimal("1e100")},{"WHEAT",3,new BigDecimal("9.0000000000001")}})check(!sale.withinConfiguredBounds((String)bad[0],(int)bad[1],(BigDecimal)bad[2]),"id/quantity/net/finite-scale bounds, not estimate authorization");
  check(plan.stillMatches(inv),"same complete baseline");inv.put(42,wheat(1));check(!plan.stillMatches(inv)&&plan.baseline().get(42).count()==0,"immutable complete43, including equipment/saddle");
  try{plan.sales().clear();throw new AssertionError("mutable sale list");}catch(UnsupportedOperationException expected){}
 }
 static void veto(){for(String reason:List.of("hotbar","equipment","protected","uuid","native-id","fingerprint","name","lore","overstack","incomplete","malformed-air")){
  var inv=inventory();var original=wheat(1);switch(reason){
   case "hotbar"->inv.put(0,original);case "equipment"->inv.put(42,original);
   case "protected"->inv.put(10,new ItemSnapshot("WHEAT","minecraft:wheat","","Wheat","canonical-wheat",1,original.lore(),true));
   case "uuid"->inv.put(10,new ItemSnapshot("WHEAT","minecraft:wheat","11111111111111111111111111111111","Wheat","canonical-wheat",1,original.lore(),false));
   case "native-id"->inv.put(10,new ItemSnapshot("WHEAT","minecraft:diamond_sword","","Wheat","canonical-wheat",1,original.lore(),false));
   case "fingerprint"->inv.put(10,new ItemSnapshot("WHEAT","minecraft:wheat","","Wheat","extra-metadata",1,original.lore(),false));
   case "name"->inv.put(10,new ItemSnapshot("WHEAT","minecraft:wheat","","Renamed valuable","canonical-wheat",1,original.lore(),false));
   case "lore"->inv.put(10,new ItemSnapshot("WHEAT","minecraft:wheat","","Wheat","canonical-wheat",1,List.of("other metadata"),false));
   case "overstack"->inv.put(10,wheat(65));case "incomplete"->inv.remove(42);case "malformed-air"->inv.put(42,new ItemSnapshot("PET","minecraft:air","","","",0,List.of(),true));
  }check(BazaarSalePlan.plan(inv,rules(3,5,320),market(),NOW).isEmpty(),"whole target veto for "+reason+", not silent partial sell");
 }}
 static void limits(){for(var r:List.of(rules(3,1,320),rules(3,5,2),BazaarSalePlan.Rules.defaults()))check(BazaarSalePlan.plan(inventory(),r,market(),NOW).isEmpty(),"no partial truncation/empty-default permission");
  check(BazaarSalePlan.plan(inventory(),rules(3,5,320),market(),NOW+120001).isEmpty(),"stale book not even an estimate");
  var inv=inventory();inv.put(10,wheat(2));check(BazaarSalePlan.plan(inv,rules(3,5,320),market(),NOW).isEmpty(),"insufficient visible depth never extrapolated");
  var high=new BazaarSalePlan.Rules(Map.of("WHEAT",new BazaarSalePlan.Material(wheat(1),64,new BigDecimal("4"))),3,5,320);check(BazaarSalePlan.plan(inventory(),high,market(),NOW).isEmpty(),"known gross below minimum net cannot produce a sale plan");
 }
 static void multiple(){
  var carrot=new ItemSnapshot("CARROT_ITEM","minecraft:carrot","","Carrot","canonical-carrot",2,List.of("COMMON"),false);var inv=inventory();inv.put(11,carrot);
  var materials=new HashMap<String,BazaarSalePlan.Material>();materials.put("WHEAT",new BazaarSalePlan.Material(wheat(1),64,new BigDecimal("3")));materials.put("CARROT_ITEM",new BazaarSalePlan.Material(carrot,64,BigDecimal.ONE));
  var books=new HashMap<>(market().books());books.put("CARROT_ITEM",books.get("WHEAT"));var publicData=new BazaarMarket(NOW,books);
  var rules=new BazaarSalePlan.Rules(materials,2,3,5);var plan=BazaarSalePlan.plan(inv,rules,publicData,NOW).orElseThrow();
  check(plan.sales().stream().map(BazaarSalePlan.Sale::productId).toList().equals(List.of("WHEAT","CARROT_ITEM"))&&plan.estimatedGrossCoins()==18,"stable first-slot product order and full exact gross sum");
  for(var small:List.of(new BazaarSalePlan.Rules(materials,1,3,5),new BazaarSalePlan.Rules(materials,2,2,5),new BazaarSalePlan.Rules(materials,2,3,4)))check(BazaarSalePlan.plan(inv,small,publicData,NOW).isEmpty(),"all selected products/stacks/items bounded, never truncate");
  materials.clear();check(rules.materials().size()==2,"immutable explicit approval map, not later widened/mutated");
  try{plan.sales().get(0).slots().clear();throw new AssertionError("mutable native-slot plan");}catch(UnsupportedOperationException expected){}
  var tool=new ItemSnapshot("TOOL","minecraft:iron_hoe","11111111111111111111111111111111","tool","protected-tool",1,List.of(),true);inv=inventory();inv.put(0,tool);
  check(BazaarSalePlan.plan(inv,rules(3,5,320),market(),NOW).isPresent(),"unrelated protected tool remains untouched, not whole-inventory authority");
  inv.put(42,new ItemSnapshot("OTHER","minecraft:stone","11111111-1111-1111-1111-111111111111","other","other",1,List.of(),true));check(BazaarSalePlan.plan(inv,rules(3,5,320),market(),NOW).isEmpty(),"duplicate canonical unrelated UUID makes complete snapshot ambiguous");
 }
 static void validation(){
  for(var exemplar:List.of(AIR,new ItemSnapshot("WHEAT","minecraft:wheat","u","Wheat","canonical-wheat",1,List.of(),false),new ItemSnapshot("WHEAT","minecraft:wheat","","Wheat","",1,List.of(),false),new ItemSnapshot("WHEAT","minecraft:wheat","","Wheat","canonical-wheat",1,List.of(),true))){boolean denied=false;try{new BazaarSalePlan.Material(exemplar,64,BigDecimal.ONE);}catch(IllegalArgumentException expected){denied=true;}check(denied,"only explicitly approved unprotected identified commodity exemplar");}
  for(BigDecimal price:List.of(BigDecimal.ZERO,new BigDecimal("-1"),new BigDecimal("1e100"),new BigDecimal("0.0000000000001"))){boolean denied=false;try{new BazaarSalePlan.Material(wheat(1),64,price);}catch(IllegalArgumentException expected){denied=true;}check(denied,"bounded positive exact per-unit minimum");}
 }
 public static void main(String[] args){success();veto();limits();multiple();validation();System.out.println("BazaarSalePlanTest: PASS (explicit pinned commodity identity, whole-product limits, protected43 baseline, fresh gross estimates vs exact configured net; no native sell/fee/price authority)");}
}
