package dev.kuan.kuanperion.automation;
import java.util.*;
public final class BazaarNavigationViewTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static MenuSnapshot menu(String title,int id,Map<Integer,ItemSnapshot> items){var s=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<90;i++)s.add(new MenuSnapshot.Slot(i,items.getOrDefault(i,BazaarPurchaseViewTest.inventory().get(0)),i>=54));return new MenuSnapshot(1,id,1,title,54,s);}
 static ItemSnapshot item(String id,String name){return new ItemSnapshot(id,"minecraft:paper","",name,name,1,List.of(),false);}
 public static void main(String[]args){
  var q=BazaarPurchaseViewTest.plan(new SpendingBudget(1000),7).requests().getFirst();
  var search=menu("Bazaar Search",1,Map.of(11,item("WHEAT","Wheat")));
  check(BazaarNavigationView.search(search,q).orElseThrow().slot()==11,"unique exact native product inside upstream search10..42");
  check(BazaarNavigationView.search(menu("Bazaar Search",1,Map.of(11,item("CARROT_ITEM","Wheat"))),q).isEmpty(),"native ID conflict rejected");
  check(BazaarNavigationView.search(menu("Bazaar Search",1,Map.of(11,item("WHEAT","Wheat"),12,item("WHEAT","Wheat"))),q).isEmpty(),"duplicate product roles rejected");
  check(BazaarNavigationView.search(menu("Other",1,Map.of(11,item("WHEAT","Wheat"))),q).isEmpty(),"foreign title rejected");
  var product=menu("Wheat",2,Map.of(10,item("","Buy Instantly")));
  check(BazaarNavigationView.instant(product,q).orElseThrow().slot()==10,"source Buy Instantly10 + exact product title");
  check(BazaarNavigationView.instant(menu("Enchanted Wheat",2,Map.of(10,item("","Buy Instantly"))),q).isEmpty(),"substring product must not match");
  var quantity=menu("Wheat → Instant Buy",3,Map.of(16,item("","Custom Amount")));
  check(BazaarNavigationView.custom(quantity,q).orElseThrow().slot()==16,"non-spending Custom Amount16 role");
  check(BazaarNavigationView.custom(menu("Other → Instant Buy",3,Map.of(16,item("","Custom Amount"))),q).isEmpty(),"different product title rejected");
  check(BazaarPurse.read(List.of("Purse: 1,234.75")).orElseThrow()==1234,"actual sidebar balance, floor cents");
  check(BazaarPurse.read(List.of("Piggy: 2,000")).orElseThrow()==2000,"source Piggy balance alias");
  for(var lines:List.of(List.of("Purse: 1,23"),List.of("Purse: 1k"),List.of("Purse: -1"),List.of("Purse: 100", "Purse: 100"),List.of("Purse: 999999999999999999999"),List.of("Purse: 100 (+1)")))check(BazaarPurse.read(lines).isEmpty(),"unknown/duplicate/compact/overflow balance refuses");
  System.out.println("BazaarNavigationViewTest: PASS");
 }
}
