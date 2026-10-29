package dev.kuan.kuanperion.automation;
import java.util.*;import java.math.BigDecimal;import static dev.kuan.kuanperion.automation.GuardedMenuAction.Result.*;
public final class BazaarPurchaseConfirmationTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static void next(BazaarPurchaseAttemptTest.H h){var old=h.m;h.m=new MenuSnapshot(1,8,2,"Confirm Purchase",54,old.slots());h.cursors.observe(1,8,BazaarPurchaseAttemptTest.AIR,2_000_000_000L);}
 static boolean confirm(BazaarPurchaseAttemptTest.H h,GuardedMenuAction.Result result){var c=new MenuContract(Set.of("Confirm Purchase"),54);return h.task.confirm(h.frame(2_000_000_000L),c,c.plan(h.m,13,MenuContract.Click.PICKUP),new BigDecimal("14"),2_000_000_000L,()->{h.clicks++;return result;});}
 public static void main(String[]args){
  var h=new BazaarPurchaseAttemptTest.H(AutomationFeature.VISITOR);check(h.arm()&&h.send(),"one initial attempt");next(h);check(h.task.canConfirm()&&confirm(h,SENT)&&h.clicks==2&&h.budget.available()==86,"same pending transaction confirms once, no new reservation");check(!confirm(h,SENT)&&h.clicks==2,"never third click");
  h.inv.put(9,BazaarPurchaseAttemptTest.item("WHEAT",3));h.m=new MenuSnapshot(1,8,3,h.m.title(),54,BazaarPurchaseAttemptTest.menu(h.inv,3).slots());h.items.observe(1,0,9,h.inv.get(9));h.items.observe(1,8,54,h.inv.get(9));h.cursors.observe(1,8,BazaarPurchaseAttemptTest.AIR,3_000_000_000L);h.task.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 12.50 coins!");h.task.poll(h.frame(3_000_000_000L),3_000_000_000L);check(h.task.state()==BazaarPurchaseAttempt.State.CONFIRMED&&h.budget.spent()==13,"new active-container receipts + original canonical and GAME cost commit original reservation once");
  for(String mode:List.of("notice","drift","cursor","reject","uncertain","expired")){
   h=new BazaarPurchaseAttemptTest.H(AutomationFeature.VISITOR);h.arm();h.send();next(h);
   switch(mode){case "notice"->h.task.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 12.50 coins!");case "drift"->h.inv.put(40,BazaarPurchaseAttemptTest.item("STONE",1));case "cursor"->h.cursors.observe(1,8,BazaarPurchaseAttemptTest.item("STONE",1),2_000_000_000L);case "expired"->h.session.invalidate();}
   var result=mode.equals("reject")?REJECTED:mode.equals("uncertain")?UNCERTAIN:SENT;
   check(!confirm(h,result)&&h.budget.available()==86&&h.budget.spent()==0,"no retry/refund or invented success: "+mode);
  }
  System.out.println("BazaarPurchaseConfirmationTest: PASS");
 }
}
