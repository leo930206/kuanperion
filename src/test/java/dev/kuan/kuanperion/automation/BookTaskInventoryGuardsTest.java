package dev.kuan.kuanperion.automation;
import java.util.*;
import static dev.kuan.kuanperion.automation.BookCombineTaskTest.*;
public final class BookTaskInventoryGuardsTest {
 static void run(String scenario){
  var x=fixture();
  if(scenario.equals("before-first")){
   check(x.task().request(policy(),true,0),"started");x.task().tick(0);x.f().extra.put(42,AutoSellTaskTest.wheat(1));x.task().tick(S);
   check(x.f().transfers==0&&!x.task().active()&&!x.session().holdsFarming(),"protected saddle drift must stop before first transfer");
  }else if(scenario.equals("before-second")){
   first(x);x.f().extra.put(12,AutoSellTaskTest.wheat(1));x.task().tick(3*S);
   check(x.f().transfers==1&&!x.task().active()&&x.f().closed==0,"unrelated nonbook inventory change cannot hide behind book-only map");
  }else if(scenario.equals("before-combine")){
   second(x);x.f().extra.put(40,AutoSellTaskTest.wheat(1));x.task().tick(5*S);
   check(x.f().combines==0&&!x.task().active()&&x.f().closed==0,"protected offhand drift denies irreversible combine and leaves known books");
  }else if(scenario.equals("result")){
   second(x);x.task().tick(5*S);x.f().placed.remove(20);x.f().placed.remove(24);x.f().placed.remove(13);var result=book(9,5,"result",1);x.f().inventory.put(9,result);x.f().extra.put(12,AutoSellTaskTest.wheat(1));x.receipts().observe(1,4,20,empty());x.receipts().observe(1,4,24,empty());x.receipts().observe(1,0,9,result.item());x.task().tick(6*S);
   check(x.task().state()!=BookCombineTask.State.COMPLETE&&x.f().closed==0&&x.hooks()[1]==1,"a good result receipt cannot bless unrelated inventory creation");
  }else if(scenario.equals("before-recovery")){
   first(x);x.task().cancel(2*S);x.f().extra.put(41,AutoSellTaskTest.wheat(1));x.task().tick(3*S);
   check(x.f().returns==0&&!x.task().active()&&x.f().closed==0,"recovery cannot move items after protected inventory drift");
  }else if(scenario.equals("source-ack")){
   check(x.task().request(policy(),true,0),"started");x.task().tick(0);x.task().tick(S);var moved=x.f().inventory.remove(9);x.f().placed.put(20,new BookCombinePlanner.Book(-1,moved.item(),moved.enchantments(),moved.compatibility()));x.receipts().observe(1,4,54,empty());x.receipts().observe(1,4,20,moved.item());x.task().tick(2*S);x.task().tick(3*S);check(x.f().transfers==1,"container-only source evidence cannot substitute for canonical inventory receipt");x.receipts().observe(1,0,9,empty());x.task().tick(4*S);x.task().tick(5*S);check(x.f().transfers==2,"canonical source proof unlocks next exact transfer");
  }else if(scenario.equals("preserved-unrelated")){
   x.f().extra.put(12,AutoSellTaskTest.wheat(5));second(x);x.task().tick(5*S);x.f().placed.remove(20);x.f().placed.remove(24);x.f().placed.remove(13);var result=book(9,5,"result",1);x.f().inventory.put(9,result);x.receipts().observe(1,4,20,empty());x.receipts().observe(1,4,24,empty());x.receipts().observe(1,0,9,result.item());x.task().tick(6*S);check(x.task().state()==BookCombineTask.State.COMPLETE&&x.f().extra.get(12).count()==5,"preexisting unrelated nonbook items are preserved, not globally forbidden");
  }else if(scenario.equals("stop-failure")){
   x=fixture(true);check(x.task().request(policy(),true,0),"started");x.task().tick(0);x.f().extra.put(42,AutoSellTaskTest.wheat(1));boolean thrown=false;try{x.task().tick(S);}catch(IllegalStateException expected){check(expected.getMessage().equals("safety stop failed"),"stop error unchanged");thrown=true;}check(thrown&&!x.session().active()&&!x.session().holdsFarming()&&!x.task().active()&&x.hooks()[1]==1,"safety stop callback failure must propagate after original-root revocation");
  }else throw new AssertionError("unknown scenario");
 }
 public static void main(String[]args){for(String s:args.length>0?List.of(args[0]):List.of("before-first","before-second","before-combine","result","before-recovery","source-ack","preserved-unrelated","stop-failure"))run(s);System.out.println("BookTaskInventoryGuardsTest: PASS (actual task/session, full43 drift before transfers/combine/recovery and after result; no unrelated native effects)");}
}
