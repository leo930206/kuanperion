package dev.kuan.kuanperion.automation;
import java.util.*;import static dev.kuan.kuanperion.automation.BookCombineTaskTest.*;
public final class BookTaskCursorTest {
 static void directResult(Fixture x,long at){x.f().placed.remove(20);x.f().placed.remove(24);x.f().placed.remove(13);var result=book(9,5,"result",1);x.f().inventory.put(9,result);x.receipts().observe(1,4,20,empty());x.receipts().observe(1,4,24,empty());x.receipts().observe(1,0,9,result.item());}
 static void run(String scenario){
  var x=fixture();
  if(scenario.equals("start")){
   x.f().emptyCursor=false;check(!x.task().request(policy(),true,0)&&!x.session().active()&&x.hooks()[0]==0,"nonempty physical cursor cannot start or pause Farming");
  }else if(scenario.equals("before-first")){
   check(x.task().request(policy(),true,0),"started");x.task().tick(0);x.f().emptyCursor=false;x.task().tick(S);check(x.f().transfers==0&&x.f().closed==0&&!x.session().active(),"nonempty ready cursor denies first transfer without repair/close");
  }else if(scenario.equals("source-ack")){
   check(x.task().request(policy(),true,0),"started");x.task().tick(0);x.task().tick(S);x.f().ackCursor=false;x.f().input(9,20,x.receipts());x.task().tick(2*S);x.task().tick(3*S);check(x.f().transfers==1,"physical empty/source/destination alone cannot authorize next transfer");x.cursor().observe(1,4,empty(),4*S);x.task().tick(4*S);x.task().tick(5*S);check(x.f().transfers==2,"fresh scoped server empty cursor permits next transfer");
  }else if(scenario.equals("result-ack")){
   second(x);x.task().tick(5*S);directResult(x,6*S);x.task().tick(6*S);check(x.task().active()&&x.f().closed==0&&x.session().holdsFarming(),"fresh item receipts cannot borrow old input cursor proof for result success");x.cursor().observe(1,4,empty(),7*S);x.task().tick(7*S);check(x.task().state()==BookCombineTask.State.COMPLETE&&x.f().closed==1&&!x.session().holdsFarming(),"result plus new scoped server cursor safely completes");
  }else if(scenario.equals("wrong-scope")){
   second(x);x.task().tick(5*S);directResult(x,6*S);x.cursor().observe(1,0,empty(),6*S);x.task().tick(6*S);check(x.task().active()&&x.f().closed==0,"another container cannot supply cursor proof");x.cursor().observe(1,4,empty(),7*S);x.task().tick(7*S);check(x.task().active(),"changed cursor epoch cannot be repaired by borrowing a later scope");x.task().tick(31*S);check(!x.task().active()&&x.f().closed==0&&x.hooks()[1]==1,"unknown cursor outcome times out without close/retry or false Farming resume");
  }else if(scenario.equals("stale-time")){
   second(x);x.task().tick(5*S);directResult(x,6*S);x.cursor().observe(1,4,empty(),4*S);x.task().tick(6*S);check(x.task().active()&&x.f().closed==0,"higher sequence with pre-attempt timestamp is not a result proof");
  }else if(scenario.equals("cancel-pending")){
   check(x.task().request(policy(),true,0),"started");x.task().tick(0);x.task().tick(S);x.f().ackCursor=false;x.f().input(9,20,x.receipts());x.task().tick(2*S);x.task().cancel(2*S);x.task().tick(3*S);check(x.f().returns==0&&x.f().closed==0&&!x.session().active()&&x.task().manualRecoveryRequired(),"uncertain cursor/input is left for manual recovery, never automatically reclaimed");
  }else if(scenario.equals("cancel-nonempty")){
   first(x);x.f().emptyCursor=false;x.task().cancel(2*S);x.task().tick(3*S);check(x.f().returns==0&&x.f().closed==0&&!x.session().active()&&x.task().manualRecoveryRequired(),"a now-nonempty cursor cannot be used for owned recovery or close");
  }else if(scenario.equals("prediction-clears")){
   second(x);x.task().tick(5*S);directResult(x,6*S);x.f().emptyCursor=false;x.task().tick(6*S);check(x.task().active()&&x.f().closed==0,"in-flight prediction may settle, but no close or progress occurs on nonempty cursor");x.f().emptyCursor=true;x.cursor().observe(1,4,empty(),7*S);x.task().tick(7*S);check(x.task().state()==BookCombineTask.State.COMPLETE,"physical and server-empty proof together settle the pending action");
  }else if(scenario.equals("nonempty-server")){
   check(x.task().request(policy(),true,0),"started");x.task().tick(0);x.task().tick(S);x.f().ackCursor=false;x.f().input(9,20,x.receipts());x.cursor().observe(1,4,book(-1,4,"carried",1).item(),2*S);x.task().tick(2*S);x.task().tick(3*S);check(x.f().transfers==1,"physical-empty cannot override a nonempty server cursor receipt");x.cursor().observe(1,4,empty(),4*S);x.task().tick(4*S);x.task().tick(5*S);check(x.f().transfers==2,"server empty may settle the pending move without another first input");
  }else if(scenario.equals("recovery-ack")){
   first(x);x.task().cancel(2*S);x.task().tick(3*S);x.f().placed.remove(20);var original=book(9,4,"one",1);x.f().inventory.put(9,original);x.receipts().observe(1,4,20,empty());x.receipts().observe(1,0,9,original.item());x.task().tick(4*S);check(x.task().active()&&x.f().closed==0,"recovery item receipt alone cannot close the owned menu");x.cursor().observe(1,4,empty(),5*S);x.task().tick(5*S);check(x.task().state()==BookCombineTask.State.CANCELLED&&x.f().closed==1&&x.hooks()[1]==1,"fresh recovery cursor proof permits bounded owned close without false success");
  }else if(scenario.equals("age-boundary")){
   for(long extra:new long[]{0,1}){var y=fixture();second(y);y.task().tick(5*S);directResult(y,6*S);y.cursor().observe(1,4,empty(),6*S);y.task().tick(16*S+extra);check(extra==0?y.task().state()==BookCombineTask.State.COMPLETE:y.task().active()&&y.f().closed==0,"cursor receipt age is inclusive10 seconds, never unbounded");}
  }else if(scenario.equals("cursor-flips-on-close")){
   second(x);x.task().tick(5*S);directResult(x,6*S);x.cursor().observe(1,4,empty(),6*S);x.f().nonemptyAtRead=x.f().cursorReads+2;x.task().tick(6*S);check(x.task().state()==BookCombineTask.State.FAILED&&x.f().closed==0&&x.hooks()[1]==1,"cursor revalidation failure at close cannot skip cleanup and report success");
  }else throw new AssertionError("unknown scenario");
 }
 public static void main(String[]args){for(String s:args.length>0?List.of(args[0]):List.of("start","before-first","source-ack","result-ack","wrong-scope","stale-time","cancel-pending","cancel-nonempty","prediction-clears","nonempty-server","recovery-ack","age-boundary","cursor-flips-on-close"))run(s);System.out.println("BookTaskCursorTest: PASS (physical cursor, fresh scoped/epoch/time proofs, settle prediction, no borrowed success/recovery/close/retry)");}
}
