package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestTaskBaitTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class P extends PestBaitButtonTaskTest.P implements PestTask.Port {
  final PestTaskTest.Fake base=new PestTaskTest.Fake();
  P(){h.session.cancel(h.now);base.alive=0;base.targets=List.of();h.inv.put(3,PestTaskTest.TOOL);}
  public PestTask.Frame frame(long n){var f=base.frame(n);return new PestTask.Frame(f.world(),f.observedAt(),f.position(),f.eye(),f.yaw(),f.pitch(),f.slot(),f.item(),f.mayFly(),f.flying(),manual,f.alive(),f.targets(),f.tools(),f.complete());}
  public MenuSnapshot menu(){return h.menu;}
  public boolean prepare(AutomationSession.Handle o,PestIdentity.Vacuum v,PestTask.Anchor a,int r,long n){return base.prepare(o,v,a,r,n);}
  public PestTask.Motion move(AutomationSession.Handle o,PestTargets.Point p,double t,long n){return base.move(o,p,t,n);}
  public PestTask.Capture capture(AutomationSession.Handle o,PestTargets.Target t,PestIdentity.Vacuum v,long n){return base.capture(o,t,v,n);}
  public PestTask.Motion restore(AutomationSession.Handle o,PestTask.Anchor a,long n){return base.restore(o,a,n);}
  public void release(AutomationSession.Handle o){base.release(o);}
 }
 static PestTask task(P p){return new PestTask(p.h.session,p,new PlayerPositionReceipts(),p.h.coordinator,p.h.items,p.h.cursors);}
 static PestPolicy policy(){return PestPolicy.defaults().withBait(new PestBaitPolicy(true,new PestBaitButtonPlan.Rules("TASTY_CHEESE",16,64,Set.of())));}
 static void opened(P p,PestTask t){for(int i=0;i<8&&p.opens==0;i++){p.h.now+=100_000_000L;t.tick(p.h.now);}check(p.opens==1,"Pest parent approaches known empty trap and opens once");p.show();check(t.ownsMenu(),"owned bait menu is an explicit exception to normal GUI cancellation");for(int i=0;i<4&&p.clicks==0;i++)t.tick(p.h.now);check(p.clicks==1&&p.h.session.holdsFarming(),"bait pickup does not release original Farming hold");}
 public static void main(String[]args){
  var p=new P();var t=task(p);check(!t.request(PestPolicy.defaults(),true,p.h.now),"old Pest flags cannot refill traps");check(t.request(policy(),true,p.h.now),"explicit bait policy may run with zero alive pests");opened(p,t);
  p.received(16,true);t.tick(p.h.now);p.h.traps=new PestTrapObservation(Set.of(),true,Set.of(),true);p.received(12,false);t.tick(p.h.now);p.received(0,true);for(int i=0;i<12&&t.active();i++){p.h.now+=100_000_000L;t.tick(p.h.now);}
  check(t.state()==PestTask.State.COMPLETE&&p.closes==1&&p.clicks==3&&p.base.restores==1&&!p.h.session.holdsFarming(),"full Pest parent: pickup/deposit/return then original pose restoration before release");
  p=new P();t=task(p);check(t.request(policy(),true,p.h.now),"manual-parent fixture");opened(p,t);p.received(16,true);p.manual=true;t.tick(p.h.now);check(t.state()==PestTask.State.CANCELLED&&p.closes==0&&p.clicks==1&&p.base.restores==0,"parent manual takeover cancels without cursor repair/close/return movement");
  System.out.println("PestTaskBaitTest: PASS");
 }
}
