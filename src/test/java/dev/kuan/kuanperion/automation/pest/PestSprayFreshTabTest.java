package dev.kuan.kuanperion.automation.pest;
public final class PestSprayFreshTabTest {
 static void check(boolean b,String s){PestSprayTaskTest.check(b,s);}
 public static void main(String[]args){
  var f=new PestSprayTaskTest.Fixture();f.readyUse();
  f.task.onSystemMessage(1,"Sprayonator! You sprayed plot 1!",f.now);f.decrease(1,true);
  f.spray=new PestSprayObservation(PestSprayObservation.State.ACTIVE);
  f.tick();f.tick();check(f.task.state()==PestSprayTask.State.AWAITING&&f.restores==0,"cached ACTIVE without a post-claim applied Tab receipt must NOT complete");
  f.applyTab();f.tick();f.tick();check(f.task.state()==PestSprayTask.State.COMPLETE,"genuinely new applied field + GAME + applied inventory delta completes");
  f=new PestSprayTaskTest.Fixture();f.now+=PestSprayTabReceipts.MAX_AGE+1;check(!f.task.request(PestSprayPlanTest.policy(),f.now)&&f.pauses==0,"stale None cannot acquire a new root");
  f=new PestSprayTaskTest.Fixture();f.tabProof=null;check(!f.task.request(PestSprayPlanTest.policy(),f.now)&&f.pauses==0,"cached None without any server receipt cannot acquire");
  f=new PestSprayTaskTest.Fixture();f.x=96;check(!f.task.request(PestSprayPlanTest.policy(),f.now)&&f.pauses==0,"cached None from the previous plot is not a refreshed new-plot fact");
  f=new PestSprayTaskTest.Fixture();f.readyUse();f.acknowledge();f.decrease(1,true);f.refreshTab=false;f.now+=PestSprayTabReceipts.MAX_AGE+1;f.task.tick(f.now);check(f.task.state()==PestSprayTask.State.AWAITING&&f.restores==0,"expired active proof cannot complete even when other receipts succeeded");f.applyTab();f.tick();f.tick();check(f.task.state()==PestSprayTask.State.COMPLETE,"bounded explicit refresh permits completion without replaying use");
  f=new PestSprayTaskTest.Fixture();f.readyUse();f.acknowledge();f.decrease(1,true);f.tabs.clear();f.applyTab();f.tick();check(!f.task.active()&&f.restores==0&&f.uses==1,"new receipt epoch cannot complete an older use");
  f=new PestSprayTaskTest.Fixture();f.readyUse();f.acknowledge();f.decrease(1,true);f.tick();check(f.task.state()==PestSprayTask.State.READY_RESTORE,"receipt accepted before restoration");f.now+=PestSprayTabReceipts.MAX_AGE+1;f.task.tick(f.now);check(f.task.state()==PestSprayTask.State.FAILED&&f.restores==0,"final no-op/real restore cannot use an expired receipt");
  System.out.println("PestSprayFreshTabTest: PASS no cached success/unknown None/plot handover/expiry/epoch reuse; no native/server claim");
 }
}
