package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.math.BigDecimal;
public final class VisitorValuationTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static ItemSnapshot item(String id,int count,boolean protectedItem){return new ItemSnapshot(id,"minecraft:wheat","",id,"fp:"+id,count,List.of(),protectedItem);}
 static Map<Integer,ItemSnapshot> inventory(){var m=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<36;i++)m.put(i,item("",0,false));m.put(9,item("ENCHANTED_WHEAT",2,false));m.put(10,item("ENCHANTED_CARROT",64,false));return m;}
 static BazaarMarket market(long now){var m=new HashMap<String,BazaarMarket.Book>();m.put("ENCHANTED_WHEAT",new BazaarMarket.Book(List.of(new BazaarMarket.Level(10000,new BigDecimal("2.1"))),List.of()));m.put("ENCHANTED_CARROT",new BazaarMarket.Book(List.of(new BazaarMarket.Level(10000,new BigDecimal("1.1"))),List.of()));return new BazaarMarket(now,m);}
 static VisitorValuation.Rules rules(){return new VisitorValuation.Rules(Set.of("Librarian"),Set.of("ENCHANTED_WHEAT","ENCHANTED_CARROT"),2000,1500,1);}
 public static void main(String[] args){
  long now=1000000;var offer=VisitorOfferTest.read();var inv=inventory();var q=VisitorValuation.estimate(offer,rules(),inv,market(now),now).orElseThrow();
  check(q.offerCoins()==1134&&q.purchaseCoins()==1059,"conservative whole-coin full opportunity cost and missing-items estimate, not reward profit");
  check(q.missing().get("ENCHANTED_WHEAT")==1&&q.missing().get("ENCHANTED_CARROT")==960,"exact protected main-inventory deficits");
  inv.put(0,item("ENCHANTED_WHEAT",64,false));inv.put(11,item("ENCHANTED_WHEAT",64,true));inv.put(12,new ItemSnapshot("ENCHANTED_WHEAT","minecraft:wheat","unique","Wheat","uuid-fp",64,List.of(),false));
  check(VisitorValuation.estimate(offer,rules(),inv,market(now),now).orElseThrow().missing().get("ENCHANTED_WHEAT")==1,"hotbar, protected, UUID items never available to submission");
  check(VisitorValuation.estimate(offer,VisitorValuation.Rules.defaults(),inv,market(now),now).isEmpty(),"off/unconfigured rules deny");
  check(VisitorValuation.estimate(offer,new VisitorValuation.Rules(Set.of("Other"),rules().materialIds(),2000,1500,1),inv,market(now),now).isEmpty(),"exact visitor allowlist");
  check(VisitorValuation.estimate(offer,new VisitorValuation.Rules(Set.of("Librarian"),Set.of("ENCHANTED_WHEAT"),2000,1500,1),inv,market(now),now).isEmpty(),"all required IDs authorized, no partial offer");
  for(var r:List.of(new VisitorValuation.Rules(Set.of("Librarian"),rules().materialIds(),1133,1500,1),new VisitorValuation.Rules(Set.of("Librarian"),rules().materialIds(),2000,1058,1),new VisitorValuation.Rules(Set.of("Librarian"),rules().materialIds(),2000,1500,26)))check(VisitorValuation.estimate(offer,r,inv,market(now),now).isEmpty(),"value, buying and copper limits independent");
  check(VisitorValuation.estimate(offer,rules(),inv,market(now),now+120001).isEmpty(),"stale market is unknown, not free");
  check(VisitorValuation.estimate(offer,rules(),inv,new BazaarMarket(now,Map.of()),now).isEmpty(),"unpriced requirement denies entire estimate");
  var shallow=new BazaarMarket(now,Map.of("ENCHANTED_WHEAT",new BazaarMarket.Book(List.of(new BazaarMarket.Level(2,BigDecimal.ONE)),List.of()),"ENCHANTED_CARROT",market(now).books().get("ENCHANTED_CARROT")));check(VisitorValuation.estimate(offer,rules(),inv,shallow,now).isEmpty(),"full request depth needed even when only one item is missing");
  inv.remove(35);check(VisitorValuation.estimate(offer,rules(),inv,market(now),now).isEmpty(),"incomplete inventory cannot be assumed empty");inv.put(35,item("",0,false));
  inv.put(9,item("ENCHANTED_WHEAT",3,false));for(int i=10;i<26;i++)inv.put(i,item("ENCHANTED_CARROT",64,false));
  var noBuy=VisitorValuation.estimate(offer,new VisitorValuation.Rules(Set.of("Librarian"),rules().materialIds(),2000,0,1),inv,market(now),now).orElseThrow();check(noBuy.missing().isEmpty()&&noBuy.purchaseCoins()==0&&noBuy.offerCoins()==1134,"inventory-only permission still accounts full material value, no spending authority");
  try{q.missing().clear();throw new AssertionError("mutable estimate");}catch(UnsupportedOperationException expected){}
  System.out.println("VisitorValuationTest: PASS (exact explicit rules, protected main-only inventory, fresh depth-rounded full/deficit prices, no unknown-as-free or estimated spending authority)");
 }
}
