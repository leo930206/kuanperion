package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
public final class PestTaskTest {
 static final long S=1_000_000_000L;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static final ItemSnapshot TOOL=new ItemSnapshot("INFINI_VACUUM","minecraft:golden_hoe","tool-uuid","Vacuum","tool-meta",1,List.of("Ability: Vacuum RIGHT CLICK","within 10 blocks"),true);
 static final ItemSnapshot OLD=new ItemSnapshot("PUMPKIN_DICER","minecraft:diamond_axe","farm-tool","Dicer","dicer-meta",1,List.of(),true);
 static final PestTargets.Point ORIGIN=new PestTargets.Point(0.5,70,0.5),NEAR=new PestTargets.Point(5,71,0.5);
 static final PestTargets.Target TARGET=new PestTargets.Target(20,UUID.fromString("00000000-0000-0000-0000-000000000020"),PestTargets.Kind.BAT,NEAR,21);
 static final class Fake implements PestTask.Port {
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());long now;int alive=1,prepares,moves,uses,releases,restores,stops;
  boolean flight=true,manual,complete=true,accepted=true,throwPrepare,throwCapture;int slot=0;ItemSnapshot item=OLD;PestTargets.Point pos=ORIGIN;List<PestTargets.Target> targets=List.of(TARGET);
  public AutomationContext context(){return ctx;}
  public PestTask.Frame frame(long time){return new PestTask.Frame(1,time,pos,new PestTargets.Point(pos.x(),pos.y()+1.62,pos.z()),0,0,slot,item,flight,false,manual,alive,targets,List.of(new PestIdentity.Vacuum(3,TOOL,10)),complete);}
  public boolean prepare(AutomationSession.Handle h,PestIdentity.Vacuum tool,PestTask.Anchor anchor,long time){prepares++;if(throwPrepare)throw new IllegalStateException("native prepare failure");slot=3;item=TOOL;return accepted;}
  public PestTask.Motion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long time){moves++;pos=goal;return accepted?PestTask.Motion.ARRIVED:PestTask.Motion.BLOCKED;}
  public PestTask.Capture capture(AutomationSession.Handle h,PestTargets.Target t,PestIdentity.Vacuum tool,long time){uses++;if(throwCapture)throw new IllegalStateException("native use failure");return accepted?PestTask.Capture.USING:PestTask.Capture.REJECTED;}
  public void release(AutomationSession.Handle h){releases++;}
  public PestTask.Motion restore(AutomationSession.Handle h,PestTask.Anchor anchor,long time){restores++;slot=anchor.slot();item=anchor.item();return accepted?PestTask.Motion.ARRIVED:PestTask.Motion.BLOCKED;}
 }
 static final class Harness {
  final Fake f=new Fake();final AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->{},()->f.stops++);final PestTask task=new PestTask(session,f);
  void start(long time){check(task.request(PestPolicy.defaults(),true,time),"start with server flight, exact vacuum, loaded target");task.tick(time);}
 }
 public static void main(String[]args){
  var h=new Harness();h.f.flight=false;check(!h.task.request(PestPolicy.defaults(),true,0)&&h.f.prepares==0,"no server flight never acquires/effects");
  h.f.flight=true;h.f.targets=List.of();check(!h.task.request(PestPolicy.defaults(),true,0),"unloaded pests not falsely considered actionable");
  h=new Harness();h.start(0);h.task.tick(S);check(h.f.uses==1&&h.session.holdsFarming(),"prepare then capture while original Farming held");
  h.f.targets=List.of();h.task.tick(2*S);check(h.task.active()&&h.task.confirmedDecreases()==0,"despawn alone not kill proof");
  h.f.alive=0;h.task.tick(3*S);h.task.tick(4*S);h.task.tick(5*S);h.task.tick(6*S);check(h.task.state()==PestTask.State.COMPLETE&&h.task.confirmedDecreases()==1&&h.f.restores==1&&!h.session.holdsFarming()&&h.f.stops==0,"paired server count/departure then return and restore before resume");
  h=new Harness();h.start(0);h.task.tick(S);h.f.alive=0;h.task.tick(2*S);check(h.task.confirmedDecreases()==0&&h.task.active(),"count alone while original target remains not proof");h.task.cancel(3*S);check(!h.task.active()&&h.f.restores==0&&h.f.stops==1,"cancel doesn't restore user's tool/look or rearm Farming");
  h=new Harness();h.start(0);h.f.manual=true;h.task.tick(S);check(h.task.state()==PestTask.State.CANCELLED&&h.f.uses==0&&h.f.releases>0,"physical input wins before next effect");
  h=new Harness();h.start(0);h.f.ctx=new AutomationContext(2,true,true,Set.of());h.task.tick(S);check(!h.task.active()&&h.f.moves==0&&h.f.restores==0,"world change never returns to old anchor");
  h=new Harness();h.start(0);h.f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.FOCUS));h.task.tick(S);check(!h.task.active()&&h.f.uses==0,"focus loss stops input immediately");
  h=new Harness();h.start(0);h.f.slot=7;h.f.item=OLD;h.task.tick(S);check(!h.task.active()&&h.f.uses==0,"manual hotbar change wins");
  h=new Harness();h.start(0);h.task.tick(S);h.f.targets=List.of();h.task.tick(21*S);check(h.task.state()==PestTask.State.TIMED_OUT&&h.f.restores==0,"missing target/count receipt bounded");
  h=new Harness();h.start(0);h.task.tick(S);h.f.targets=List.of();h.f.alive=0;h.task.tick(2*S);h.task.tick(3*S);h.f.accepted=false;h.task.tick(4*S);h.task.tick(5*S);check(h.task.state()==PestTask.State.FAILED&&h.f.stops==1,"restore rejection cannot report success/resume");
  h=new Harness();h.start(0);h.f.item=new ItemSnapshot(TOOL.skyblockId(),TOOL.minecraftId(),TOOL.uuid(),TOOL.name(),"bag-updated",1,List.of("Ability: Vacuum RIGHT CLICK","within 10 blocks","Vacuum Bag: 1"),true);h.task.tick(S);check(h.task.active()&&h.f.uses==1,"same UUID/range remains usable when server updates vacuum bag metadata");
  h=new Harness();h.start(0);h.f.item=new ItemSnapshot(TOOL.skyblockId(),TOOL.minecraftId(),"different-uuid",TOOL.name(),TOOL.fingerprint(),1,TOOL.lore(),true);h.task.tick(S);check(!h.task.active()&&h.f.uses==0,"different UUID never substituted as original tool");
  h=new Harness();h.start(0);h.session.invalidate();h.task.tick(S);check(h.task.state()==PestTask.State.CANCELLED&&h.f.uses==0,"revoked ownership cannot act");
  h=new Harness();h.f.throwPrepare=true;h.start(0);check(h.task.state()==PestTask.State.FAILED&&!h.session.holdsFarming()&&h.f.stops==1,"native preparation exception releases ownership and stops held Farming");
  h=new Harness();h.start(0);h.f.throwCapture=true;h.task.tick(S);check(h.task.state()==PestTask.State.FAILED&&!h.session.active()&&h.f.stops==1,"native use exception cannot escape with held input/session");
  h=new Harness();h.start(0);h.f.complete=false;h.task.tick(S);check(!h.task.active()&&h.f.uses==0,"unknown Tab/native frame is not empty successful Garden");
  h=new Harness();h.start(0);h.f.ctx=new AutomationContext(1,false,true,Set.of());h.task.tick(S);check(!h.task.active()&&h.f.uses==0,"manually disarmed Farming cannot be captured by a stale hold");
  System.out.println("PestTaskTest: PASS (server-granted flight, typed exact tool, paired count/departure proof, bounded capture, return-before-resume, manual/world/focus/slot/revocation guards)");
 }
}
