package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
public final class PestPlotTravelTest {
 static final long S=1_000_000_000L;static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final class Fake implements PestPlotTravel.Port {
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());PestTargets.Point pos=new PestTargets.Point(0,75,0);int commands,releases;boolean accept=true;
  public AutomationContext context(){return ctx;}public PestTargets.Point position(){return pos;}
  public boolean travel(AutomationSession.Handle h,PestPlot plot,long now){commands++;return accept;}
  public void release(AutomationSession.Handle h){releases++;}
 }
 public static void main(String[]args){
  var f=new Fake();var session=new AutomationSession(new AutomationCoordinator(),f::context,()->{},()->{});var receipts=new PlayerPositionReceipts();var t=new PestPlotTravel(session,receipts,f);var handle=session.begin(AutomationFeature.PEST,0,120*S);
  check(t.begin(handle,PestPlot.of(1),0)&&f.commands==1&&f.releases==1,"one leased typed travel, release owned flight keys first");
  check(!t.begin(handle,PestPlot.of(2),1)&&f.commands==1,"no duplicate command or preemption");f.pos=new PestTargets.Point(0,75,-96);t.tick(S);check(t.arrival(handle,S).isEmpty(),"local position in destination is not server proof");
  receipts.observe(1,1,new PlayerPositionReceipts.Position(0,75,-96),false,2*S);t.tick(2*S);check(t.arrival(handle,2*S).isEmpty(),"relative packet cannot prove warp arrival");
  receipts.observe(1,2,new PlayerPositionReceipts.Position(96,75,0),true,3*S);t.tick(3*S);check(t.arrival(handle,3*S).isEmpty(),"fresh packet for wrong plot rejected");
  receipts.observe(1,3,new PlayerPositionReceipts.Position(0,75,-96),true,4*S);f.pos=new PestTargets.Point(10,75,-96);t.tick(4*S);check(t.arrival(handle,4*S).isEmpty(),"local player must match authoritative destination, not just share plot");
  f.pos=new PestTargets.Point(0,75,-96);t.tick(4*S);check(t.arrival(handle,4*S).isPresent()&&t.state()==PestPlotTravel.State.ARRIVED,"new absolute packet + exact expected plot + current pose");
  receipts.observe(1,4,new PlayerPositionReceipts.Position(0,75,0),true,5*S);check(t.arrival(handle,5*S).isEmpty(),"later correction revokes a cached arrival");t.reset();
  check(t.begin(handle,PestPlot.of(2),6*S),"next bounded request");t.tick(21*S);check(t.state()==PestPlotTravel.State.TIMED_OUT&&f.commands==2,"15s deadline, no automatic retries");t.reset();
  check(t.begin(handle,PestPlot.of(2),30*S),"world guard fixture");f.ctx=new AutomationContext(2,true,true,Set.of());t.tick(31*S);check(t.state()==PestPlotTravel.State.CANCELLED&&t.arrival(handle,31*S).isEmpty(),"world change cancels, never returns to old coordinates");t.reset();
  f.ctx=new AutomationContext(1,true,true,Set.of());f.accept=false;check(!t.begin(handle,PestPlot.of(2),40*S)&&t.state()==PestPlotTravel.State.FAILED,"native dispatch rejection not successful travel");t.reset();
  session.invalidate();int before=f.commands;check(!t.begin(handle,PestPlot.of(2),41*S)&&f.commands==before,"revoked root ownership cannot dispatch");
  System.out.println("PestPlotTravelTest: PASS (one typed guarded command, server absolute/plot/pose quorum, corrections revoke, world/lease/native error/deadline, no retry)");
 }
}
