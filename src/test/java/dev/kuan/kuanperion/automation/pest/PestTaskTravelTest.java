package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
public final class PestTaskTravelTest {
 static final long S=1_000_000_000L;static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final class Fake implements PestTask.Port {
  final PestTaskTest.Fake base=new PestTaskTest.Fake();final List<PestPlot> sent=new ArrayList<>();Set<Integer> plots=Set.of(1);boolean known=true,accept=true;
  Fake(){base.targets=List.of();}
  public AutomationContext context(){return base.context();}public PestTargets.Point position(){return base.pos;}
  public PestTask.Frame frame(long now){return base.frame(now).withPlots(plots,known);}
  public boolean prepare(AutomationSession.Handle h,PestIdentity.Vacuum v,PestTask.Anchor a,int radius,long now){return base.prepare(h,v,a,radius,now);}
  public PestTask.Motion move(AutomationSession.Handle h,PestTargets.Point p,double t,long now){return base.move(h,p,t,now);}
  public PestTask.Capture capture(AutomationSession.Handle h,PestTargets.Target t,PestIdentity.Vacuum v,long now){return base.capture(h,t,v,now);}
  public PestTask.Motion restore(AutomationSession.Handle h,PestTask.Anchor a,long now){return base.restore(h,a,now);}
  public void release(AutomationSession.Handle h){base.release(h);}
  public boolean travel(AutomationSession.Handle h,PestPlot plot,long now){sent.add(plot);return accept;}
 }
 static final class H {
  Fake f=new Fake();PlayerPositionReceipts receipts=new PlayerPositionReceipts();AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->{},()->f.base.stops++);PestTask task=new PestTask(session,f,receipts);
  void start(){check(task.request(PestPolicy.defaults().withTravelPlots(2),true,0),"known infested plot + explicit travel permits no loaded targets");task.tick(0);task.tick(S);}
  void arrival(int id,int packet,long now){var p=PestPlot.of(id);f.base.pos=new PestTargets.Point(p.centerX(),70,p.centerZ());receipts.observe(1,packet,new PlayerPositionReceipts.Position(p.centerX(),70,p.centerZ()),true,now);task.tick(now);}
 }
 public static void main(String[]args){
  H h=new H();check(!h.task.request(PestPolicy.defaults(),true,0),"old policy never expands to travel");h.f.known=false;check(!h.task.request(PestPolicy.defaults().withTravelPlots(2),true,0),"unknown plots never authorize travel");
  h=new H();h.start();check(h.f.sent.size()==1&&h.f.sent.getFirst().id()==1,"exact observed destination dispatched once");h.f.base.pos=new PestTargets.Point(0,70,-96);h.task.tick(2*S);check(h.task.active()&&h.f.base.moves==0&&h.f.base.prepares==1,"local teleport prediction cannot start movement or reset fence");
  h.arrival(1,1,3*S);check(h.f.base.prepares==2,"server receipt confirms plot before reanchor");h.f.base.targets=List.of(new PestTargets.Target(20,PestTaskTest.TARGET.uuid(),PestTargets.Kind.BAT,new PestTargets.Point(2,71,-96),21));h.task.tick(4*S);check(h.f.base.uses==1,"normal guarded capture after confirmed arrival");
  h.f.base.targets=List.of();h.f.base.alive=0;h.task.tick(5*S);h.task.tick(6*S);h.task.tick(7*S);check(h.f.sent.size()==2&&h.f.sent.getLast().id()==0&&h.f.base.restores==0,"return command retains original anchor, no premature restore");h.arrival(0,2,8*S);
  for(int i=9;i<14&&h.task.active();i++)h.task.tick(i*S);check(h.task.state()==PestTask.State.COMPLETE&&h.f.base.pos.equals(PestTaskTest.ORIGIN)&&h.f.base.stops==0,"return packet then exact original fractional anchor/tool/view before Farm resume");
  h=new H();h.start();h.task.tick(16*S);check(h.task.state()==PestTask.State.TIMED_OUT&&h.f.sent.size()==1&&h.f.base.stops==1,"no receipt: bounded stop, never repeat warp");
  h=new H();h.start();h.f.base.manual=true;h.task.tick(2*S);check(h.task.state()==PestTask.State.CANCELLED&&h.f.sent.size()==1&&h.f.base.moves==0,"manual input cancels pending teleport");
  h=new H();h.f.accept=false;h.start();check(h.task.state()==PestTask.State.FAILED&&h.f.base.stops==1,"native command rejection releases held Farming");
  h=new H();h.start();h.f.base.slot=5;h.f.base.item=PestTaskTest.OLD;h.arrival(1,1,2*S);check(h.task.state()==PestTask.State.CANCELLED&&h.f.base.prepares==1,"manual slot change during travel is never overwritten by re-preparation");
  h=new H();h.start();h.arrival(1,1,2*S);for(int i=3;i<=8;i++)h.task.tick(i*S);check(h.f.sent.size()==2&&h.f.sent.getLast().id()==0,"visited plot never retried even when still in Tab; bounded incomplete return");h.arrival(0,2,9*S);for(int i=10;i<15&&h.task.active();i++)h.task.tick(i*S);check(h.task.state()==PestTask.State.INCOMPLETE&&h.f.base.stops==1,"remaining pests cannot become a successful Garden-clear report");
  h=new H();var original=new PestTargets.Point(235.125,78.5,235.25);h.f.base.pos=original;h.start();h.arrival(1,1,2*S);h.f.base.targets=List.of(new PestTargets.Target(20,PestTaskTest.TARGET.uuid(),PestTargets.Kind.BAT,new PestTargets.Point(2,71,-96),21));h.task.tick(3*S);h.f.base.targets=List.of();h.f.base.alive=0;h.task.tick(4*S);h.task.tick(5*S);check(h.f.sent.getLast().id()==24,"return destination comes from original known plot, not hardcoded barn or overwritten home");h.arrival(24,2,6*S);for(int i=7;i<20&&h.task.active();i++)h.task.tick(i*S);check(h.task.state()==PestTask.State.COMPLETE&&h.f.base.pos.equals(original)&&h.f.base.prepares>=4,"far corner original anchor restored by multiple bounded return legs, not broad flight fence");
  System.out.println("PestTaskTravelTest: PASS (opt-in known destinations, native receipts before movement, bounded dispatch, capture and confirmed return to exact Farm anchor, manual/error/timeout)");
 }
}
