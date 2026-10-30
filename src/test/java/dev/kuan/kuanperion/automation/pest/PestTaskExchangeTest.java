package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestTaskExchangeTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class P extends PestExchangeTaskTest.P implements PestTask.Port {
  final PestTaskTest.Fake base=new PestTaskTest.Fake();AutomationSession.Handle seen;boolean closeFailure;
  P(){session.cancel(now);inv.put(0,PestTaskTest.OLD);}
  public PestTask.Frame frame(long n){var f=base.frame(n);return new PestTask.Frame(f.world(),f.observedAt(),f.position(),f.eye(),f.yaw(),f.pitch(),f.slot(),f.item(),f.mayFly(),f.flying(),manual,f.alive(),f.targets(),f.tools(),f.complete());}
  public boolean prepare(AutomationSession.Handle h,PestIdentity.Vacuum v,PestTask.Anchor a,int r,long n){return base.prepare(h,v,a,r,n);}
  public PestTask.Motion move(AutomationSession.Handle h,PestTargets.Point p,double t,long n){return base.move(h,p,t,n);}
  public PestTask.Capture capture(AutomationSession.Handle h,PestTargets.Target t,PestIdentity.Vacuum v,long n){return base.capture(h,t,v,n);}
  public PestTask.Motion restore(AutomationSession.Handle h,PestTask.Anchor a,long n){return base.restore(h,a,n);}
  public void release(AutomationSession.Handle h){base.release(h);}
  public boolean openExchange(AutomationSession.Handle h,long n){check(session.isCurrent(h)&&session.holdsFarming(),"same original Farm hold at exchange open");seen=h;return super.openExchange(h,n);}
  public GuardedMenuAction.Result exchangeClick(AutomationSession.Handle h,PestExchangeTask.Action a,long n){check(h.equals(seen),"same root at effect, no reacquisition");return super.exchangeClick(h,a,n);}
  public boolean closeExchange(AutomationSession.Handle h,PestExchangeTask.Action a,long n){check(h.equals(seen),"same root at settled close");return !closeFailure&&super.closeExchange(h,a,n);}
 }
 static PestTask task(P p){return new PestTask(p.session,p,new PlayerPositionReceipts(),p.coordinator,p.items,p.cursors);}
 static void cleanup(P p,PestTask t,boolean exchange){check(t.request(PestPolicy.defaults().withExchangeAfterCleanup(exchange),true,p.now),"loaded Pest root starts normally");t.tick(p.now);p.now+=PestExchangeTaskTest.S;t.tick(p.now);check(p.base.uses==1&&p.session.holdsFarming(),"normal Pest capture under original hold");p.base.alive=0;p.base.targets=List.of();p.now+=PestExchangeTaskTest.S;t.tick(p.now);}
 static void opened(P p,PestTask t){for(int i=0;i<20&&p.opens==0;i++){p.now+=100_000_000L;t.tick(p.now);}check(p.opens==1&&p.base.restores==0&&p.session.holdsFarming(),"cleanup follows into exchange before return/release");p.show(false,true);check(t.ownsMenu(),"typed source exchange menu is only an owned exception");for(int i=0;i<5&&p.clicks==0;i++)t.tick(p.now);check(p.clicks==1,"one owned exchange role click");}
 public static void main(String[]args){
  var p=new P();var t=task(p);cleanup(p,t,true);opened(p,t);p.ack(true,true);for(int i=0;i<15&&t.active();i++){p.now+=100_000_000L;t.tick(p.now);}check(t.state()==PestTask.State.COMPLETE&&t.confirmedExchanges()==1&&p.closes==1&&p.base.restores==1&&!p.session.holdsFarming(),"full capture/exchange/applied receipt/exact close/original return/restore composition");
  p=new P();t=task(p);cleanup(p,t,false);for(int i=0;i<15&&t.active();i++){p.now+=100_000_000L;t.tick(p.now);}check(t.state()==PestTask.State.COMPLETE&&p.opens==0&&t.confirmedExchanges()==0,"old/default policy never calls Phillip");
  p=new P();t=task(p);cleanup(p,t,true);opened(p,t);p.manual=true;t.tick(p.now);check(t.state()==PestTask.State.CANCELLED&&p.closes==0&&p.base.restores==0&&!p.session.holdsFarming(),"manual takeover stops child and original Farm without repair/return");
  p=new P();p.uncertain=true;t=task(p);cleanup(p,t,true);opened(p,t);for(int i=0;i<5&&t.active();i++)t.tick(p.now);check(t.state()==PestTask.State.FAILED&&p.opens==1&&p.clicks==1&&p.closes==0&&p.base.restores==0,"uncertain exchange cannot retry or resume original Farm");
  p=new P();p.closeFailure=true;t=task(p);cleanup(p,t,true);opened(p,t);p.ack(true,true);for(int i=0;i<5&&t.active();i++)t.tick(p.now);check(t.state()==PestTask.State.FAILED&&p.base.restores==0&&t.confirmedExchanges()==0,"failed final close never becomes completed return authority");
  p=new P();t=task(p);cleanup(p,t,true);for(int i=0;i<20&&p.opens==0;i++){p.now+=100_000_000L;t.tick(p.now);}p.show(true,true);for(int i=0;i<15&&t.active();i++){p.now+=100_000_000L;t.tick(p.now);}check(t.state()==PestTask.State.COMPLETE&&p.opens==1&&p.clicks==0&&p.closes==1&&t.confirmedExchanges()==0&&p.base.restores==1,"known skip returns normally with no invented new exchange");
  for(String mode:List.of("world","farm","focus")){
   p=new P();t=task(p);cleanup(p,t,true);opened(p,t);p.ctx=switch(mode){case "world"->new AutomationContext(2,true,true,Set.of(AutomationContext.Blocker.MENU));case "farm"->new AutomationContext(1,false,true,Set.of(AutomationContext.Blocker.MENU));default->new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU,AutomationContext.Blocker.FOCUS));};t.tick(p.now);check(!t.active()&&p.closes==0&&p.base.restores==0&&!p.session.holdsFarming(),mode+" invalidates original exchange hold without close/return/rearm");
  }
  System.out.println("PestTaskExchangeTest: PASS real Pest capture/child/return composition; controlled ports, not live server");
 }
}
