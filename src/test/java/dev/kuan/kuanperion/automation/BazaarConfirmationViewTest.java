package dev.kuan.kuanperion.automation;
import java.util.*;import java.math.BigDecimal;
public final class BazaarConfirmationViewTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args){
  var budget=new SpendingBudget(128);var req=BazaarPurchaseViewTest.plan(budget,64).requests().getFirst();var initial=BazaarPurchaseViewTest.menu("Wheat",12,"Buy 64x Wheat",List.of("Price: 128 coins"));var quote=BazaarPurchaseView.inspect(initial,req).orElseThrow();
  for(String alias:List.of("Wheat","Buy Wheat"))check(BazaarPurchaseView.inspect(BazaarPurchaseViewTest.menu("Confirm Instant Buy",13,alias,List.of("Amount: 64x","Price: 128 coins","Click to buy now!")),req).isPresent(),"upstream exact bare-product alias still needs independent exact quantity/activation");
  budget.reserve(128,1000);
  var next=BazaarPurchaseViewTest.menu("Confirm Purchase",13,"Confirm",List.of("Wheat","Amount: 64x","Price: 128 coins"));
  check(BazaarPurchaseView.inspect(next,req).isEmpty(),"new purchase cannot borrow money already reserved");
  check(BazaarPurchaseView.confirmation(next,req,quote).isPresent(),"same transaction confirmation uses original capped reservation, not double budget");
  for(String name:List.of("Custom Amount","Buy 64x Wheat","Buy Anyway"))check(BazaarPurchaseView.confirmation(BazaarPurchaseViewTest.menu("Confirm Purchase",13,name,List.of("Wheat","Amount: 64x","Price: 128 coins","Click to buy now!")),req,quote).isEmpty(),"no same-buy/override replay role: "+name);
  for(var m:List.of(BazaarPurchaseViewTest.menu("Confirm Purchase",13,"Confirm",List.of("Wheat","Amount: 64x","Price: 129 coins")),BazaarPurchaseViewTest.menu("Confirm Purchase",13,"Confirm",List.of("Wheat","Amount: 63x","Price: 128 coins")),BazaarPurchaseViewTest.menu("Bazaar Warning",13,"Confirm",List.of("Wheat","Amount: 64x","Price: 128 coins")),BazaarPurchaseViewTest.menu("Confirm Purchase",13,"Confirm",List.of("Wheat","Price: 128 coins"))))check(BazaarPurchaseView.confirmation(m,req,quote).isEmpty(),"cost/count/unknown warning/missing amount remains refused");
  check(BazaarPurchaseView.confirmation(next,req,null).isEmpty(),"no original GUI quote, no continuation");
  System.out.println("BazaarConfirmationViewTest: PASS");
 }
}
