package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.AutomationSession;
public final class PestSprayRestoreFreshTabTest {
 public static void main(String[]args){
  var f=new PestSprayTaskTest.Fixture(){
   @Override public PestSprayTask.Effect sprayEffect(AutomationSession.Handle h,PestSprayTask.Action a,long at){
    var result=super.sprayEffect(h,a,at);if(a.kind()==PestSprayTask.Kind.RESTORE){tabProof=null;spray=PestSprayObservation.unknown();}return result;
   }
  };
  f.readyUse();f.acknowledge();f.decrease(1,true);f.tick();f.tick();
  PestSprayTaskTest.check(f.task.state()!=PestSprayTask.State.COMPLETE&&f.restores==1,"a previously valid native restore is not permission to finish after its applied Tab proof has been revoked");
  System.out.println("PestSprayRestoreFreshTabTest: PASS post-native final proof rechecked, no repair/retry");
 }
}
