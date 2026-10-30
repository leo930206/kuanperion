package dev.kuan.kuanperion.automation.pest;
public final class PestSprayGroundMotionTest {
 static class F extends PestSprayTaskTest.Fixture {
  double vy=-.08*(double).98F;boolean supported=true;
  @Override public PestSprayTask.Frame sprayFrame(long at){var f=super.sprayFrame(at);return new PestSprayTask.Frame(f.world(),f.observedAt(),f.level(),f.player(),f.inventory(),f.cursor(),f.selected(),f.idleInventory(),f.usingItem(),f.x(),f.y(),f.z(),f.vx(),vy,f.vz(),f.yaw(),f.pitch(),f.spray(),f.positionEpoch(),f.positionSequence(),f.tab(),supported);}
 }
 static class G extends PestSpraySelectionTaskTest.F {
  @Override PestSprayTask.Frame motion(long at){var f=super.motion(at);return new PestSprayTask.Frame(f.world(),f.observedAt(),f.level(),f.player(),f.inventory(),f.cursor(),f.selected(),f.idleInventory(),f.usingItem(),f.x(),f.y(),f.z(),f.vx(),-.08*(double).98F,f.vz(),f.yaw(),f.pitch(),f.spray(),f.positionEpoch(),f.positionSequence(),f.tab(),true);}
 }
 public static void main(String[]args){
  var f=new F();f.begin();for(int i=0;i<20&&f.task.active()&&f.uses==0;i++)f.tick();PestSprayTaskTest.check(f.uses==1&&f.task.state()==PestSprayTask.State.AWAITING,"stable supported vanilla post-collision gravity residual must not prevent settling: "+f.task.state());
  f=new F();f.supported=false;f.begin();for(int i=0;i<20&&f.task.active();i++)f.tick();PestSprayTaskTest.check(f.uses==0&&f.task.state()==PestSprayTask.State.TIMED_OUT,"airborne identical downward velocity is not supported residual");
  f=new F();f.vy=.08;f.begin();for(int i=0;i<20&&f.task.active();i++)f.tick();PestSprayTaskTest.check(f.uses==0,"upward/jump velocity is never exempt even with support flag");
  double residual=-.08*(double).98F;PestSprayTaskTest.check(PestSprayGroundMotion.residual(true,false,.08,1,false,residual),"exact pinned float drag expression");
  for(int i=0;i<9;i++){boolean support=i!=0,exceptional=i==1,discard=i==2;double g=i==3?Double.NaN:i==4?0:.08,mod=i==5?Double.NaN:1,vy=i==6?Math.nextUp(residual):i==7?.01:i==8?Double.NaN:residual;PestSprayTaskTest.check(!PestSprayGroundMotion.residual(support,exceptional,g,mod,discard,vy),"unsupported/exceptional/wrong-friction/nonfinite/zero-gravity/nonexact/upward rejected "+i);}
  var g=new G();g.clicked();g.selected(true,true);g.tick();g.tick();PestSprayTaskTest.check(g.task.state()==PestSpraySelectionTask.State.COMPLETE&&g.closes==1&&g.stops==0,"original-root selection uses the same proven stationary support exception, retaining all receipt guards");
  System.out.println("PestSprayGroundMotionTest: PASS");
 }
}
