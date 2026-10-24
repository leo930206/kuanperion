package dev.kuan.kuanperion.automation;
import java.util.*;
import java.math.BigDecimal;
import static dev.kuan.kuanperion.automation.GuardedMenuAction.Result.*;
public final class BazaarPurchaseLedgerTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static ItemSnapshot item(int n){return new ItemSnapshot(n==0?"":"WHEAT",n==0?"minecraft:air":"minecraft:wheat","","Wheat",n==0?"":"wheat-metadata",n,List.of(),n==0);}
 static Map<Integer,ItemSnapshot> before(){var map=new HashMap<Integer,ItemSnapshot>();for(int i=9;i<=44;i++)map.put(i,item(0));return map;}
 static BazaarPurchaseLedger.Order order(){return new BazaarPurchaseLedger.Order("WHEAT","Wheat",3,new BigDecimal("14.00"));}
 public static void main(String[] args){
  var receipts=new InventoryReceipts();var zero=new BazaarPurchaseLedger(new SpendingBudget(0),receipts,()->true);check(!zero.begin(order(),1,100,before(),0,()->{throw new AssertionError("zero budget click");}),"zero budget never clicks");
  var budget=new SpendingBudget(100);var ledger=new BazaarPurchaseLedger(budget,receipts,()->true);int[] clicks={0};check(ledger.begin(order(),1,100,before(),0,()->{clicks[0]++;return SENT;}),"reserve immediately before guarded click");check(budget.available()==86&&budget.spent()==0,"reservation not a purchase receipt");
  ledger.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 12.50 coins!");ledger.poll(1);check(ledger.state()==BazaarPurchaseLedger.State.AWAITING,"chat alone does not prove item delivery");
  check(!receipts.confirmsIncrease(receipts.stamp(1),before(),"WHEAT",3),"no local prediction receipt");
  receipts.observe(1,0,8,item(3));ledger.poll(2);check(ledger.state()==BazaarPurchaseLedger.State.AWAITING,"non-player inventory slot not a delivery");
  receipts.observe(1,0,9,item(3));ledger.poll(3);check(ledger.state()==BazaarPurchaseLedger.State.CONFIRMED&&budget.spent()==13&&budget.available()==87&&clicks[0]==1,"exact fresh delivery and matching actual cost then commit, conservatively ceil fractional coins");
  ledger.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 12.50 coins!");ledger.poll(4);check(budget.spent()==13,"receipt cannot replay");
  var budget2=new SpendingBudget(100);var ledger2=new BazaarPurchaseLedger(budget2,receipts,()->true);check(ledger2.begin(order(),1,100,before(),10,()->SENT),"second purchase");
  ledger2.onSystemMessage(1,"Player: [Bazaar] Bought 3x Wheat for 12.50 coins!");ledger2.onSystemMessage(2,"[Bazaar] Bought 3x Wheat for 12.50 coins!");ledger2.onSystemMessage(1,"[Bazaar] Bought 4x Wheat for 12.50 coins!");ledger2.onSystemMessage(1,"[Bazaar] Bought 3x Carrot for 12.50 coins!");receipts.observe(1,0,9,item(3));ledger2.poll(11);check(ledger2.state()==BazaarPurchaseLedger.State.AWAITING,"wrong source/world/product/quantity or inventory alone not sufficient");
  ledger2.cancel();check(ledger2.state()==BazaarPurchaseLedger.State.UNCERTAIN&&budget2.available()==86,"after click cancellation cannot refund reservation or retry automatically");
  var budget3=new SpendingBudget(100);var ledger3=new BazaarPurchaseLedger(budget3,receipts,()->true);check(!ledger3.begin(order(),1,100,before(),20,()->REJECTED)&&budget3.available()==100,"guard says no click: reservation safely released");
  var ledger4=new BazaarPurchaseLedger(budget3,receipts,()->true);check(!ledger4.begin(order(),1,100,before(),30,()->{throw new IllegalStateException("native send outcome unknown");})&&budget3.available()==86&&ledger4.state()==BazaarPurchaseLedger.State.UNCERTAIN,"native failure may follow send: keep hold");
  var typedUnknownBudget=new SpendingBudget(100);var typedUnknown=new BazaarPurchaseLedger(typedUnknownBudget,receipts,()->true);typedUnknown.begin(order(),1,100,before(),35,()->UNCERTAIN);check(typedUnknown.state()==BazaarPurchaseLedger.State.UNCERTAIN&&typedUnknownBudget.available()==86,"typed ambiguous native result never releases money reservation");
  var ledger5=new BazaarPurchaseLedger(new SpendingBudget(100),receipts,()->true);ledger5.begin(order(),1,100,before(),40,()->SENT);ledger5.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 15 coins!");check(ledger5.state()==BazaarPurchaseLedger.State.UNCERTAIN,"server charged over GUI limit: never commit misleading success");
  var ledger6=new BazaarPurchaseLedger(new SpendingBudget(100),receipts,()->true);ledger6.begin(order(),1,100,before(),50,()->SENT);ledger6.poll(15_000_000_050L);check(ledger6.state()==BazaarPurchaseLedger.State.UNCERTAIN,"bounded delivery/receipt deadline");
  var tinyBudget=new SpendingBudget(2);var tiny=new BazaarPurchaseLedger(tinyBudget,receipts,()->true);tiny.begin(new BazaarPurchaseLedger.Order("WHEAT","Wheat",3,new BigDecimal("0.90")),1,100,before(),60,()->SENT);tiny.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 0.50 coins!");receipts.observe(1,0,9,item(3));tiny.poll(61);check(tiny.state()==BazaarPurchaseLedger.State.CONFIRMED&&tinyBudget.spent()==1,"fractional sub-coin costs still require and accept a real receipt");
  var conflict=new BazaarPurchaseLedger(new SpendingBudget(100),receipts,()->true);conflict.begin(order(),1,100,before(),70,()->SENT);conflict.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 12 coins!");conflict.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 13 coins!");check(conflict.state()==BazaarPurchaseLedger.State.UNCERTAIN,"conflicting matching receipts never select the cheaper result");
  var protectedBefore=before();protectedBefore.put(9,item(64));var stamp=receipts.stamp(1);receipts.observe(1,0,9,item(63));receipts.observe(1,0,10,item(4));check(receipts.confirmsIncrease(stamp,protectedBefore,"WHEAT",3),"aggregate authoritative delta across split stacks");
  System.out.println("BazaarPurchaseLedgerTest: PASS (zero budget, guarded click, dual server receipts, no replay/spoof, bounded deadline, uncertain spend never refunded)");
 }
}
