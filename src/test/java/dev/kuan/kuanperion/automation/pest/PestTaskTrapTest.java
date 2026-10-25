package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestTaskTrapTest {
 static final long S=1_000_000_000L;static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final class Fake implements PestTask.Port {
  final PestTaskTest.Fake base=new PestTaskTest.Fake();PestTrapObservation traps=PestTrapObservation.parse(List.of("Full Traps: #1"));MenuSnapshot menu;int opens,clicks,closes;
  Fake(){base.alive=0;base.targets=List.of();}
  public AutomationContext context(){return base.context();}public PestTask.Frame frame(long now){return base.frame(now);}
  public boolean prepare(AutomationSession.Handle h,PestIdentity.Vacuum v,PestTask.Anchor a,int r,long now){return base.prepare(h,v,a,r,now);}
  public PestTask.Motion move(AutomationSession.Handle h,PestTargets.Point p,double t,long now){return base.move(h,p,t,now);}
  public PestTask.Capture capture(AutomationSession.Handle h,PestTargets.Target t,PestIdentity.Vacuum v,long now){return base.capture(h,t,v,now);}
  public PestTask.Motion restore(AutomationSession.Handle h,PestTask.Anchor a,long now){return base.restore(h,a,now);}
  public void release(AutomationSession.Handle h){base.release(h);}public PestTrapRelease.Facts facts(long now){return new PestTrapRelease.Facts(1,now,base.alive,traps,List.of(PestTrapReleaseTest.TARGET));}
  public MenuSnapshot menu(){return menu;}public PestTrapRelease.Open open(AutomationSession.Handle h,PestTrapTarget t,long now){opens++;return PestTrapRelease.Open.SENT;}
  public boolean release(AutomationSession.Handle h,MenuContract c,MenuContract.Ticket t,long now){clicks++;return true;}
  public void closeOwnedMenu(int id){closes++;menu=null;base.ctx=new AutomationContext(1,true,true,Set.of());}
 }
 public static void main(String[]args){
  var f=new Fake();var initial=f;var session=new AutomationSession(new AutomationCoordinator(),f::context,()->{},()->initial.base.stops++);var task=new PestTask(session,f);
  check(!task.request(PestPolicy.defaults(),true,0),"existing policy never auto releases traps");check(task.request(PestPolicy.defaults().withClearTraps(true),true,0),"explicit opt-in known loaded full trap may begin with no flying pests");
  for(int i=0;i<6&&f.opens==0;i++)task.tick(i*S);check(f.opens==1,"bounded approach and one guarded native interaction");
  f.menu=PestTrapReleaseTest.menu(4,"Pest Trap",List.of(new MenuSnapshot.Slot(4,PestTrapReleaseTest.button("Release All Pests"))));f.base.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));check(task.ownsMenu(),"only currently expected trap menu is an exception to GUI cancellation");task.tick(6*S);check(f.clicks==1,"owned trap GUI survives root guard for single exact release click");
  f.traps=PestTrapObservation.parse(List.of("Full Traps: None"));f.base.alive=1;task.tick(7*S);check(f.closes==1&&session.holdsFarming(),"trap cleanup does not prematurely release original Farm hold");
  f.base.targets=List.of(new PestTargets.Target(20,PestTaskTest.TARGET.uuid(),PestTargets.Kind.BAT,new PestTargets.Point(f.base.pos.x()+1,f.base.pos.y()+1,f.base.pos.z()),21));task.tick(8*S);check(f.base.uses==1,"released pests use existing guarded capture driver");f.base.targets=List.of();f.base.alive=0;for(int i=9;i<15&&task.active();i++)task.tick(i*S);
  check(task.state()==PestTask.State.COMPLETE&&f.base.restores==1&&f.base.stops==0,"trap + capture + exact return completes before same Farm token resumes");
  f=new Fake();var fixed=f;session=new AutomationSession(new AutomationCoordinator(),f::context,()->{},()->fixed.base.stops++);task=new PestTask(session,f);check(task.request(PestPolicy.defaults().withClearTraps(true),true,0),"manual GUI fixture");task.tick(0);f.base.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));task.tick(S);check(task.state()==PestTask.State.CANCELLED&&f.clicks==0&&f.opens==0,"unowned user GUI never gets trap exception");
  System.out.println("PestTaskTrapTest: PASS (explicit opt-in full traps at zero alive, shared legal-flight/capture/root lease, exact owned GUI exception, return-before-resume, manual priority)");
 }
}
